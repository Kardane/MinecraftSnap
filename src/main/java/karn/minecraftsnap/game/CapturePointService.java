package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
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
	private LaneRuntimeRegistry laneRuntimeRegistry;
	private UnitHookService unitHookService;

	public CapturePointService(MatchManager matchManager, StatsRepository statsRepository) {
		this(matchManager, statsRepository, null, null);
	}

	public CapturePointService(MatchManager matchManager, StatsRepository statsRepository, UiSoundService uiSoundService) {
		this(matchManager, statsRepository, uiSoundService, null);
	}

	public CapturePointService(
		MatchManager matchManager,
		StatsRepository statsRepository,
		UiSoundService uiSoundService,
		TextTemplateResolver textTemplateResolver
	) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		this.uiSoundService = uiSoundService;
		this.textTemplateResolver = textTemplateResolver;
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

	public void setUnitHookService(UnitHookService unitHookService) {
		this.unitHookService = unitHookService;
	}

	public void setLaneRuntimeRegistry(LaneRuntimeRegistry laneRuntimeRegistry) {
		this.laneRuntimeRegistry = laneRuntimeRegistry;
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
		if (shouldRenderParticles(matchManager.isLaneActive(laneId), laneRegion, pointConfig) && matchManager.getServerTicks() % 10L == 0L) {
			spawnDustBorder(server, worldId, pointConfig, state.getOwner());
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
			matchManager.addScore(ownerTeam, 1);
			notifyCaptureScoreBiomeEffect(server, systemConfig, laneId, ownerTeam);
			rewardCaptureScore(occupants, ownerTeam, systemConfig);
		}
	}

	private void notifyCaptureScoreBiomeEffect(MinecraftServer server, SystemConfig systemConfig, LaneId laneId, TeamId ownerTeam) {
		var context = createBiomeContext(server, systemConfig, laneId);
		if (context != null) {
			context.laneRuntime().biomeEffect().onCaptureScore(context, ownerTeam);
		}
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
		return playerState.isCaptain()
			|| (playerState.isUnit() && playerState.getCurrentUnitId() != null);
	}

	static boolean shouldRenderParticles(boolean laneActive, SystemConfig.LaneRegionConfig laneRegion, SystemConfig.CaptureRegionConfig captureRegion) {
		return captureRegion != null && captureRegion.enabled && contains(laneRegion, captureRegion);
	}

	private void spawnDustBorder(MinecraftServer server, String worldId, SystemConfig.CaptureRegionConfig region, CaptureOwner owner) {
		var world = resolveWorld(server, worldId);
		if (world == null) {
			return;
		}
		var effect = new DustParticleEffect(color(owner), 1.0f);
		var minX = region.minX;
		var maxX = region.maxX;
		var minY = region.minY;
		var maxY = region.maxY;
		var minZ = region.minZ;
		var maxZ = region.maxZ;
		sampleEdge(world, effect, minX, minY, minZ, maxX, minY, minZ);
		sampleEdge(world, effect, minX, minY, maxZ, maxX, minY, maxZ);
		sampleEdge(world, effect, minX, maxY, minZ, maxX, maxY, minZ);
		sampleEdge(world, effect, minX, maxY, maxZ, maxX, maxY, maxZ);
		sampleEdge(world, effect, minX, minY, minZ, minX, maxY, minZ);
		sampleEdge(world, effect, maxX, minY, minZ, maxX, maxY, minZ);
		sampleEdge(world, effect, minX, minY, maxZ, minX, maxY, maxZ);
		sampleEdge(world, effect, maxX, minY, maxZ, maxX, maxY, maxZ);
		sampleEdge(world, effect, minX, minY, minZ, minX, minY, maxZ);
		sampleEdge(world, effect, maxX, minY, minZ, maxX, minY, maxZ);
		sampleEdge(world, effect, minX, maxY, minZ, minX, maxY, maxZ);
		sampleEdge(world, effect, maxX, maxY, minZ, maxX, maxY, maxZ);
	}

	private void spawnDust(ServerWorld world, DustParticleEffect effect, double x, double y, double z) {
		for (var player : world.getPlayers()) {
			world.spawnParticles(player, effect, true, false, x + 0.5, y + 0.5, z + 0.5, 8, 0.04, 0.04, 0.04, 0.0);
		}
	}

	private void sampleEdge(ServerWorld world, DustParticleEffect effect, double startX, double startY, double startZ, double endX, double endY, double endZ) {
		var dx = endX - startX;
		var dy = endY - startY;
		var dz = endZ - startZ;
		var length = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
		var steps = Math.max(1, (int) Math.ceil(length / particleSpacing()));
		for (int i = 0; i <= steps; i++) {
			var progress = i / (double) steps;
			spawnDust(
				world,
				effect,
				startX + dx * progress,
				startY + dy * progress,
				startZ + dz * progress
			);
		}
	}

	static int color(CaptureOwner owner) {
		return switch (owner) {
			case RED -> 0xFF3333;
			case BLUE -> 0x3366FF;
			case NEUTRAL -> 0xFFFFFF;
		};
	}

	static double particleBorderY(SystemConfig.CaptureRegionConfig region) {
		return region == null ? 0.0D : region.maxY;
	}

	static double particleSpacing() {
		return 0.5D;
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
			matchManager.getTotalSeconds() - matchManager.getRemainingSeconds()
		);
	}
}
