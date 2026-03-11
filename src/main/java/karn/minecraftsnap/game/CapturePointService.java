package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CapturePointService {
	private final MatchManager matchManager;
	private final StatsRepository statsRepository;
	private final Map<LaneId, CapturePointState> states = new EnumMap<>(LaneId.class);

	public CapturePointService(MatchManager matchManager, StatsRepository statsRepository) {
		this.matchManager = matchManager;
		this.statsRepository = statsRepository;
		for (var laneId : LaneId.values()) {
			states.put(laneId, new CapturePointState(laneId));
		}
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return;
		}

		var captureConfig = systemConfig.capture;
		processLane(server, systemConfig.world, systemConfig.inGame.lane1Region, captureConfig.lane1, LaneId.LANE_1, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, systemConfig.world, systemConfig.inGame.lane2Region, captureConfig.lane2, LaneId.LANE_2, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, systemConfig.world, systemConfig.inGame.lane3Region, captureConfig.lane3, LaneId.LANE_3, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);

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
		String worldId,
		SystemConfig.LaneRegionConfig laneRegion,
		SystemConfig.CaptureRegionConfig pointConfig,
		LaneId laneId,
		int captureStepSeconds,
		int scoreIntervalTicks
	) {
		var state = states.get(laneId);
		if (!matchManager.isLaneActive(laneId) || pointConfig == null || !pointConfig.enabled || !contains(laneRegion, pointConfig)) {
			state.getProgress().reset();
			return;
		}

		var occupants = findOccupants(server, worldId, pointConfig);
		int redCount = 0;
		int blueCount = 0;
		for (var player : occupants) {
			var playerState = matchManager.getPlayerState(player.getUuid());
			if (!playerState.isUnit() || player.isSpectator() || playerState.getCurrentUnitId() == null) {
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
		boolean captured = state.update(occupyingTeam, contested, captureStepSeconds);
		if (matchManager.getServerTicks() % 10L == 0L) {
			spawnDustBorder(server, worldId, pointConfig, state.getOwner());
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
			statsRepository.addLadder(player.getUuid(), player.getName().getString(), 1);
		}

		if (ownerPresent) {
			matchManager.addScore(ownerTeam, 1);
			rewardVillagerCurrency(occupants, ownerTeam);
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

	private void rewardVillagerCurrency(List<ServerPlayerEntity> occupants, TeamId ownerTeam) {
		for (var player : occupants) {
			var playerState = matchManager.getPlayerState(player.getUuid());
			if (!playerState.isUnit()
				|| player.isSpectator()
				|| playerState.getCurrentUnitId() == null
				|| playerState.getTeamId() != ownerTeam
				|| playerState.getFactionId() != FactionId.VILLAGER) {
				continue;
			}
			playerState.addEmeralds(1);
			statsRepository.addEmeralds(player.getUuid(), player.getName().getString(), 1);
		}
	}

	static boolean contains(SystemConfig.CaptureRegionConfig region, Vec3d pos) {
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

	private void spawnDustBorder(MinecraftServer server, String worldId, SystemConfig.CaptureRegionConfig region, CaptureOwner owner) {
		var world = resolveWorld(server, worldId);
		if (world == null) {
			return;
		}
		var effect = new DustParticleEffect(color(owner), 1.0f);
		double y = region.minY + 0.1;
		for (double x = region.minX; x <= region.maxX; x += 1.0) {
			spawnDust(world, effect, x, y, region.minZ);
			spawnDust(world, effect, x, y, region.maxZ);
		}
		for (double z = region.minZ + 1.0; z < region.maxZ; z += 1.0) {
			spawnDust(world, effect, region.minX, y, z);
			spawnDust(world, effect, region.maxX, y, z);
		}
	}

	private void spawnDust(ServerWorld world, DustParticleEffect effect, double x, double y, double z) {
		world.spawnParticles(effect, x + 0.5, y, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
	}

	private int color(CaptureOwner owner) {
		return switch (owner) {
			case RED -> 0xFF3333;
			case BLUE -> 0x3366FF;
			case NEUTRAL -> 0xFFFFFF;
		};
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
}
