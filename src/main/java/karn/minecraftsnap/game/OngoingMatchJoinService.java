package karn.minecraftsnap.game;

import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

public class OngoingMatchJoinService {
	private final MatchManager matchManager;
	private final UnitSpawnQueueService unitSpawnQueueService;
	private final UnitSpawnService unitSpawnService;
	private final TextTemplateResolver textTemplateResolver;

	public OngoingMatchJoinService(
		MatchManager matchManager,
		UnitSpawnQueueService unitSpawnQueueService,
		UnitSpawnService unitSpawnService,
		TextTemplateResolver textTemplateResolver
	) {
		this.matchManager = matchManager;
		this.unitSpawnQueueService = unitSpawnQueueService;
		this.unitSpawnService = unitSpawnService;
		this.textTemplateResolver = textTemplateResolver;
	}

	public String join(ServerPlayerEntity player, SystemConfig systemConfig, TextConfigFile textConfig) {
		if (player == null) {
			return textConfig.commandPlayerNotFoundMessage;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return textConfig.midMatchJoinUnavailableMessage;
		}
		if (!canJoin(matchManager.getPhase(), state)) {
			return textConfig.midMatchJoinAlreadyJoinedMessage;
		}
		var teamId = selectBalancedTeam(
			matchManager.getOnlineTeamPlayers(TeamId.RED).size(),
			matchManager.getOnlineTeamPlayers(TeamId.BLUE).size()
		);
		matchManager.setRole(player, teamId, RoleType.UNIT);
		moveUnitToWaitingState(player, systemConfig);
		unitSpawnQueueService.enqueueSpectatorUnit(matchManager, teamId, player.getUuid());
		return textConfig.midMatchJoinSuccessMessage.replace("{team}", teamId.getDisplayName());
	}

