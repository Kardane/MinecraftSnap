package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.WikiGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

public class LobbyCoordinator {
	private final java.util.EnumMap<TeamId, Long> factionGuiOpenTicks = new java.util.EnumMap<>(TeamId.class);
	private boolean autoAdvanceTeamSelection = true;
	private final MatchManager matchManager;
	private final StatsRepository statsRepository;
	private final TeamAssignmentService teamAssignmentService;
	private final CaptainSelectionService captainSelectionService;
	private final FactionSelectionService factionSelectionService;
	private final WikiGuiService wikiGuiService;
	private final FactionSelectionGuiService factionSelectionGuiService;
	private final LobbyScoreboardService lobbyScoreboardService;
	private final PreparationGuiService preparationGuiService;
	private final UnitSpawnService unitSpawnService;
	private final TextTemplateResolver textTemplateResolver;
	private final java.util.function.BiConsumer<TeamId, FactionId> factionSelectionHandler;

	public LobbyCoordinator(
		MatchManager matchManager,
		StatsRepository statsRepository,
		TeamAssignmentService teamAssignmentService,
		CaptainSelectionService captainSelectionService,
		FactionSelectionService factionSelectionService,
		WikiGuiService wikiGuiService,
		FactionSelectionGuiService factionSelectionGuiService,
		LobbyScoreboardService lobbyScoreboardService,
		PreparationGuiService preparationGuiService,
		UnitSpawnService unitSpawnService,
		TextTemplateResolver textTemplateResolver,
		java.util.function.BiConsumer<TeamId, FactionId> factionSelectionHandler
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.teamAssignmentService = teamAssignmentService;
		this.captainSelectionService = captainSelectionService;
		this.factionSelectionService = factionSelectionService;
		this.wikiGuiService = wikiGuiService;
		this.factionSelectionGuiService = factionSelectionGuiService;
		this.lobbyScoreboardService = lobbyScoreboardService;
		this.preparationGuiService = preparationGuiService;
		this.unitSpawnService = unitSpawnService;
		this.textTemplateResolver = textTemplateResolver;
		this.factionSelectionHandler = factionSelectionHandler;
	}

	public void handleJoin(ServerPlayerEntity player, SystemConfig config) {
		if (matchManager.getPhase() == MatchPhase.GAME_START) {
			preparePlayerForGameStart(player, config);
		} else if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
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

		if (autoAdvanceTeamSelection
			&& matchManager.getPhase() == MatchPhase.TEAM_SELECT
			&& matchManager.getPhaseTicks() >= config.lobby.factionSelectDelaySeconds * 20L) {
			matchManager.setPhase(MatchPhase.FACTION_SELECT);
			openCaptainFactionGuis(server, config);
			return;
		}

		if (matchManager.getPhase() == MatchPhase.FACTION_SELECT) {
			reopenUnselectedCaptainFactionGuis(server, config);
			if (matchManager.isFactionSelectionComplete()
				|| matchManager.getPhaseTicks() >= config.lobby.factionSelectDurationSeconds * 20L) {
				enterGameStart(server, config, false);
			}
		}

		if (matchManager.getPhase() == MatchPhase.GAME_START
			&& matchManager.getPhaseTicks() >= config.gameStart.waitSeconds * 20L) {
			matchManager.startGameRunning();
			lobbyScoreboardService.sync(server, matchManager, statsRepository);
		}
	}

	public void setLaneRevealState(LaneId laneId, boolean revealed) {
		matchManager.setLaneRevealOverride(laneId, revealed);
	}

	public boolean isLaneRevealed(LaneId laneId) {
		return matchManager.isLaneRevealed(laneId);
	}

	public void forcePhase(MatchPhase phase, MinecraftServer server, SystemConfig config) {
		if (phase == MatchPhase.GAME_START) {
			enterGameStart(server, config, true);
			return;
		}
		if (phase == MatchPhase.GAME_RUNNING) {
			enterGameStart(server, config, true);
			matchManager.startGameRunning();
			lobbyScoreboardService.sync(server, matchManager, statsRepository);
			return;
		}

		matchManager.setPhase(phase);
		if (phase == MatchPhase.LOBBY) {
			for (var player : server.getPlayerManager().getPlayerList()) {
				applyLobbyState(player, config);
			}
		}
	}

	public void startTeamSelection(MinecraftServer server, SystemConfig config) {
		startTeamSelection(server, config, autoAdvanceTeamSelection);
	}

	public void assignTeamsOnly(MinecraftServer server, SystemConfig config) {
		startTeamSelection(server, config, false);
	}

	public boolean toggleAutoAdvanceTeamSelection() {
		autoAdvanceTeamSelection = !autoAdvanceTeamSelection;
		return autoAdvanceTeamSelection;
	}

	public boolean isAutoAdvanceTeamSelection() {
		return autoAdvanceTeamSelection;
	}

