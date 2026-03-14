package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CaptainSkillService {
	static final int VILLAGER_COST = 4;
	static final int MONSTER_COST = 4;
	static final int NETHER_COST = 5;
	static final int VILLAGER_COOLDOWN_SECONDS = 60;
	static final int MONSTER_COOLDOWN_SECONDS = 60;
	static final int NETHER_COOLDOWN_SECONDS = 45;
	static final long RAIN_DURATION_TICKS = 20L * 120L;
	static final long THUNDER_DURATION_TICKS = 20L * 60L;
	static final long PORTAL_DURATION_TICKS = 20L * 30L;

	private final MatchManager matchManager;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final CaptainWeatherGuiService captainWeatherGuiService;
	private final CaptainManaService captainManaService;
	private final TextTemplateResolver textTemplateResolver;
	private final UiSoundService uiSoundService;
	private final Random random;

	public CaptainSkillService(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService
	) {
		this(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, new Random());
	}

	CaptainSkillService(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random
	) {
		this.matchManager = matchManager;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.captainWeatherGuiService = captainWeatherGuiService;
		this.captainManaService = captainManaService;
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
		this.random = random;
	}

	public boolean useCaptainSkill(ServerPlayerEntity captain, SystemConfig systemConfig) {
		if (captain == null || systemConfig == null) {
			return false;
		}
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() == null) {
			return false;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getSkillCooldownSeconds() > 0) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillCooldownMessage.replace("{seconds}", Integer.toString(captainState.getSkillCooldownSeconds()))), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		if (captainState.getCurrentMana() < skillCostFor(state.getFactionId())) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillInsufficientManaMessage), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		return switch (state.getFactionId()) {
			case VILLAGER -> activateVillagerSkill(captain, state, systemConfig);
			case MONSTER -> {
				openMonsterWeatherGui(captain, systemConfig);
				yield true;
			}
			case NETHER -> activateNetherSkill(captain, state, systemConfig);
		};
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null) {
			return;
		}
		if (!isCaptainSkillPhase(matchManager.getPhase())) {
			clearRuntimeStates(server, systemConfig);
			return;
		}
		tickMonsterWeather(server, systemConfig);
		tickNetherPortals(server, systemConfig);
	}

	public void handleSpawnRefund(ServerPlayerEntity captain, UnitDefinition definition, LaneId laneId) {
		if (captain == null || definition == null || laneId == null) {
			return;
		}
		var state = matchManager.getPlayerState(captain.getUuid());
		if (state.getFactionId() != FactionId.NETHER) {
			return;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getNetherPortalLaneId() != laneId || !isPortalActive(captainState, matchManager.getServerTicks())) {
			return;
		}
		var refundMana = refundAmount(definition.cost());
		var refundCooldown = refundAmount(definition.spawnCooldownSeconds());
		captainManaService.refundSpawnResources(captain.getUuid(), refundMana, refundCooldown);
		if (refundMana > 0 || refundCooldown > 0) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainPortalRefundMessage
				.replace("{mana}", Integer.toString(refundMana))
				.replace("{cooldown}", Integer.toString(refundCooldown))), true);
		}
	}

	static int skillCostFor(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER, MONSTER -> 4;
			case NETHER -> 5;
		};
	}

	static int skillCooldownFor(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER, MONSTER -> 60;
			case NETHER -> 45;
		};
	}

	static int refundAmount(int value) {
		return Math.max(0, (int) Math.floor(value * 0.4D));
	}

	private boolean activateVillagerSkill(ServerPlayerEntity captain, PlayerMatchState captainMatchState, SystemConfig systemConfig) {
		var targetLane = UnitSpawnService.nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(targetLane)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().closedLaneMessage), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		var targets = findRecallTargets(captainMatchState.getTeamId(), targetLane);
		if (targets.isEmpty()) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerNoTargetMessage), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		if (!captainManaService.trySpendForSkill(captain.getUuid(), VILLAGER_COST, VILLAGER_COOLDOWN_SECONDS)) {
			return false;
		}
		var targetSpawn = UnitSpawnService.safeUnitSpawn(systemConfig, captainMatchState.getTeamId(), targetLane);
		var world = resolveWorld(captain, systemConfig.world);
		var targetPos = new Vec3d(targetSpawn.x, targetSpawn.y, targetSpawn.z);
		for (var player : targets) {
			spawnLineParticles(world, player.getPos(), targetPos, ParticleTypes.END_ROD);
			player.teleportTo(new TeleportTarget(world, targetPos, Vec3d.ZERO, targetSpawn.yaw, targetSpawn.pitch, TeleportTarget.NO_OP));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 30, 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 30, 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 30, 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 30, 0));
			player.setHealth(player.getMaxHealth());
		}
		playGlobal(world, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 3.0f, 1.0f);
		captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerSuccessMessage.replace("{lane}", laneLabel(targetLane))), false);
		if (uiSoundService != null) {
			uiSoundService.playEventSuccess(captain);
		}
		return true;
	}

	private void openMonsterWeatherGui(ServerPlayerEntity captain, SystemConfig systemConfig) {
		var current = currentMonsterWeatherOption();
		var remainingSeconds = currentMonsterWeatherRemainingSeconds();
		captainWeatherGuiService.open(
			captain,
			textConfig(),
			current,
			remainingSeconds,
			option -> activateMonsterWeather(captain, option, systemConfig)
		);
	}

	private void activateMonsterWeather(ServerPlayerEntity captain, CaptainWeatherGuiService.WeatherOption option, SystemConfig systemConfig) {
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() != FactionId.MONSTER) {
			return;
		}
		if (!captainManaService.trySpendForSkill(captain.getUuid(), MONSTER_COST, MONSTER_COOLDOWN_SECONDS)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillCooldownMessage.replace("{seconds}", Integer.toString(captainManaService.getOrCreate(captain.getUuid()).getSkillCooldownSeconds()))), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return;
		}
		clearMonsterWeatherStates(resolveWorld(captain, systemConfig.world));
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		captainState.setMonsterWeatherType(option.name());
		captainState.setMonsterWeatherLastProcessedSecond(-1);
		captainState.setMonsterWeatherEndTick(switch (option) {
			case CLEAR -> 0L;
			case RAIN -> matchManager.getServerTicks() + RAIN_DURATION_TICKS;
			case THUNDER -> matchManager.getServerTicks() + THUNDER_DURATION_TICKS;
		});
		applyWorldWeather(resolveWorld(captain, systemConfig.world), option);
		playGlobal(resolveWorld(captain, systemConfig.world), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 3.0f, 1.0f);
		captain.sendMessage(textTemplateResolver.format(textConfig().captainMonsterWeatherSuccessMessage.replace("{weather}", weatherLabel(option))), false);
		if (uiSoundService != null) {
			uiSoundService.playEventSuccess(captain);
		}
	}

	private boolean activateNetherSkill(ServerPlayerEntity captain, PlayerMatchState captainMatchState, SystemConfig systemConfig) {
		var laneId = UnitSpawnService.nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(laneId)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().closedLaneMessage), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		if (!captainManaService.trySpendForSkill(captain.getUuid(), NETHER_COST, NETHER_COOLDOWN_SECONDS)) {
			return false;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		captainState.setNetherPortalLaneId(laneId);
		captainState.setNetherPortalEndTick(matchManager.getServerTicks() + PORTAL_DURATION_TICKS);
		captainState.setNetherPortalLastProcessedSecond(-1);
		var world = resolveWorld(captain, systemConfig.world);
		playGlobal(world, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 3.0f, 1.0f);
		spawnPortalBurst(world, UnitSpawnService.safeUnitSpawn(systemConfig, captainMatchState.getTeamId(), laneId));
		captain.sendMessage(textTemplateResolver.format(textConfig().captainNetherPortalSuccessMessage.replace("{lane}", laneLabel(laneId))), false);
		if (uiSoundService != null) {
			uiSoundService.playEventSuccess(captain);
		}
		return true;
	}

	private void tickMonsterWeather(MinecraftServer server, SystemConfig systemConfig) {
		var active = activeMonsterWeatherCaptain();
		if (active == null) {
			return;
		}
		var captain = server.getPlayerManager().getPlayer(active);
		if (captain == null) {
			return;
		}
		var captainState = captainManaService.getOrCreate(active);
		var option = parseWeather(captainState.getMonsterWeatherType());
		var world = resolveWorld(captain, systemConfig.world);
		if (option != CaptainWeatherGuiService.WeatherOption.CLEAR
			&& captainState.getMonsterWeatherEndTick() > 0L
			&& matchManager.getServerTicks() >= captainState.getMonsterWeatherEndTick()) {
			captainState.clearMonsterWeather();
			applyWorldWeather(world, CaptainWeatherGuiService.WeatherOption.CLEAR);
			return;
		}
		var currentSecond = (int) (matchManager.getServerTicks() / 20L);
		if (captainState.getMonsterWeatherLastProcessedSecond() == currentSecond) {
			return;
		}
		captainState.setMonsterWeatherLastProcessedSecond(currentSecond);
		var allies = teamUnits(matchManager.getPlayerState(active).getTeamId());
		if (option == CaptainWeatherGuiService.WeatherOption.RAIN && currentSecond % 5 == 0) {
			for (var ally : allies) {
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 6, 0));
			}
		}
		if (option == CaptainWeatherGuiService.WeatherOption.THUNDER) {
			if (currentSecond % 3 == 0) {
				for (var ally : allies) {
					ally.heal(2.0F);
					ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 4, 0));
				}
			}
			if (random.nextDouble() < 0.03D) {
				var targets = enemyUnits(matchManager.getPlayerState(active).getTeamId());
				if (!targets.isEmpty()) {
					var target = targets.get(random.nextInt(targets.size()));
					spawnLightningStrike(world, target);
				}
			}
		}
	}

	private void tickNetherPortals(MinecraftServer server, SystemConfig systemConfig) {
		for (var captainId : matchManager.getCaptainIds()) {
			var captain = server.getPlayerManager().getPlayer(captainId);
			if (captain == null) {
				continue;
			}
			var state = matchManager.getPlayerState(captainId);
			if (state.getFactionId() != FactionId.NETHER) {
				continue;
			}
			var captainState = captainManaService.getOrCreate(captainId);
			if (!isPortalActive(captainState, matchManager.getServerTicks())) {
				captainState.clearNetherPortal();
				continue;
			}
			var laneId = captainState.getNetherPortalLaneId();
			if (laneId == null) {
				continue;
			}
			var world = resolveWorld(captain, systemConfig.world);
			var spawn = UnitSpawnService.safeUnitSpawn(systemConfig, state.getTeamId(), laneId);
			if (matchManager.getServerTicks() % 10L == 0L) {
				spawnPortalBurst(world, spawn);
			}
			var currentSecond = (int) (matchManager.getServerTicks() / 20L);
			if (captainState.getNetherPortalLastProcessedSecond() == currentSecond) {
				continue;
			}
			captainState.setNetherPortalLastProcessedSecond(currentSecond);
			for (var ally : laneUnits(state.getTeamId(), laneId)) {
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0));
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0));
			}
		}
	}

	private void clearRuntimeStates(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null) {
			return;
		}
		for (var captainId : matchManager.getCaptainIds()) {
			captainManaService.clearRuntimeState(captainId);
		}
		var world = resolveWorld(server, systemConfig.world);
		applyWorldWeather(world, CaptainWeatherGuiService.WeatherOption.CLEAR);
	}

	private void clearMonsterWeatherStates(ServerWorld world) {
		for (var captainId : matchManager.getCaptainIds()) {
			captainManaService.getOrCreate(captainId).clearMonsterWeather();
		}
		applyWorldWeather(world, CaptainWeatherGuiService.WeatherOption.CLEAR);
	}

	private java.util.UUID activeMonsterWeatherCaptain() {
		return matchManager.getCaptainIds().stream()
			.filter(captainId -> {
				var state = captainManaService.getOrCreate(captainId);
				return !state.getMonsterWeatherType().isBlank();
			})
			.findFirst()
			.orElse(null);
	}

	private CaptainWeatherGuiService.WeatherOption currentMonsterWeatherOption() {
		var captainId = activeMonsterWeatherCaptain();
		if (captainId == null) {
			return CaptainWeatherGuiService.WeatherOption.CLEAR;
		}
		return parseWeather(captainManaService.getOrCreate(captainId).getMonsterWeatherType());
	}

	private int currentMonsterWeatherRemainingSeconds() {
		var captainId = activeMonsterWeatherCaptain();
		if (captainId == null) {
			return 0;
		}
		var endTick = captainManaService.getOrCreate(captainId).getMonsterWeatherEndTick();
		if (endTick <= 0L) {
			return 0;
		}
		return Math.max(0, (int) Math.ceil((endTick - matchManager.getServerTicks()) / 20.0D));
	}

	private CaptainWeatherGuiService.WeatherOption parseWeather(String value) {
		try {
			return CaptainWeatherGuiService.WeatherOption.valueOf(value);
		} catch (Exception ignored) {
			return CaptainWeatherGuiService.WeatherOption.CLEAR;
		}
	}

	private boolean isPortalActive(CaptainState state, long serverTicks) {
		return state.getNetherPortalLaneId() != null && state.getNetherPortalEndTick() > serverTicks;
	}

	private List<ServerPlayerEntity> findRecallTargets(TeamId teamId, LaneId targetLane) {
		var result = new ArrayList<ServerPlayerEntity>();
		for (var player : matchManager.getOnlinePlayers()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (!state.isUnit() || state.getTeamId() != teamId || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			var runtime = laneRuntimeRegistry.findByPlayer(player);
			if (runtime == null || runtime.laneId() == targetLane) {
				continue;
			}
			result.add(player);
		}
		result.sort(Comparator.comparing(player -> player.getUuid().toString()));
		return result;
	}

	private List<ServerPlayerEntity> teamUnits(TeamId teamId) {
		return matchManager.getOnlinePlayers().stream()
			.filter(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return state.isUnit() && state.getTeamId() == teamId && state.getCurrentUnitId() != null && !player.isSpectator();
			})
			.toList();
	}

	private List<ServerPlayerEntity> enemyUnits(TeamId teamId) {
		return matchManager.getOnlinePlayers().stream()
			.filter(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return state.isUnit() && state.getTeamId() != null && state.getTeamId() != teamId && state.getCurrentUnitId() != null && !player.isSpectator();
			})
			.toList();
	}

	private List<ServerPlayerEntity> laneUnits(TeamId teamId, LaneId laneId) {
		return teamUnits(teamId).stream()
			.filter(player -> {
				var runtime = laneRuntimeRegistry.findByPlayer(player);
				return runtime != null && runtime.laneId() == laneId;
			})
			.toList();
	}

	private void applyWorldWeather(ServerWorld world, CaptainWeatherGuiService.WeatherOption option) {
		if (world == null) {
			return;
		}
		switch (option) {
			case CLEAR -> world.setWeather(20 * 300, 0, false, false);
			case RAIN -> world.setWeather(0, (int) RAIN_DURATION_TICKS, true, false);
			case THUNDER -> world.setWeather(0, (int) THUNDER_DURATION_TICKS, true, true);
		}
	}

	private void spawnLineParticles(ServerWorld world, Vec3d from, Vec3d to, net.minecraft.particle.ParticleEffect particle) {
		if (world == null) {
			return;
		}
		var delta = to.subtract(from);
		var length = delta.length();
		var steps = Math.max(1, (int) Math.ceil(length / 0.6D));
		for (int i = 0; i <= steps; i++) {
			var pos = from.add(delta.multiply(i / (double) steps));
			for (var player : world.getPlayers()) {
				world.spawnParticles(player, particle, true, false, pos.x, pos.y + 1.0D, pos.z, 3, 0.03D, 0.03D, 0.03D, 0.0D);
			}
		}
	}

	private void spawnPortalBurst(ServerWorld world, SystemConfig.PositionConfig position) {
		if (world == null || position == null) {
			return;
		}
		for (var player : world.getPlayers()) {
			world.spawnParticles(player, ParticleTypes.PORTAL, true, false, position.x, position.y + 1.0D, position.z, 180, 3.2D, 2.2D, 3.2D, 0.2D);
		}
	}

	private void playGlobal(ServerWorld world, net.minecraft.sound.SoundEvent sound, SoundCategory category, float volume, float pitch) {
		if (world == null) {
			return;
		}
		for (var player : world.getPlayers()) {
			player.playSoundToPlayer(sound, category, volume, pitch);
		}
	}

	private void spawnLightningStrike(ServerWorld world, ServerPlayerEntity target) {
		if (world == null || target == null) {
			return;
		}
		var lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
		lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
		lightning.setCosmetic(false);
		world.spawnEntity(lightning);
		target.damage(world, world.getDamageSources().lightningBolt(), 4.0F);
	}

	private ServerWorld resolveWorld(ServerPlayerEntity player, String worldId) {
		return resolveWorld(player.getServer(), worldId);
	}

	private ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		try {
			var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}

	private String weatherLabel(CaptainWeatherGuiService.WeatherOption option) {
		return switch (option) {
			case CLEAR -> textConfig().captainMonsterWeatherClearName;
			case RAIN -> textConfig().captainMonsterWeatherRainName;
			case THUNDER -> textConfig().captainMonsterWeatherThunderName;
		};
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	private boolean isCaptainSkillPhase(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING;
	}
}
