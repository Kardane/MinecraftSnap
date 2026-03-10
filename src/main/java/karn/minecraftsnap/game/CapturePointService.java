package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
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
		processLane(server, captureConfig.lane1, LaneId.LANE_1, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, captureConfig.lane2, LaneId.LANE_2, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);
		processLane(server, captureConfig.lane3, LaneId.LANE_3, captureConfig.captureStepSeconds, captureConfig.scoreIntervalTicks);

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
		SystemConfig.CapturePointConfig pointConfig,
		LaneId laneId,
		int captureStepSeconds,
		int scoreIntervalTicks
	) {
		var state = states.get(laneId);
		if (!matchManager.isLaneActive(laneId) || pointConfig == null || !pointConfig.enabled) {
			state.getProgress().reset();
			return;
		}

		var occupants = findOccupants(server, pointConfig);
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

	private List<ServerPlayerEntity> findOccupants(MinecraftServer server, SystemConfig.CapturePointConfig pointConfig) {
		return server.getPlayerManager().getPlayerList().stream()
			.filter(player -> player.getWorld().getRegistryKey().getValue().toString().equals(pointConfig.world))
			.filter(player -> player.getPos().distanceTo(new Vec3d(pointConfig.x, pointConfig.y, pointConfig.z)) <= pointConfig.radius)
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
}
