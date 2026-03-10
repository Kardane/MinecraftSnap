package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
	private final Set<UUID> pendingSpectators = new HashSet<>();
	private final Map<UUID, Long> laneWarningTicks = new HashMap<>();

	public InGameRuleService(MatchManager matchManager, StatsRepository statsRepository, TextTemplateResolver textTemplateResolver) {
		this(matchManager, statsRepository, textTemplateResolver, null, null, null, null);
	}

	public InGameRuleService(
		MatchManager matchManager,
		StatsRepository statsRepository,
		TextTemplateResolver textTemplateResolver,
		UnitSpawnService unitSpawnService,
		CaptainManaService captainManaService,
		UnitRegistry unitRegistry,
		UnitAbilityService unitAbilityService
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.textTemplateResolver = textTemplateResolver;
		this.unitSpawnService = unitSpawnService;
		this.captainManaService = captainManaService;
		this.unitRegistry = unitRegistry;
		this.unitAbilityService = unitAbilityService;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		server.getGameRules().get(GameRules.NATURAL_REGENERATION).set(matchManager.getPhase() != MatchPhase.GAME_RUNNING, server);
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

	public boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayerEntity victim)) {
			return true;
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
			return true;
		}

		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.UNIT) {
			return true;
		}
		if (pendingSpectators.contains(player.getUuid())) {
			return false;
		}

		var attacker = source.getAttacker() instanceof ServerPlayerEntity attackerPlayer ? attackerPlayer : null;
		if (unitAbilityService != null && unitRegistry != null) {
			unitAbilityService.handleUnitDeath(player, matchManager, unitRegistry);
		}
		recordKillAndDeath(player.getUuid(), player.getName().getString(), attacker == null ? null : attacker.getUuid(), attacker == null ? null : attacker.getName().getString());
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
				rewardKillCurrency(killerId, killerName, killerState);
				if (unitAbilityService != null && captainManaService != null && unitRegistry != null) {
					unitAbilityService.handleEnemyKill(killerId, matchManager, captainManaService, unitRegistry);
				}
			}
		}
	}

	public boolean shouldBlockClosedLane(PlayerMatchState state, LaneId laneId) {
		return state.getRoleType() == RoleType.UNIT && !matchManager.isLaneRevealed(laneId);
	}

	public LaneId findContainingLane(String worldId, double x, double y, double z, SystemConfig.InGameConfig config) {
		if (contains(config.lane1Region, worldId, x, y, z)) {
			return LaneId.LANE_1;
		}
		if (contains(config.lane2Region, worldId, x, y, z)) {
			return LaneId.LANE_2;
		}
		if (contains(config.lane3Region, worldId, x, y, z)) {
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
		var laneId = findContainingLane(player.getWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), systemConfig.inGame);
		if (laneId == null || !shouldBlockClosedLane(state, laneId)) {
			return;
		}

		var lastTick = laneWarningTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE);
		if (matchManager.getServerTicks() - lastTick >= systemConfig.inGame.laneWarningCooldownTicks) {
			player.sendMessage(textTemplateResolver.format(systemConfig.inGame.closedLaneMessage), true);
			laneWarningTicks.put(player.getUuid(), matchManager.getServerTicks());
		}

		var spawn = systemConfig.gameStart.unitSpawn;
		net.minecraft.server.world.ServerWorld world;
		try {
			world = player.getServer().getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(spawn.world)));
		} catch (Exception ignored) {
			world = null;
		}
		if (world == null) {
			world = player.getServer().getOverworld();
		}
		player.teleportTo(new net.minecraft.world.TeleportTarget(
			world,
			new net.minecraft.util.math.Vec3d(spawn.x, spawn.y, spawn.z),
			net.minecraft.util.math.Vec3d.ZERO,
			spawn.yaw,
			spawn.pitch,
			net.minecraft.world.TeleportTarget.NO_OP
		));
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

	private boolean contains(SystemConfig.LaneRegionConfig region, String worldId, double x, double y, double z) {
		return region != null
			&& region.world.equals(worldId)
			&& x >= region.minX && x <= region.maxX
			&& y >= region.minY && y <= region.maxY
			&& z >= region.minZ && z <= region.maxZ;
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
}
