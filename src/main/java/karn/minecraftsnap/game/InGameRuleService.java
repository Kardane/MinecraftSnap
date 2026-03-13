package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InGameRuleService {
	private final MatchManager matchManager;
	private final StatsRepository statsRepository;
	private final TextTemplateResolver textTemplateResolver;
	private final UnitSpawnService unitSpawnService;
	private final CaptainManaService captainManaService;
	private final UnitRegistry unitRegistry;
	private final UnitAbilityService unitAbilityService;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final UnitHookService unitHookService;
	private final UiSoundService uiSoundService;
	private SystemConfig lastSystemConfig = new SystemConfig();
	private final Set<UUID> pendingSpectators = new HashSet<>();
	private final Map<UUID, Long> laneWarningTicks = new HashMap<>();

	public InGameRuleService(MatchManager matchManager, StatsRepository statsRepository, TextTemplateResolver textTemplateResolver) {
		this(matchManager, statsRepository, textTemplateResolver, null, null, null, null, null, null, null);
	}

	public InGameRuleService(
		MatchManager matchManager,
		StatsRepository statsRepository,
		TextTemplateResolver textTemplateResolver,
		UnitSpawnService unitSpawnService,
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitAbilityService unitAbilityService,
		LaneRuntimeRegistry laneRuntimeRegistry,
		UnitHookService unitHookService,
		UiSoundService uiSoundService
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.textTemplateResolver = textTemplateResolver;
		this.unitSpawnService = unitSpawnService;
		this.captainManaService = captainManaService;
		this.unitRegistry = unitRegistry;
		this.unitAbilityService = unitAbilityService;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.unitHookService = unitHookService;
		this.uiSoundService = uiSoundService;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		lastSystemConfig = systemConfig;
		server.getGameRules().get(GameRules.NATURAL_REGENERATION).set(matchManager.getPhase() != MatchPhase.GAME_RUNNING, server);
		enforceCaptainFlight(server, systemConfig);
		enforceCaptainInvisibility(server);
		disableNonOperatorFlightOnGameEnd(server);
		if (isCaptainFlightPhase(matchManager.getPhase())) {
			for (var player : server.getPlayerManager().getPlayerList()) {
				applyCaptainFloor(player, systemConfig);
			}
		}
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			player.getHungerManager().setFoodLevel(20);
			player.getHungerManager().setSaturationLevel(20.0f);
			applyLaneRestriction(player, systemConfig);
		}

		applyPendingSpectators(server);
	}

	public boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity victim)) {
			return true;
		}
		if (unitHookService != null) {
			unitHookService.handleDamaged(victim, source, amount, nullSafeSystemConfig());
		}
		notifyDamagedHook(victim, source, amount);
		if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
			if (unitHookService != null) {
				unitHookService.handleAttack(attacker, victim, amount, nullSafeSystemConfig());
			}
			notifyAttackHook(attacker, victim, amount);
		}
		if (matchManager.getPlayerState(victim.getUuid()).isCaptain()) {
			return false;
		}

		if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) {
			return true;
		}

		var victimState = matchManager.getPlayerState(victim.getUuid());
		var attackerState = matchManager.getPlayerState(attacker.getUuid());
		return isDamageAllowed(victimState, attackerState, matchManager.getPhase());
	}

	public boolean isDamageAllowed(PlayerMatchState victimState, PlayerMatchState attackerState, MatchPhase phase) {
		if (phase != MatchPhase.GAME_RUNNING) {
			return false;
		}
		if (victimState.getRoleType() == RoleType.SPECTATOR || attackerState.getRoleType() == RoleType.SPECTATOR) {
			return false;
		}
		if (victimState.getTeamId() == null || attackerState.getTeamId() == null) {
			return false;
		}
		return victimState.getTeamId() != attackerState.getTeamId();
	}

	public boolean handlePotentialDeath(LivingEntity entity, DamageSource source, float damageAmount) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}

		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return !matchManager.getPlayerState(player.getUuid()).isCaptain() || !isCaptainProtectedPhase(matchManager.getPhase());
		}

		var state = matchManager.getPlayerState(player.getUuid());
		if (state.isCaptain()) {
			player.setHealth(player.getMaxHealth());
			return false;
		}
		if (state.getRoleType() != RoleType.UNIT) {
			return true;
		}
		if (pendingSpectators.contains(player.getUuid())) {
			return false;
		}
		if (unitHookService != null) {
			unitHookService.handleDeath(player, source, nullSafeSystemConfig());
		}

		var attacker = source.getAttacker() instanceof ServerPlayerEntity attackerPlayer ? attackerPlayer : null;
		recordKillAndDeath(player.getUuid(), player.getName().getString(), attacker == null ? null : attacker.getUuid(), attacker == null ? null : attacker.getName().getString());
		if (attacker != null && unitHookService != null) {
			unitHookService.handleKill(attacker, player, nullSafeSystemConfig());
		}
		broadcastCustomDeathMessage(player, attacker, source);
		notifyDeathHook(player, source);
		matchManager.clearCurrentUnit(player.getUuid());
		if (unitSpawnService != null) {
			unitSpawnService.resetPlayer(player);
		}
		pendingSpectators.add(player.getUuid());
		player.setHealth(player.getMaxHealth());
		player.clearStatusEffects();
		return false;
	}

	public void recordKillAndDeath(UUID victimId, String victimName, UUID killerId, String killerName) {
		statsRepository.addDeath(victimId, victimName, 1);
		pendingSpectators.add(victimId);
		if (killerId != null && killerName != null) {
			var victimState = matchManager.getPlayerState(victimId);
			var killerState = matchManager.getPlayerState(killerId);
			if (victimState.getTeamId() != null && killerState.getTeamId() != null && victimState.getTeamId() != killerState.getTeamId()) {
				statsRepository.addKill(killerId, killerName, 1);
				statsRepository.addLadder(killerId, killerName, 3);
				if (unitHookService == null) {
					rewardKillCurrency(killerId, killerName, killerState);
				}
			}
		}
	}

	public boolean shouldBlockClosedLane(PlayerMatchState state, LaneId laneId) {
		return state.getRoleType() == RoleType.UNIT && !matchManager.isLaneRevealed(laneId);
	}

	public LaneId findContainingLane(String configuredWorldId, String worldId, double x, double y, double z, SystemConfig.InGameConfig config) {
		if (!configuredWorldId.equals(worldId)) {
			return null;
		}
		if (contains(config.lane1Region, x, y, z)) {
			return LaneId.LANE_1;
		}
		if (contains(config.lane2Region, x, y, z)) {
			return LaneId.LANE_2;
		}
		if (contains(config.lane3Region, x, y, z)) {
			return LaneId.LANE_3;
		}
		return null;
	}

	public boolean isPendingSpectator(UUID playerId) {
		return pendingSpectators.contains(playerId);
	}

	public StatsRepository getStatsRepository() {
		return statsRepository;
	}

	private void applyLaneRestriction(ServerPlayerEntity player, SystemConfig systemConfig) {
		var state = matchManager.getPlayerState(player.getUuid());
		var laneId = findContainingLane(systemConfig.world, player.getWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), systemConfig.inGame);
		if (laneId == null || !shouldBlockClosedLane(state, laneId)) {
			return;
		}
		if (isInsideCaptureRegion(player, systemConfig.capture)) {
			return;
		}

		var lastTick = laneWarningTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE);
		if (matchManager.getServerTicks() - lastTick >= systemConfig.inGame.laneWarningCooldownTicks) {
			player.sendMessage(textTemplateResolver.format(systemConfig.inGame.closedLaneMessage), true);
			if (uiSoundService != null) {
				uiSoundService.playEventWarning(player);
			}
			laneWarningTicks.put(player.getUuid(), matchManager.getServerTicks());
		}
	}

	private void applyPendingSpectators(MinecraftServer server) {
		if (pendingSpectators.isEmpty()) {
			return;
		}

		var processed = new HashSet<UUID>();
		for (var playerId : pendingSpectators) {
			var player = server.getPlayerManager().getPlayer(playerId);
			if (player == null) {
				matchManager.clearCurrentUnit(playerId);
				processed.add(playerId);
				continue;
			}

			player.changeGameMode(GameMode.SPECTATOR);
			if (unitSpawnService != null) {
				unitSpawnService.resetPlayer(player);
			}
			player.getInventory().clear();
			matchManager.clearCurrentUnit(playerId);
			processed.add(playerId);
		}
		pendingSpectators.removeAll(processed);
	}

	private void rewardKillCurrency(UUID playerId, String playerName, PlayerMatchState state) {
		if (state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null) {
			return;
		}
		if (state.getFactionId() == FactionId.VILLAGER) {
			state.addEmeralds(1);
			statsRepository.addEmeralds(playerId, playerName, 1);
		} else if (state.getFactionId() == FactionId.NETHER) {
			state.addGoldIngots(1);
			statsRepository.addGoldIngots(playerId, playerName, 1);
		}
	}

	private boolean contains(SystemConfig.LaneRegionConfig region, double x, double y, double z) {
		return region != null
			&& x >= region.minX && x <= region.maxX
			&& y >= region.minY && y <= region.maxY
			&& z >= region.minZ && z <= region.maxZ;
	}

	private boolean isInsideCaptureRegion(ServerPlayerEntity player, SystemConfig.CaptureConfig captureConfig) {
		if (player == null || captureConfig == null) {
			return false;
		}
		return CapturePointService.contains(captureConfig.lane1, player.getPos())
			|| CapturePointService.contains(captureConfig.lane2, player.getPos())
			|| CapturePointService.contains(captureConfig.lane3, player.getPos());
	}

	private net.minecraft.server.world.ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		try {
			var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	private void enforceCaptainFlight(MinecraftServer server, SystemConfig systemConfig) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (player.isSpectator()) {
				boolean changed = false;
				if (!player.getAbilities().allowFlying) {
					player.getAbilities().allowFlying = true;
					changed = true;
				}
				if (!player.getAbilities().flying) {
					player.getAbilities().flying = true;
					changed = true;
				}
				if (player.getAbilities().getFlySpeed() != systemConfig.inGame.defaultFlySpeed) {
					player.getAbilities().setFlySpeed(systemConfig.inGame.defaultFlySpeed);
					changed = true;
				}
				if (changed) {
					player.sendAbilitiesUpdate();
				}
				continue;
			}
			boolean changed = false;
			var captainFlight = state.isCaptain() && isCaptainFlightPhase(matchManager.getPhase());
			if (captainFlight) {
				if (!player.getAbilities().allowFlying) {
					player.getAbilities().allowFlying = true;
					player.getAbilities().flying = true;
					changed = true;
				}
				if (player.getAbilities().getFlySpeed() != systemConfig.inGame.captainFlySpeed) {
					player.getAbilities().setFlySpeed(systemConfig.inGame.captainFlySpeed);
					changed = true;
				}
				player.fallDistance = 0.0f;
			} else {
				if (!player.hasPermissionLevel(2) && player.getAbilities().flying) {
					player.getAbilities().flying = false;
					changed = true;
				}
				if (!player.hasPermissionLevel(2) && player.getAbilities().allowFlying) {
					player.getAbilities().allowFlying = false;
					changed = true;
				}
				if (player.getAbilities().getFlySpeed() != systemConfig.inGame.defaultFlySpeed) {
					player.getAbilities().setFlySpeed(systemConfig.inGame.defaultFlySpeed);
					changed = true;
				}
			}
			if (changed) {
				player.sendAbilitiesUpdate();
			}
		}
	}

	private void enforceCaptainInvisibility(MinecraftServer server) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (!state.isCaptain()) {
				continue;
			}
			if (isCaptainGamePhase(matchManager.getPhase())) {
				var effect = player.getStatusEffect(StatusEffects.INVISIBILITY);
				if (effect == null || effect.getDuration() <= 40) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, false));
				}
			} else {
				player.removeStatusEffect(StatusEffects.INVISIBILITY);
			}
		}
	}

	private void disableNonOperatorFlightOnGameEnd(MinecraftServer server) {
		if (matchManager.getPhase() != MatchPhase.GAME_END) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			if (player.hasPermissionLevel(2) || player.isSpectator()) {
				continue;
			}
			boolean changed = false;
			if (player.getAbilities().flying) {
				player.getAbilities().flying = false;
				changed = true;
			}
			if (player.getAbilities().allowFlying) {
				player.getAbilities().allowFlying = false;
				changed = true;
			}
			if (changed) {
				player.sendAbilitiesUpdate();
			}
		}
	}

	private void applyCaptainFloor(ServerPlayerEntity player, SystemConfig systemConfig) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (!state.isCaptain() || state.getTeamId() == null || !isCaptainFlightPhase(matchManager.getPhase()) || player.getY() >= systemConfig.inGame.captainMinY) {
			return;
		}
		var spawn = systemConfig.gameStart.captainSpawnFor(state.getTeamId());
		var world = resolveWorld(player.getServer(), systemConfig.world);
		player.teleportTo(new net.minecraft.world.TeleportTarget(
			world,
			new net.minecraft.util.math.Vec3d(spawn.x, spawn.y, spawn.z),
			net.minecraft.util.math.Vec3d.ZERO,
			spawn.yaw,
			spawn.pitch,
			net.minecraft.world.TeleportTarget.NO_OP
		));
		player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
		player.fallDistance = 0.0f;
	}

	static boolean isCaptainFlightPhase(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING;
	}

	static boolean isCaptainProtectedPhase(MatchPhase phase) {
		return isCaptainGamePhase(phase);
	}

	static boolean isCaptainGamePhase(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING || phase == MatchPhase.GAME_END;
	}

	private void notifyDamagedHook(ServerPlayerEntity victim, DamageSource source, float amount) {
		var context = createContext(victim);
		if (context != null) {
			context.laneRuntime().biomeEffect().onDamaged(context, victim, source, amount);
		}
	}

	private void notifyAttackHook(ServerPlayerEntity attacker, ServerPlayerEntity victim, float amount) {
		var context = createContext(attacker);
		if (context != null) {
			context.laneRuntime().biomeEffect().onAttack(context, attacker, victim, amount);
		}
	}

	private void notifyDeathHook(ServerPlayerEntity victim, DamageSource source) {
		var context = createContext(victim);
		if (context != null) {
			context.laneRuntime().biomeEffect().onDeath(context, victim, source);
		}
	}

	private void broadcastCustomDeathMessage(ServerPlayerEntity victim, ServerPlayerEntity attacker, DamageSource source) {
		var server = matchManager.getServer();
		if (server == null || victim == null) {
			return;
		}
		var template = nullSafeSystemConfig().announcements.customDeathMessage;
		if (template == null || template.isBlank()) {
			return;
		}
		var killerName = attacker != null ? attacker.getName().getString() : "환경";
		server.getPlayerManager().broadcast(
			textTemplateResolver.format(template
				.replace("{victim}", victim.getName().getString())
				.replace("{killer}", killerName)
				.replace("{damage_type}", source == null ? "unknown" : source.getName())),
			false
		);
		if (uiSoundService != null) {
			uiSoundService.playEventWarning(server);
		}
	}

	private BiomeRuntimeContext createContext(ServerPlayerEntity player) {
		if (laneRuntimeRegistry == null || player == null) {
			return null;
		}
		var runtime = laneRuntimeRegistry.findByPlayer(player);
		if (runtime == null || !runtime.hasActiveBiome()) {
			return null;
		}
		var elapsedSeconds = matchManager.getTotalSeconds() - matchManager.getRemainingSeconds();
		return new BiomeRuntimeContext(
			player.getServer(),
			(net.minecraft.server.world.ServerWorld) player.getWorld(),
			runtime,
			runtime.biomeEntry(),
			textTemplateResolver,
			matchManager.getServerTicks(),
			elapsedSeconds
		);
	}

	private SystemConfig nullSafeSystemConfig() {
		return lastSystemConfig == null ? new SystemConfig() : lastSystemConfig;
	}

}