	private void startTeamSelection(MinecraftServer server, SystemConfig config, boolean autoAdvance) {
		autoAdvanceTeamSelection = autoAdvance;
		var players = server.getPlayerManager().getPlayerList();
		if (players.isEmpty()) {
			matchManager.setPhase(MatchPhase.TEAM_SELECT);
			return;
		}

		matchManager.setPhase(MatchPhase.TEAM_SELECT);
		var candidates = players.stream()
			.map(player -> {
				var stats = statsRepository.getOrCreate(player.getUuid(), player.getName().getString());
				return new TeamAssignmentService.PlayerCandidate(
					player.getUuid(),
					player.getName().getString(),
					stats.ladder,
					stats.preference,
					null,
					false
				);
			})
			.toList();
		var assignments = teamAssignmentService.assignTeams(candidates);
		var captains = captainSelectionService.selectCaptains(candidates, assignments);

		for (var candidate : candidates) {
			var teamId = assignments.get(candidate.playerId());
			var roleType = candidate.playerId().equals(captains.get(teamId)) ? RoleType.CAPTAIN : RoleType.UNIT;
			matchManager.setRole(candidate.playerId(), teamId, roleType);
			var state = matchManager.getPlayerState(candidate.playerId());
			state.setPreferredUnitId(roleType == RoleType.UNIT ? preferredUnitId(candidate.preference()) : null);
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

		enterGameStart(server, config, true);
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
		if (matchManager.getPhase() == MatchPhase.GAME_START && config.gameStart.allowShiftF) {
			preparationGuiService.open(player, state);
			return true;
		}

		wikiGuiService.open(player, matchManager.getPhase());
		return true;
	}

	public void openCurrentGui(ServerPlayerEntity player, SystemConfig config) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (matchManager.getPhase() == MatchPhase.FACTION_SELECT && state.getRoleType() == RoleType.CAPTAIN && state.getTeamId() != null) {
			openFactionGui(player, config);
			return;
		}
		if (matchManager.getPhase() == MatchPhase.GAME_START && config.gameStart.allowShiftF) {
			preparationGuiService.open(player, state);
			return;
		}

		openWiki(player);
	}

	private void enterGameStart(MinecraftServer server, SystemConfig config, boolean forced) {
		if (forced || !matchManager.isFactionSelectionComplete()) {
			fillMissingFactions();
		}

		factionGuiOpenTicks.clear();
		matchManager.enterGameStart();
		for (var player : server.getPlayerManager().getPlayerList()) {
			preparePlayerForGameStart(player, config);
		}
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

	private void reopenUnselectedCaptainFactionGuis(MinecraftServer server, SystemConfig config) {
		if (matchManager.getServerTicks() % 20L != 0L) {
			return;
		}
		for (var teamId : TeamId.values()) {
			if (matchManager.getFactionSelection(teamId) != null) {
				continue;
			}
			var captainId = matchManager.getCaptainId(teamId);
			if (captainId == null) {
				continue;
			}
			var player = server.getPlayerManager().getPlayer(captainId);
			if (player == null) {
				continue;
			}
			var lastOpened = factionGuiOpenTicks.getOrDefault(teamId, Long.MIN_VALUE);
			if (matchManager.getServerTicks() - lastOpened < 20L) {
				continue;
			}
			openFactionGui(player, config);
		}
	}

	private void openFactionGui(ServerPlayerEntity player, SystemConfig config) {
		var state = matchManager.getPlayerState(player.getUuid());
		var teamId = state.getTeamId();
		if (teamId == null) {
			wikiGuiService.open(player, matchManager.getPhase());
			return;
		}

		factionGuiOpenTicks.put(teamId, matchManager.getServerTicks());
		factionSelectionGuiService.open(player, teamId, matchManager.getFactionSelection(teamId), factionId -> {
			factionSelectionHandler.accept(teamId, factionId);
			factionGuiOpenTicks.remove(teamId);
			if (matchManager.isFactionSelectionComplete()) {
				enterGameStart(player.getServer(), config, false);
			}
		});
	}

	private void preparePlayerForGameStart(ServerPlayerEntity player, SystemConfig config) {
		unitSpawnService.resetPlayer(player);
		player.getInventory().clear();
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(20.0f);

		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() == RoleType.UNIT) {
			matchManager.clearCurrentUnit(player.getUuid());
			player.changeGameMode(GameMode.SPECTATOR);
			teleport(player, config.world, config.gameStart.captainSpawnFor(state.getTeamId()));
		} else {
			player.changeGameMode(GameMode.ADVENTURE);
			teleport(player, config.world, config.gameStart.captainSpawnFor(state.getTeamId()));
			unitSpawnService.getUnitLoadoutService().giveCaptainItems(player, state.getFactionId(), textTemplateResolver);
		}
	}

	private void applyLobbyState(ServerPlayerEntity player, SystemConfig config) {
		unitSpawnService.resetPlayer(player);
		player.getInventory().clear();
		matchManager.clearCurrentUnit(player.getUuid());
		player.changeGameMode(GameMode.ADVENTURE);
		var position = new SystemConfig.PositionConfig();
		position.x = config.lobby.spawnX;
		position.y = config.lobby.spawnY;
		position.z = config.lobby.spawnZ;
		position.yaw = config.lobby.spawnYaw;
		position.pitch = config.lobby.spawnPitch;
		teleport(player, config.world, position);
	}

	private void teleport(ServerPlayerEntity player, String worldId, SystemConfig.PositionConfig position) {
		var world = resolveWorld(player.getServer(), worldId);
		var teleportTarget = new TeleportTarget(
			world,
			new Vec3d(position.x, position.y, position.z),
			Vec3d.ZERO,
			position.yaw,
			position.pitch,
			TeleportTarget.NO_OP
		);
		player.teleportTo(teleportTarget);
	}

	private ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		try {
			var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
			var world = server.getWorld(worldKey);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	private String preferredUnitId(String preference) {
		if (preference == null || !preference.startsWith("unit:")) {
			return null;
		}
		var unitId = preference.substring("unit:".length()).trim();
		return unitId.isBlank() ? null : unitId;
	}
}
