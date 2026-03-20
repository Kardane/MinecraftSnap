package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import java.util.List;
import java.util.UUID;

public class UnitSpawnService {
	private final CaptainManaService captainManaService;
	private final UnitRegistry unitRegistry;
	private final UnitLoadoutService unitLoadoutService;
	private final UnitAbilityService unitAbilityService;
	private final UiSoundService uiSoundService;
	private final java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier;
	private final java.util.function.Supplier<UnitHookService> unitHookServiceSupplier;
	private final java.util.Random random = new java.util.Random();

	public UnitSpawnService() {
		this(new CaptainManaService(), null, new UnitLoadoutService(), new UnitAbilityService(), null, () -> null, () -> null);
	}

	public UnitSpawnService(
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitLoadoutService unitLoadoutService,
		UnitAbilityService unitAbilityService,
		java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier,
		java.util.function.Supplier<UnitHookService> unitHookServiceSupplier
	) {
		this(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService, null, captainSkillServiceSupplier, unitHookServiceSupplier);
	}

	public UnitSpawnService(
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitLoadoutService unitLoadoutService,
		UnitAbilityService unitAbilityService,
		UiSoundService uiSoundService,
		java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier,
		java.util.function.Supplier<UnitHookService> unitHookServiceSupplier
	) {
		this.captainManaService = captainManaService;
		this.unitRegistry = unitRegistry;
		this.unitLoadoutService = unitLoadoutService;
		this.unitAbilityService = unitAbilityService;
		this.uiSoundService = uiSoundService;
		this.captainSkillServiceSupplier = captainSkillServiceSupplier;
		this.unitHookServiceSupplier = unitHookServiceSupplier;
	}

	public SpawnCandidate selectSpawnCandidate(String unitId, List<SpawnCandidate> candidates) {
		var shuffled = new java.util.ArrayList<>(candidates);
		java.util.Collections.shuffle(shuffled, random);

		for (var candidate : shuffled) {
			if (candidate.spectator() && unitId.equals(candidate.preferredUnitId())) {
				return candidate;
			}
		}
		for (var candidate : shuffled) {
			if (candidate.spectator()) {
				return candidate;
			}
		}
		return null;
	}

