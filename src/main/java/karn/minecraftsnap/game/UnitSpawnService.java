package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;

import java.util.List;
import java.util.UUID;

public class UnitSpawnService {
	static final String SPAWN_PROTECTION_UNTIL_TICK_KEY = "spawn_protection_until_tick";
	static final String DELAYED_AMMO_RESTOCK_TICK_KEY = "delayed_ammo_restock_tick";
	static final int SPAWN_PROTECTION_TICKS = 60;
	static final long DELAYED_AMMO_RESTOCK_TICKS = 20L;
	private final CaptainManaService captainManaService;
	private final UnitRegistry unitRegistry;
	private final UnitLoadoutService unitLoadoutService;
	private final UnitAbilityService unitAbilityService;
	private final UiSoundService uiSoundService;
	private final java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier;
	private final java.util.function.Supplier<UnitHookService> unitHookServiceSupplier;
	private final UnitSpawnQueueService unitSpawnQueueService;
	private final java.util.Random random = new java.util.Random();

	public UnitSpawnService() {
		this(new CaptainManaService(), null, new UnitLoadoutService(), new UnitAbilityService(), null, null, () -> null, () -> null);
	}

	public UnitSpawnService(
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitLoadoutService unitLoadoutService,
		UnitAbilityService unitAbilityService,
		UnitSpawnQueueService unitSpawnQueueService,
		java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier,
		java.util.function.Supplier<UnitHookService> unitHookServiceSupplier
	) {
		this(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService, null, unitSpawnQueueService, captainSkillServiceSupplier, unitHookServiceSupplier);
	}

