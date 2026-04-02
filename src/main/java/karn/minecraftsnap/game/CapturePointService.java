package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CapturePointService {
	private final MatchManager matchManager;
	private final StatsRepository statsRepository;
	private final Map<LaneId, CapturePointState> states = new EnumMap<>(LaneId.class);
	private final UiSoundService uiSoundService;
	private final TextTemplateResolver textTemplateResolver;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final UnitHookService unitHookService;

	public CapturePointService(
		MatchManager matchManager,
		StatsRepository statsRepository,
		UiSoundService uiSoundService,
		TextTemplateResolver textTemplateResolver,
		LaneRuntimeRegistry laneRuntimeRegistry,
		UnitHookService unitHookService
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.uiSoundService = uiSoundService;
		this.textTemplateResolver = textTemplateResolver;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.unitHookService = unitHookService;
		for (var laneId : LaneId.values()) {
			states.put(laneId, new CapturePointState(laneId));
		}
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return;
		}

		var captureConfig = systemConfig.capture;
		processLane(server, systemConfig, systemConfig.world, systemConfig.inGame.lane1Region, captureConfig.lane1, LaneId.LANE_1, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, systemConfig, systemConfig.world, systemConfig.inGame.lane2Region, captureConfig.lane2, LaneId.LANE_2, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, systemConfig, systemConfig.world, systemConfig.inGame.lane3Region, captureConfig.lane3, LaneId.LANE_3, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);

		if (matchManager.getServerTicks() % 20L == 0L) {
			var heldBy = allPointsHeldBySingleTeam();
			matchManager.recordAllPointsHeld(heldBy, captureConfig.allPointsHoldSeconds);
		}
	}

	public CapturePointState getState(LaneId laneId) {
		return states.get(laneId);
	}

	public void resetAll() {
		states.values().forEach(CapturePointState::reset);
	}

	private void processLane(
		MinecraftServer server,
		SystemConfig systemConfig,
		String worldId,
		SystemConfig.LaneRegionConfig laneRegion,
		SystemConfig.CaptureRegionConfig pointConfig,
		LaneId laneId,
		int captureStepSeconds,
		int scoreIntervalTicks
	) {
		var state = states.get(laneId);
		if (shouldRenderParticles(matchManager.isLaneActive(laneId), laneRegion, pointConfig) && matchManager.getServerTicks() % 2L == 0L) {
			spawnDustBeam(server, worldId, pointConfig, state.getOwner(), matchManager.getServerTicks());
		}
		if (!matchManager.isLaneActive(laneId) || pointConfig == null || !pointConfig.enabled || !contains(laneRegion, pointConfig)) {
			state.getProgress().reset();
			return;
		}

		var occupants = findOccupants(server, worldId, pointConfig);
		int redCount = 0;
		int blueCount = 0;
		for (var player : occupants) {
			var playerState = matchManager.getPlayerState(player.getUuid());
			if (!countsForCapture(playerState, player.isSpectator())) {
				continue;
			}
			if (playerState.getTeamId() == TeamId.RED) {
				redCount++;
			} else if (playerState.getTeamId() == TeamId.BLUE) {
				blueCount++;
			}
		}

		boolean contested = redCount > 0 && blueCount > 0;
		TeamId occupyingTeam = contested ? null : redCount > 0 ? TeamId.RED : blueCount > 0 ? TeamId.BLUE : null;
		var wasContested = state.getProgress().isContested();
		var previousOwner = state.getOwner();
		if (matchManager.getServerTicks() % 20L == 0L && isCaptureInProgress(state, occupyingTeam, contested)) {
			playCaptureSound(occupants);
		}
		boolean captured = state.update(occupyingTeam, contested, captureStepSeconds);
		if (contested && !wasContested) {
			playContestedSound(occupants);
		}
		if (captured && previousOwner != state.getOwner()) {
			playCapturedSound(occupants);
			broadcastCaptureOwnerChanged(server, laneId, state.getOwner());
			spawnCaptureFireworks(server, worldId, pointConfig, state.getOwner());
		}
		if (captured && occupyingTeam != null) {
			for (var player : occupants) {
				var playerState = matchManager.getPlayerState(player.getUuid());
				if (playerState.isUnit() && playerState.getCurrentUnitId() != null && playerState.getTeamId() == occupyingTeam && !player.isSpectator()) {
					statsRepository.addCapture(player.getUuid(), player.getName().getString(), 1);
				}
			}
		}

		if (matchManager.getServerTicks() % scoreIntervalTicks != 0L) {
			return;
		}

		var ownerTeam = ownerTeam(state.getOwner());
		if (ownerTeam == null) {
			return;
		}

		boolean ownerPresent = false;
		for (var player : occupants) {
			var playerState = matchManager.getPlayerState(player.getUuid());
			if (!playerState.isUnit() || player.isSpectator() || playerState.getCurrentUnitId() == null || playerState.getTeamId() != ownerTeam) {
				continue;
			}
			ownerPresent = true;
		}

		if (ownerPresent) {
			var context = createBiomeContext(server, systemConfig, laneId);
			var scoreAmount = captureScoreAmount(context, ownerTeam);
			if (scoreAmount <= 0) {
				return;
			}
			matchManager.addScore(ownerTeam, scoreAmount);
			notifyCaptureScoreBiomeEffect(context, ownerTeam);
			rewardCaptureScore(occupants, ownerTeam, systemConfig);
		}
	}

	int captureScoreAmount(BiomeRuntimeContext context, TeamId ownerTeam) {
		if (context == null) {
			return 1;
		}
		return context.laneRuntime().biomeEffect().captureScoreAmount(context, ownerTeam);
	}

	private void notifyCaptureScoreBiomeEffect(BiomeRuntimeContext context, TeamId ownerTeam) {
		if (context == null) {
			return;
		}
		context.laneRuntime().biomeEffect().onCaptureScore(context, ownerTeam);
	}

	private List<ServerPlayerEntity> findOccupants(MinecraftServer server, String worldId, SystemConfig.CaptureRegionConfig pointConfig) {
		return server.getPlayerManager().getPlayerList().stream()
			.filter(player -> player.getWorld().getRegistryKey().getValue().toString().equals(worldId))
			.filter(player -> contains(pointConfig, player.getPos()))
			.toList();
	}

	private TeamId allPointsHeldBySingleTeam() {
		TeamId team = null;
		for (var state : states.values()) {
			var ownerTeam = ownerTeam(state.getOwner());
			if (ownerTeam == null) {
				return null;
			}
			if (team == null) {
				team = ownerTeam;
			} else if (team != ownerTeam) {
				return null;
			}
		}
		return team;
	}

	private TeamId ownerTeam(CaptureOwner owner) {
		return switch (owner) {
			case RED -> TeamId.RED;
			case BLUE -> TeamId.BLUE;
			case NEUTRAL -> null;
		};
	}

	private void rewardCaptureScore(List<ServerPlayerEntity> occupants, TeamId ownerTeam, SystemConfig systemConfig) {
		for (var player : occupants) {
			var playerState = matchManager.getPlayerState(player.getUuid());
			if (!playerState.isUnit()
				|| player.isSpectator()
				|| playerState.getCurrentUnitId() == null
				|| playerState.getTeamId() != ownerTeam) {
				continue;
			}
			if (unitHookService != null) {
				unitHookService.handleCaptureScore(player, systemConfig);
			} else if (playerState.getFactionId() == FactionId.VILLAGER) {
				playerState.addEmeralds(1);
				statsRepository.addEmeralds(player.getUuid(), player.getName().getString(), 1);
			}
			playerState.addMatchCaptureScore(1);
			player.getWorld().playSound(
				null,
				player.getBlockPos(),
				SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
				SoundCategory.PLAYERS,
				0.7f,
				1.2f
			);
		}
	}

	public static boolean contains(SystemConfig.CaptureRegionConfig region, Vec3d pos) {
		return region != null
			&& pos.x >= region.minX && pos.x <= region.maxX
			&& pos.y >= region.minY && pos.y <= region.maxY
			&& pos.z >= region.minZ && pos.z <= region.maxZ;
	}

	static boolean contains(SystemConfig.LaneRegionConfig laneRegion, SystemConfig.CaptureRegionConfig captureRegion) {
		return laneRegion != null
			&& captureRegion != null
			&& captureRegion.minX >= laneRegion.minX
			&& captureRegion.maxX <= laneRegion.maxX
			&& captureRegion.minY >= laneRegion.minY
			&& captureRegion.maxY <= laneRegion.maxY
			&& captureRegion.minZ >= laneRegion.minZ
			&& captureRegion.maxZ <= laneRegion.maxZ;
	}

	static boolean countsForCapture(PlayerMatchState playerState, boolean spectator) {
		if (playerState == null || spectator || playerState.getTeamId() == null) {
			return false;
		}
		return playerState.isUnit() && playerState.getCurrentUnitId() != null;
	}

	static boolean shouldRenderParticles(boolean laneActive, SystemConfig.LaneRegionConfig laneRegion, SystemConfig.CaptureRegionConfig captureRegion) {
		return captureRegion != null && captureRegion.enabled && contains(laneRegion, captureRegion);
	}

	private void spawnDustBeam(MinecraftServer server, String worldId, SystemConfig.CaptureRegionConfig region, CaptureOwner owner, long serverTicks) {
		var world = resolveWorld(server, worldId);
		if (world == null) {
			return;
		}
		var effect = new DustParticleEffect(color(owner), particleSize());
		var startY = region.maxY;
		var sampleCount = Math.max(1, (int) Math.ceil((particleBeamTopY() - startY) / particleSpacing()) + 1);
		var step = particleBeamStep(serverTicks, sampleCount);
		var y = Math.min(particleBeamTopY(), startY + step * particleSpacing());
		spawnDust(world, effect, particleCenterX(region), y, particleCenterZ(region));
	}

	private void spawnDust(ServerWorld world, DustParticleEffect effect, double x, double y, double z) {
		for (var player : world.getPlayers()) {
			world.spawnParticles(player, effect, true, false, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	static int color(CaptureOwner owner) {
		return switch (owner) {
			case RED -> 0xFF3333;
			case BLUE -> 0x3366FF;
			case NEUTRAL -> 0xFFFFFF;
		};
	}

	static double particleCenterX(SystemConfig.CaptureRegionConfig region) {
		return region == null ? 0.0D : (region.minX + region.maxX) / 2.0D;
	}

	static double particleCenterZ(SystemConfig.CaptureRegionConfig region) {
		return region == null ? 0.0D : (region.minZ + region.maxZ) / 2.0D;
	}

	static List<Vec3d> fireworkLaunchPositions(SystemConfig.CaptureRegionConfig region) {
		if (region == null) {
			return List.of();
		}
		double y = region.maxY + 0.5D;
		return List.of(
			new Vec3d(region.minX, y, region.minZ),
			new Vec3d(region.minX, y, region.maxZ),
			new Vec3d(region.maxX, y, region.minZ),
			new Vec3d(region.maxX, y, region.maxZ)
		);
	}

	static double particleBeamTopY() {
		return 30.0D;
	}

	static double particleSpacing() {
		return 0.5D;
	}

	static float particleSize() {
		return 3.0F;
	}

	static int particleBeamStep(long serverTicks, int sampleCount) {
		if (sampleCount <= 0) {
			return 0;
		}
		return (int) ((serverTicks / 2L) % sampleCount);
	}

	private boolean isCaptureInProgress(CapturePointState state, TeamId occupyingTeam, boolean contested) {
		if (state == null) {
			return false;
		}
		if (contested) {
			return true;
		}
		return occupyingTeam != null && state.getOwner() != CaptureOwner.fromTeam(occupyingTeam);
	}

	private void playCaptureSound(List<ServerPlayerEntity> occupants) {
		for (var player : occupants) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (!state.isUnit() || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			player.getWorld().playSound(
				null,
				player.getBlockPos(),
				SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(),
				SoundCategory.PLAYERS,
				0.6f,
				1.6f
			);
		}
	}

	private void playContestedSound(List<ServerPlayerEntity> occupants) {
		if (uiSoundService == null) {
			return;
		}
		for (var player : occupants) {
			uiSoundService.playEventWarning(player);
		}
	}

	private void playCapturedSound(List<ServerPlayerEntity> occupants) {
		if (uiSoundService == null) {
			return;
		}
		for (var player : occupants) {
			uiSoundService.playEventSuccess(player);
		}
	}

	private ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		try {
			var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	private void broadcastCaptureOwnerChanged(MinecraftServer server, LaneId laneId, CaptureOwner owner) {
		if (server == null || textTemplateResolver == null) {
			return;
		}
		server.getPlayerManager().broadcast(textTemplateResolver.format(textConfig().captureOwnerChangedBroadcastTemplate
			.replace("{lane}", laneLabel(laneId))
			.replace("{owner}", ownerLabel(owner))), false);
	}

	private void spawnCaptureFireworks(MinecraftServer server, String worldId, SystemConfig.CaptureRegionConfig region, CaptureOwner owner) {
		var world = resolveWorld(server, worldId);
		if (world == null || region == null) {
			return;
		}
		var rocket = captureFireworkRocket(owner);
		for (var position : fireworkLaunchPositions(region)) {
			world.spawnEntity(new FireworkRocketEntity(world, position.x, position.y, position.z, rocket.copy()));
		}
	}

	private ItemStack captureFireworkRocket(CaptureOwner owner) {
		var rocket = new ItemStack(Items.FIREWORK_ROCKET);
		rocket.set(DataComponentTypes.FIREWORKS, new FireworksComponent(
			1,
			List.of(new FireworkExplosionComponent(
				FireworkExplosionComponent.Type.SMALL_BALL,
				IntList.of(color(owner)),
				IntList.of(),
				false,
				false
			))
		));
		return rocket;
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> textConfig().captureLane1Name;
			case LANE_2 -> textConfig().captureLane2Name;
			case LANE_3 -> textConfig().captureLane3Name;
		};
	}

	private String ownerLabel(CaptureOwner owner) {
		return switch (owner) {
			case RED -> textConfig().captureOwnerRedName;
			case BLUE -> textConfig().captureOwnerBlueName;
			case NEUTRAL -> textConfig().captureOwnerNeutralName;
		};
	}

	private BiomeRuntimeContext createBiomeContext(MinecraftServer server, SystemConfig systemConfig, LaneId laneId) {
		if (server == null || systemConfig == null || laneRuntimeRegistry == null || textTemplateResolver == null) {
			return null;
		}
		var runtime = laneRuntimeRegistry.get(laneId);
		if (runtime == null || !runtime.hasActiveBiome()) {
			return null;
		}
		return new BiomeRuntimeContext(
			server,
			resolveWorld(server, systemConfig.world),
			matchManager,
			runtime,
			runtime.biomeEntry(),
			textTemplateResolver,
			matchManager.getServerTicks(),
			matchManager.getElapsedSeconds()
		);
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