	public SpawnResult spawnSelectedUnit(
		ServerPlayerEntity captain,
		String unitId,
		MatchManager matchManager,
		SystemConfig systemConfig,
		TextTemplateResolver textTemplateResolver
	) {
		var captainState = matchManager.getPlayerState(captain.getUuid());
		if (!captainState.isCaptain() || captainState.getTeamId() == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnCaptainOnlyMessage);
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnUnknownUnitMessage);
		}
		if (captainState.getFactionId() != definition.factionId()) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnWrongFactionMessage);
		}
		var laneId = nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(laneId)) {
			playDeny(captain);
			return SpawnResult.error(systemConfig.gameStart.captainSpawnBlockedLaneMessage.replace("{lane}", laneLabel(laneId)));
		}

		var candidates = matchManager.getOnlineSpectatorUnits(captainState.getTeamId()).stream()
			.map(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return new SpawnCandidate(player.getUuid(), state.getPreferredUnitId(), player.isSpectator());
			})
			.toList();
		var candidate = selectSpawnCandidate(unitId, candidates);
		if (candidate == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnNoCandidateMessage);
		}
		if (!captainManaService.trySpendForSpawn(captain.getUuid(), definition.cost())) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnInsufficientManaMessage);
		}

		var target = captain.getServer().getPlayerManager().getPlayer(candidate.playerId());
		if (target == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnTargetMissingMessage);
		}

		target.changeGameMode(GameMode.ADVENTURE);
		teleport(target, systemConfig.world, safeUnitSpawn(systemConfig, captainState.getTeamId(), laneId));
		var unitHookService = unitHookServiceSupplier.get();
		if (unitHookService != null) {
			unitHookService.assignUnit(target, definition, systemConfig);
		} else {
			matchManager.setCurrentUnit(target.getUuid(), definition.id());
			unitAbilityService.clearPlayerState(target.getUuid());
			unitLoadoutService.applyUnitLoadout(target, definition, textTemplateResolver);
			DisguiseSupport.applyDisguise(target, definition.disguise());
		}
		target.sendMessage(textTemplateResolver.format(textConfig().unitSpawnedMessage.replace("{unit}", definition.displayName())), false);
		var captainSkillService = captainSkillServiceSupplier.get();
		if (captainSkillService != null) {
			captainSkillService.handleSpawnRefund(captain, definition, laneId);
		}
		captain.sendMessage(textTemplateResolver.format(textConfig().captainSpawnSuccessMessage
			.replace("{player}", target.getName().getString())
			.replace("{unit}", definition.displayName())), false);
		playSuccess(captain);
		playSuccess(target);
		return SpawnResult.success(target.getUuid(), definition.id());
	}

	public void maintainActiveUnits(MatchManager matchManager) {
		for (var player : matchManager.getOnlinePlayers()) {
			var state = matchManager.getPlayerState(player.getUuid());
			var unitId = state.getCurrentUnitId();
			if (unitId == null) {
				continue;
			}
			var definition = unitRegistry.get(unitId);
			if (definition == null) {
				continue;
			}
			unitLoadoutService.maintainLoadout(player, definition);
		}
	}

	public void resetPlayer(ServerPlayerEntity player) {
		unitAbilityService.clearPlayerState(player.getUuid());
		DisguiseSupport.clearDisguise(player);
		unitLoadoutService.resetCombatState(player);
	}

	public CaptainManaService getCaptainManaService() {
		return captainManaService;
	}

	public UnitRegistry getUnitRegistry() {
		return unitRegistry;
	}

	public UnitLoadoutService getUnitLoadoutService() {
		return unitLoadoutService;
	}

	public UnitAbilityService getUnitAbilityService() {
		return unitAbilityService;
	}


	private void teleport(ServerPlayerEntity player, String worldId, SystemConfig.PositionConfig position) {
		var world = resolveWorld(player, worldId);
		player.teleportTo(new TeleportTarget(
			world,
			new Vec3d(position.x, position.y, position.z),
			Vec3d.ZERO,
			position.yaw,
			position.pitch,
			TeleportTarget.NO_OP
		));
	}

	private ServerWorld resolveWorld(ServerPlayerEntity player, String worldId) {
		try {
			var key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
			var world = player.getServer().getWorld(key);
			return world != null ? world : player.getServer().getOverworld();
		} catch (Exception ignored) {
			return player.getServer().getOverworld();
		}
	}

	public static LaneId nearestLaneForCaptain(ServerPlayerEntity captain, SystemConfig systemConfig) {
		if (captain == null || systemConfig == null || systemConfig.inGame == null) {
			return LaneId.LANE_1;
		}
		return nearestLaneForPosition(captain.getX(), captain.getZ(), systemConfig);
	}

	static LaneId nearestLaneForPosition(double x, double z, SystemConfig systemConfig) {
		if (systemConfig == null || systemConfig.inGame == null) {
			return LaneId.LANE_1;
		}
		var closestLane = LaneId.LANE_1;
		var closestDistance = squaredDistanceXZ(x, z, centerX(systemConfig.inGame.lane1Region), centerZ(systemConfig.inGame.lane1Region));
		var lane2Distance = squaredDistanceXZ(x, z, centerX(systemConfig.inGame.lane2Region), centerZ(systemConfig.inGame.lane2Region));
		if (lane2Distance < closestDistance) {
			closestLane = LaneId.LANE_2;
			closestDistance = lane2Distance;
		}
		var lane3Distance = squaredDistanceXZ(x, z, centerX(systemConfig.inGame.lane3Region), centerZ(systemConfig.inGame.lane3Region));
		if (lane3Distance < closestDistance) {
			return LaneId.LANE_3;
		}
		return closestLane;
	}

	static SystemConfig.PositionConfig safeUnitSpawn(SystemConfig systemConfig, TeamId teamId, LaneId laneId) {
		var spawn = systemConfig.gameStart.unitSpawnFor(teamId, laneId);
		if (spawn == null || spawn.y < 0.0D) {
			return systemConfig.gameStart.captainSpawnFor(teamId);
		}
		return spawn;
	}

	private static double centerX(SystemConfig.LaneRegionConfig region) {
		return region == null ? 0.0D : (region.minX + region.maxX) / 2.0D;
	}

	private static double centerZ(SystemConfig.LaneRegionConfig region) {
		return region == null ? 0.0D : (region.minZ + region.maxZ) / 2.0D;
	}

	private static double squaredDistanceXZ(double x1, double z1, double x2, double z2) {
		var dx = x1 - x2;
		var dz = z1 - z2;
		return dx * dx + dz * dz;
	}

	private static String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}

	private void playDeny(ServerPlayerEntity player) {
		if (uiSoundService != null) {
			uiSoundService.playUiDeny(player);
		}
	}

	private void playSuccess(ServerPlayerEntity player) {
		if (uiSoundService != null) {
			uiSoundService.playUiConfirm(player);
		}
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	public record SpawnCandidate(UUID playerId, String preferredUnitId, boolean spectator) {
	}

	public record SpawnResult(boolean success, UUID targetPlayerId, String unitId, String message) {
		public static SpawnResult success(UUID targetPlayerId, String unitId) {
			return new SpawnResult(true, targetPlayerId, unitId, null);
		}

		public static SpawnResult error(String message) {
			return new SpawnResult(false, null, null, message);
		}
	}
}