	public void handleJoin(ServerPlayerEntity player, SystemConfig systemConfig, TextConfigFile textConfig) {
		if (player == null) {
			return;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (shouldResetStaleAssignmentOnJoin(matchManager.getPhase(), state, queuePosition(state, player.getUuid()))) {
			movePlayerToLobbyState(player, systemConfig);
			matchManager.setRole(player, null, RoleType.NONE);
			player.sendMessage(textTemplateResolver.format(textConfig.midMatchJoinAvailableMessage), false);
			return;
		}
		if (shouldRestoreWaitingUnitOnJoin(matchManager.getPhase(), state)) {
			moveUnitToWaitingState(player, systemConfig);
			unitSpawnQueueService.enqueueSpectatorUnit(matchManager, state.getTeamId(), player.getUuid());
			return;
		}
		if (shouldNotifyJoinAvailable(matchManager.getPhase(), state)) {
			player.sendMessage(textTemplateResolver.format(textConfig.midMatchJoinAvailableMessage), false);
		}
	}

	public boolean handleDisconnect(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (shouldRemoveCaptainFromTeamOnDisconnect(matchManager.getPhase(), state)) {
			unitSpawnService.resetPlayer(player);
			player.getInventory().clear();
			unitSpawnQueueService.removePlayer(player.getUuid());
			matchManager.setRole(player, null, RoleType.NONE);
			return true;
		}
		if (!shouldHandleMidMatchLeave(matchManager.getPhase(), state)) {
			return false;
		}
		var teamId = state.getTeamId();
		unitSpawnService.resetPlayer(player);
		player.getInventory().clear();
		matchManager.clearCurrentUnit(player.getUuid());
		unitSpawnQueueService.enqueueSpectatorUnit(matchManager, teamId, player.getUuid());
		return true;
	}

	static boolean canJoin(MatchPhase phase, PlayerMatchState state) {
		return phase == MatchPhase.GAME_RUNNING && !hasAssignment(state);
	}

	static boolean shouldNotifyJoinAvailable(MatchPhase phase, PlayerMatchState state) {
		return canJoin(phase, state);
	}

	static boolean shouldHandleMidMatchLeave(MatchPhase phase, PlayerMatchState state) {
		return phase == MatchPhase.GAME_RUNNING
			&& state != null
			&& state.getTeamId() != null
			&& state.getRoleType() == RoleType.UNIT;
	}

	static boolean shouldRemoveCaptainFromTeamOnDisconnect(MatchPhase phase, PlayerMatchState state) {
		return phase == MatchPhase.GAME_RUNNING
			&& state != null
			&& state.getTeamId() != null
			&& state.getRoleType() == RoleType.CAPTAIN;
	}

	static TeamId selectBalancedTeam(int redCount, int blueCount) {
		return redCount <= blueCount ? TeamId.RED : TeamId.BLUE;
	}

	static boolean shouldRestoreWaitingUnitOnJoin(MatchPhase phase, PlayerMatchState state) {
		return phase == MatchPhase.GAME_RUNNING
			&& state != null
			&& state.getTeamId() != null
			&& state.getRoleType() == RoleType.UNIT
			&& state.getCurrentUnitId() == null;
	}

	static boolean shouldResetStaleAssignmentOnJoin(MatchPhase phase, PlayerMatchState state, int queuePosition) {
		return shouldRestoreWaitingUnitOnJoin(phase, state) && queuePosition <= 0;
	}

	private static boolean hasAssignment(PlayerMatchState state) {
		return state != null && state.getTeamId() != null;
	}

	private int queuePosition(PlayerMatchState state, java.util.UUID playerId) {
		if (state == null || state.getTeamId() == null || playerId == null) {
			return 0;
		}
		return unitSpawnQueueService.queuePosition(matchManager, state.getTeamId(), playerId);
	}

	private void moveUnitToWaitingState(ServerPlayerEntity player, SystemConfig systemConfig) {
		if (player == null || systemConfig == null) {
			return;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getTeamId() == null) {
			return;
		}
		unitSpawnService.resetPlayer(player);
		player.getInventory().clear();
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(20.0f);
		matchManager.clearCurrentUnit(player.getUuid());
		player.changeGameMode(GameMode.SPECTATOR);
		teleportToCaptainSpawn(player, systemConfig, state.getTeamId());
	}

	private void movePlayerToLobbyState(ServerPlayerEntity player, SystemConfig systemConfig) {
		if (player == null || systemConfig == null) {
			return;
		}
		unitSpawnService.resetPlayer(player);
		player.getInventory().clear();
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(20.0f);
		matchManager.clearCurrentUnit(player.getUuid());
		unitSpawnQueueService.removePlayer(player.getUuid());
		player.changeGameMode(GameMode.ADVENTURE);
		teleportToLobbySpawn(player, systemConfig);
	}

	private void teleportToCaptainSpawn(ServerPlayerEntity player, SystemConfig systemConfig, TeamId teamId) {
		var spawn = systemConfig.gameStart.captainSpawnFor(teamId);
		var world = resolveWorld(player, systemConfig.world);
		player.teleportTo(new TeleportTarget(
			world,
			new Vec3d(spawn.x, spawn.y, spawn.z),
			Vec3d.ZERO,
			spawn.yaw,
			spawn.pitch,
			TeleportTarget.NO_OP
		));
		player.setVelocity(Vec3d.ZERO);
		player.fallDistance = 0.0f;
	}

	private void teleportToLobbySpawn(ServerPlayerEntity player, SystemConfig systemConfig) {
		var spawn = systemConfig.lobby;
		var world = resolveWorld(player, systemConfig.world);
		player.teleportTo(new TeleportTarget(
			world,
			new Vec3d(spawn.spawnX, spawn.spawnY, spawn.spawnZ),
			Vec3d.ZERO,
			spawn.spawnYaw,
			spawn.spawnPitch,
			TeleportTarget.NO_OP
		));
		player.setVelocity(Vec3d.ZERO);
		player.fallDistance = 0.0f;
	}

	private ServerWorld resolveWorld(ServerPlayerEntity player, String worldId) {
		try {
			var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
			var world = player.getServer().getWorld(worldKey);
			return world != null ? world : player.getServer().getOverworld();
		} catch (Exception ignored) {
			return player.getServer().getOverworld();
		}
	}
}
