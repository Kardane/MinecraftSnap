package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.WikiGuiService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import java.util.List;

public class LobbyCoordinator {
	private final MatchManager matchManager;
	private final StatsRepository statsRepository;
	private final TeamAssignmentService teamAssignmentService;
	private final CaptainSelectionService captainSelectionService;
	private final FactionSelectionService factionSelectionService;
	private final WikiGuiService wikiGuiService;
	private final FactionSelectionGuiService factionSelectionGuiService;
	private final LobbyScoreboardService lobbyScoreboardService;

	public LobbyCoordinator(
		MatchManager matchManager,
		StatsRepository statsRepository,
		TeamAssignmentService teamAssignmentService,
		CaptainSelectionService captainSelectionService,
		FactionSelectionService factionSelectionService,
		WikiGuiService wikiGuiService,
		FactionSelectionGuiService factionSelectionGuiService,
		LobbyScoreboardService lobbyScoreboardService
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.teamAssignmentService = teamAssignmentService;
		this.captainSelectionService = captainSelectionService;
		this.factionSelectionService = factionSelectionService;
		this.wikiGuiService = wikiGuiService;
		this.factionSelectionGuiService = factionSelectionGuiService;
		this.lobbyScoreboardService = lobbyScoreboardService;
	}

	public void handleJoin(ServerPlayerEntity player, SystemConfig config) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			applyLobbyState(player, config);
		}

		lobbyScoreboardService.sync(player.getServer(), matchManager, statsRepository);
	}

	public void handleDisconnect(MinecraftServer server) {
		lobbyScoreboardService.sync(server, matchManager, statsRepository);
	}

	public void tick(MinecraftServer server, SystemConfig config) {
		if (matchManager.getServerTicks() % 20L == 0L) {
			lobbyScoreboardService.sync(server, matchManager, statsRepository);
		}

		if (matchManager.getPhase() == MatchPhase.TEAM_SELECT
			&& matchManager.getPhaseTicks() >= config.lobby.factionSelectDelaySeconds * 20L) {
			matchManager.setPhase(MatchPhase.FACTION_SELECT);
			openCaptainFactionGuis(server, config);
			return;
		}

		if (matchManager.getPhase() == MatchPhase.FACTION_SELECT) {
			if (matchManager.isFactionSelectionComplete()
				|| matchManager.getPhaseTicks() >= config.lobby.factionSelectDurationSeconds * 20L) {
				startGame(server, config, false);
			}
		}
	}

	public void startTeamSelection(MinecraftServer server, SystemConfig config) {
		var players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			matchManager.setPhase(MatchPhase.TEAM_SELECT);
			return;
		}

		matchManager.setPhase(MatchPhase.TEAM_SELECT);
		var candidates = players.stream()
			.map(player -> {
				var stats = statsRepository.getOrCreate(player.getUuid(), player.getName().getString());
				var state = matchManager.getPlayerState(player.getUuid());
				return new TeamAssignmentService.PlayerCandidate(
					player.getUuid(),
					player.getName().getString(),
					stats.ladder,
					stats.preference,
					state.getRoleType() == RoleType.CAPTAIN ? state.getTeamId() : null,
					state.getRoleType() == RoleType.CAPTAIN
				);
			})
			.toList();
		var assignments = teamAssignmentService.assignTeams(candidates);
		var captains = captainSelectionService.selectCaptains(candidates, assignments);

		for (var candidate : candidates) {
			var teamId = assignments.get(candidate.playerId());
			var roleType = candidate.playerId().equals(captains.get(teamId)) ? RoleType.CAPTAIN : RoleType.UNIT;
			matchManager.setRole(candidate.playerId(), teamId, roleType);
		}

		for (var player : players) {
			applyLobbyState(player, config);
		}

		lobbyScoreboardService.sync(server, matchManager, statsRepository);
	}

	public void forceStartGame(MinecraftServer server, SystemConfig config) {
		if (matchManager.getCaptainId(TeamId.RED) == null || matchManager.getCaptainId(TeamId.BLUE) == null) {
			startTeamSelection(server, config);
		}

		startGame(server, config, true);
	}

	public void openWiki(ServerPlayerEntity player) {
		wikiGuiService.open(player, matchManager.getPhase());
	}

	public boolean handleShortcut(ServerPlayerEntity player, SystemConfig config) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (player.isSneaking() && matchManager.getPhase() == MatchPhase.FACTION_SELECT && state.getRoleType() == RoleType.CAPTAIN && state.getTeamId() != null) {
			openFactionGui(player, config);
			return true;
		}

		wikiGuiService.open(player, matchManager.getPhase());
		return true;
	}

	public boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (!matchManager.isPregameDamageBlocked()) {
			return true;
		}

		return !(entity instanceof ServerPlayerEntity) || !(source.getAttacker() instanceof ServerPlayerEntity);
	}

	public void openCurrentGui(ServerPlayerEntity player, SystemConfig config) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (matchManager.getPhase() == MatchPhase.FACTION_SELECT && state.getRoleType() == RoleType.CAPTAIN && state.getTeamId() != null) {
			openFactionGui(player, config);
			return;
		}

		openWiki(player);
	}

	private void startGame(MinecraftServer server, SystemConfig config, boolean forced) {
		if (forced || !matchManager.isFactionSelectionComplete()) {
			fillMissingFactions();
		}

		matchManager.setPhase(MatchPhase.GAME_RUNNING);
		lobbyScoreboardService.sync(server, matchManager, statsRepository);
	}

	private void fillMissingFactions() {
		var selections = factionSelectionService.newSelectionMap();
		selections.putAll(matchManager.getFactionSelectionsSnapshot());
		factionSelectionService.fillMissingWithDefault(selections);
		for (var entry : selections.entrySet()) {
			matchManager.setFactionSelection(entry.getKey(), entry.getValue());
		}
	}

	private void openCaptainFactionGuis(MinecraftServer server, SystemConfig config) {
		for (var teamId : TeamId.values()) {
			var captainId = matchManager.getCaptainId(teamId);
			if (captainId == null) {
				continue;
			}

			var player = server.getPlayerManager().getPlayer(captainId);
			if (player != null) {
				openFactionGui(player, config);
			}
		}
	}

	private void openFactionGui(ServerPlayerEntity player, SystemConfig config) {
		var state = matchManager.getPlayerState(player.getUuid());
		var teamId = state.getTeamId();
		if (teamId == null) {
			wikiGuiService.open(player, matchManager.getPhase());
			return;
		}

		factionSelectionGuiService.open(player, teamId, matchManager.getFactionSelection(teamId), factionId -> {
			matchManager.setFactionSelection(teamId, factionId);
			if (matchManager.isFactionSelectionComplete()) {
				startGame(player.getServer(), config, false);
			}
		});
	}

	private void applyLobbyState(ServerPlayerEntity player, SystemConfig config) {
		player.changeGameMode(GameMode.ADVENTURE);
		var world = resolveLobbyWorld(player.getServer(), config);
		var teleportTarget = new TeleportTarget(
			world,
			new net.minecraft.util.math.Vec3d(config.lobby.spawnX, config.lobby.spawnY, config.lobby.spawnZ),
			net.minecraft.util.math.Vec3d.ZERO,
			config.lobby.spawnYaw,
			config.lobby.spawnPitch,
			TeleportTarget.NO_OP
		);
		player.teleportTo(teleportTarget);
	}

	private ServerWorld resolveLobbyWorld(MinecraftServer server, SystemConfig config) {
		try {
			var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(config.lobby.world));
			var world = server.getWorld(worldKey);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}
}