	public UnitSpawnService(
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitLoadoutService unitLoadoutService,
		UnitAbilityService unitAbilityService,
		UiSoundService uiSoundService,
		UnitSpawnQueueService unitSpawnQueueService,
		java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier,
		java.util.function.Supplier<UnitHookService> unitHookServiceSupplier
	) {
		this.captainManaService = captainManaService;
		this.unitRegistry = unitRegistry;
		this.unitLoadoutService = unitLoadoutService;
		this.unitAbilityService = unitAbilityService;
		this.uiSoundService = uiSoundService;
		this.unitSpawnQueueService = unitSpawnQueueService;
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
		var captainPlayerState = matchManager.getPlayerState(captain.getUuid());
		if (!captainPlayerState.isCaptain() || captainPlayerState.getTeamId() == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnCaptainOnlyMessage);
		}

		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnUnknownUnitMessage);
		}
		if (captainPlayerState.getFactionId() != definition.factionId()) {
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnWrongFactionMessage);
		}
		var captainManaState = this.captainManaService.getOrCreate(captain.getUuid());
		var laneId = resolveSpawnLane(captain, systemConfig);
		if (!canSpawnOnLane(matchManager, laneId)) {
			playDeny(captain);
			return SpawnResult.error(textConfig().captainSpawnBlockedLaneMessage.replace("{lane}", laneLabel(laneId)));
		}

		var candidate = selectCandidate(unitId, matchManager, captainPlayerState.getTeamId());
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
			if (unitSpawnQueueService != null) {
				unitSpawnQueueService.removePlayer(candidate.playerId());
			}
			playDeny(captain);
			return SpawnResult.error(textConfig().unitSpawnTargetMissingMessage);
		}

		target.changeGameMode(GameMode.ADVENTURE);
		teleport(target, systemConfig.world, safeUnitSpawn(systemConfig, captainPlayerState.getTeamId(), laneId));
		var unitHookService = unitHookServiceSupplier.get();
		if (unitHookService != null) {
			unitHookService.assignUnit(target, definition, systemConfig);
		} else {
			matchManager.setCurrentUnit(target.getUuid(), definition.id());
			unitAbilityService.clearPlayerState(target.getUuid());
			unitLoadoutService.applyUnitLoadout(target, definition, textTemplateResolver);
			DisguiseSupport.applyDisguise(target, definition.disguise());
		}
		applySpawnProtection(target, matchManager);
		target.sendMessage(textTemplateResolver.format(textConfig().unitSpawnedMessage.replace("{unit}", definition.displayName())), false);
		captain.sendMessage(textTemplateResolver.format(textConfig().captainSpawnSuccessMessage
			.replace("{player}", target.getName().getString())
			.replace("{unit}", definition.displayName())), false);
		if (unitSpawnQueueService != null) {
			unitSpawnQueueService.consume(captainPlayerState.getTeamId(), target.getUuid());
		}
		var refundMana = portalManaRefundAmount(captainPlayerState.getFactionId(), captainManaState, laneId, matchManager.getServerTicks());
		if (refundMana > 0) {
			this.captainManaService.restoreMana(captain.getUuid(), refundMana);
			captain.sendMessage(textTemplateResolver.format(textConfig().captainPortalRefundMessage.replace("{mana}", Integer.toString(refundMana))), false);
		}
		var mod = MinecraftSnap.getInstance();
		if (mod != null) {
			mod.getServerStatsRepository().recordUnitPick(definition.id());
		}
		playSuccess(captain);
		playSuccess(target);
		applyCaptainSpawnItemCooldown(captain, systemConfig);
		return SpawnResult.success(target.getUuid(), definition.id());
	}

	private SpawnCandidate selectCandidate(String unitId, MatchManager matchManager, TeamId teamId) {
		if (unitSpawnQueueService != null) {
			var nextPlayerId = unitSpawnQueueService.peekNextPlayer(matchManager, teamId);
			if (nextPlayerId == null) {
				return null;
			}
			var player = matchManager.getServer() == null ? null : matchManager.getServer().getPlayerManager().getPlayer(nextPlayerId);
			var spectator = player == null || player.isSpectator();
			return new SpawnCandidate(nextPlayerId, null, spectator);
		}

		var candidates = matchManager.getOnlineSpectatorUnits(teamId).stream()
			.map(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return new SpawnCandidate(player.getUuid(), state.getPreferredUnitId(), player.isSpectator());
			})
			.toList();
		return selectSpawnCandidate(unitId, candidates);
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
			maintainDelayedAmmoRestock(player, state, definition, matchManager.getServerTicks());
		}
	}

	static boolean usesDelayedAmmoRestock(String unitId) {
		return "skeleton".equals(unitId) || "stray".equals(unitId) || "bogged".equals(unitId);
	}

	static Long nextAmmoRestockTick(String unitId, boolean hasAmmo, Long pendingTick, long serverTicks) {
		if (!usesDelayedAmmoRestock(unitId) || hasAmmo) {
			return null;
		}
		return pendingTick != null ? pendingTick : serverTicks + DELAYED_AMMO_RESTOCK_TICKS;
	}

	static boolean shouldRestockAmmo(String unitId, boolean hasAmmo, Long pendingTick, long serverTicks) {
		return usesDelayedAmmoRestock(unitId)
			&& !hasAmmo
			&& pendingTick != null
			&& serverTicks >= pendingTick;
	}

	private void maintainDelayedAmmoRestock(ServerPlayerEntity player, PlayerMatchState state, UnitDefinition definition, long serverTicks) {
		if (player == null || state == null || definition == null) {
			return;
		}
		var ammoStack = unitLoadoutService.ammoStack(definition.ammoType());
		if (ammoStack.isEmpty()) {
			state.removeUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY);
			return;
		}
		boolean hasAmmo = player.getInventory().contains(ammoStack);
		if (!usesDelayedAmmoRestock(definition.id())) {
			if (!hasAmmo) {
				player.getInventory().insertStack(ammoStack.copy());
			}
			state.removeUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY);
			return;
		}
		var pendingTick = state.getUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY);
		var nextTick = nextAmmoRestockTick(definition.id(), hasAmmo, pendingTick, serverTicks);
		if (nextTick == null) {
			state.removeUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY);
			return;
		}
		if (shouldRestockAmmo(definition.id(), hasAmmo, nextTick, serverTicks)) {
			player.getInventory().insertStack(ammoStack.copy());
			state.removeUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY);
			return;
		}
		state.setUnitRuntimeLong(DELAYED_AMMO_RESTOCK_TICK_KEY, nextTick);
	}

	public void tickSpawnProtection(MinecraftServer server, MatchManager matchManager) {
		if (server == null || matchManager == null) {
			return;
		}
		long serverTicks = matchManager.getServerTicks();
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			var protectedUntil = state.getUnitRuntimeLong(SPAWN_PROTECTION_UNTIL_TICK_KEY);
			if (protectedUntil == null || protectedUntil <= 0L) {
				continue;
			}
			if (!state.isUnit() || state.getCurrentUnitId() == null || player.isSpectator() || serverTicks >= protectedUntil) {
				state.removeUnitRuntimeLong(SPAWN_PROTECTION_UNTIL_TICK_KEY);
				player.setInvulnerable(false);
				continue;
			}
			player.setInvulnerable(true);
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 4, true, false, false));
			if (serverTicks % 5L == 0L) {
				player.getWorld().spawnParticles(player, ParticleTypes.HAPPY_VILLAGER, true, false, player.getX(), player.getY() + 2.1D, player.getZ(), 4, 0.25D, 0.15D, 0.25D, 0.0D);
			}
		}
	}

	public void resetPlayer(ServerPlayerEntity player) {
		player.setInvulnerable(false);
		unitLoadoutService.resetPlayerAttributes(player);
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

	static boolean canSpawnOnLane(MatchManager matchManager, LaneId laneId) {
		return matchManager != null && laneId != null && matchManager.isLaneRevealed(laneId);
	}

	static LaneId resolveSpawnLane(ServerPlayerEntity captain, SystemConfig systemConfig) {
		return nearestLaneForCaptain(captain, systemConfig);
	}

	static LaneId activeNetherPortalLane(FactionId captainFaction, CaptainState captainState, long serverTicks) {
		if (captainFaction != FactionId.NETHER || captainState == null || captainState.getNetherPortalEndTick() <= serverTicks) {
			return null;
		}
		return captainState.getNetherPortalLaneId();
	}

	static int portalManaRefundAmount(FactionId captainFaction, CaptainState captainState, LaneId spawnLaneId, long serverTicks) {
		if (spawnLaneId == null) {
			return 0;
		}
		var portalLane = activeNetherPortalLane(captainFaction, captainState, serverTicks);
		if (portalLane != spawnLaneId) {
			return 0;
		}
		return 1;
	}

	private void applyCaptainSpawnItemCooldown(ServerPlayerEntity captain, SystemConfig systemConfig) {
		if (captain == null || systemConfig == null || systemConfig.gameStart == null) {
			return;
		}
		var cooldownTicks = Math.max(0, systemConfig.gameStart.unitSpawnItemCooldownTicks);
		if (cooldownTicks > 0) {
			captain.getItemCooldownManager().set(Items.BELL.getDefaultStack(), cooldownTicks);
		}
	}

	static SystemConfig.PositionConfig safeUnitSpawn(SystemConfig systemConfig, TeamId teamId, LaneId laneId) {
		var spawn = systemConfig.gameStart.unitSpawnFor(teamId, laneId);
		if (spawn == null || spawn.y < 0.0D) {
			return systemConfig.gameStart.captainSpawnFor(teamId);
		}
		return spawn;
	}

	void applySpawnProtection(ServerPlayerEntity target, MatchManager matchManager) {
		if (target == null || matchManager == null) {
			return;
		}
		matchManager.getPlayerState(target.getUuid()).setUnitRuntimeLong(SPAWN_PROTECTION_UNTIL_TICK_KEY, matchManager.getServerTicks() + SPAWN_PROTECTION_TICKS);
		target.setInvulnerable(true);
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, SPAWN_PROTECTION_TICKS, 4, true, false, false));
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
