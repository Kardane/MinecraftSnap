package karn.minecraftsnap.lane;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.CaptureOwner;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.CapturePointState;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class LaneRuntimeRegistry {
	private final EnumMap<LaneId, LaneRuntime> runtimes = new EnumMap<>(LaneId.class);
	private String worldId = "minecraft:overworld";

	public LaneRuntimeRegistry() {
		for (var laneId : LaneId.values()) {
			runtimes.put(laneId, new LaneRuntime(laneId));
		}
	}

	public LaneRuntime get(LaneId laneId) {
		return runtimes.get(laneId);
	}

	public List<LaneRuntime> all() {
		return List.copyOf(runtimes.values());
	}

	public void refresh(MinecraftServer server, SystemConfig config, MatchManager matchManager, CapturePointService capturePointService) {
		worldId = config.world;
		for (var laneId : LaneId.values()) {
			var runtime = runtimes.get(laneId);
			var laneRegion = resolveLaneRegion(laneId, config);
			var captureRegion = captureRegionOf(laneId, config);
			runtime.updateRegions(laneRegion, captureRegion);
			runtime.updateOccupants(
				collectUnitOccupants(server, matchManager, laneRegion),
				collectCaptureOccupants(server, matchManager, captureRegion)
			);
			runtime.updateCapture(ownerOf(capturePointService, laneId), statusOf(capturePointService, laneId), redScoreOf(capturePointService, laneId, config), blueScoreOf(capturePointService, laneId, config));
		}
	}

	public LaneRuntime findByPlayer(ServerPlayerEntity player) {
		if (player == null) {
			return null;
		}
		var worldId = player.getWorld().getRegistryKey().getValue().toString();
		if (!this.worldId.equals(worldId)) {
			return null;
		}
		for (var runtime : runtimes.values()) {
			if (runtime.laneRegion() == null) {
				continue;
			}
			if (contains(runtime.laneRegion(), player.getX(), player.getY(), player.getZ())) {
				return runtime;
			}
		}
		return null;
	}

	public void reset() {
		for (var runtime : runtimes.values()) {
			runtime.updateOccupants(List.of(), List.of());
			runtime.updateCapture(CaptureOwner.NEUTRAL, LaneCaptureStatus.IDLE, 0, 0);
			runtime.clearBiome();
		}
	}

	private List<java.util.UUID> collectUnitOccupants(MinecraftServer server, MatchManager matchManager, SystemConfig.LaneRegionConfig laneRegion) {
		if (server == null || laneRegion == null) {
			return List.of();
		}
		var occupants = new ArrayList<java.util.UUID>();
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			if (worldId.equals(player.getWorld().getRegistryKey().getValue().toString()) && contains(laneRegion, player.getX(), player.getY(), player.getZ())) {
				occupants.add(player.getUuid());
			}
		}
		return occupants;
	}

	private List<java.util.UUID> collectCaptureOccupants(MinecraftServer server, MatchManager matchManager, SystemConfig.CaptureRegionConfig captureRegion) {
		if (server == null || captureRegion == null || !captureRegion.enabled) {
			return List.of();
		}
		var occupants = new ArrayList<java.util.UUID>();
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			if (worldId.equals(player.getWorld().getRegistryKey().getValue().toString())
				&& contains(captureRegion, player.getPos().x, player.getPos().y, player.getPos().z)) {
				occupants.add(player.getUuid());
			}
		}
		return occupants;
	}

	private CaptureOwner ownerOf(CapturePointService capturePointService, LaneId laneId) {
		return captureStateOf(capturePointService, laneId).getOwner();
	}

	private LaneCaptureStatus statusOf(CapturePointService capturePointService, LaneId laneId) {
		var state = captureStateOf(capturePointService, laneId);
		if (state.getProgress().isContested()) {
			return LaneCaptureStatus.CONTESTED;
		}
		if (state.getProgress().getTeamId() == TeamId.RED) {
			return LaneCaptureStatus.RED_PROGRESS;
		}
		if (state.getProgress().getTeamId() == TeamId.BLUE) {
			return LaneCaptureStatus.BLUE_PROGRESS;
		}
		return switch (state.getOwner()) {
			case RED -> LaneCaptureStatus.RED_OWNED;
			case BLUE -> LaneCaptureStatus.BLUE_OWNED;
			case NEUTRAL -> LaneCaptureStatus.IDLE;
		};
	}

	private int redScoreOf(CapturePointService capturePointService, LaneId laneId, SystemConfig config) {
		var state = captureStateOf(capturePointService, laneId);
		if (state.getProgress().getTeamId() == TeamId.RED) {
			return state.getProgress().getSeconds();
		}
		return state.getOwner() == CaptureOwner.RED ? config.capture.captureStepSeconds : 0;
	}

	private int blueScoreOf(CapturePointService capturePointService, LaneId laneId, SystemConfig config) {
		var state = captureStateOf(capturePointService, laneId);
		if (state.getProgress().getTeamId() == TeamId.BLUE) {
			return state.getProgress().getSeconds();
		}
		return state.getOwner() == CaptureOwner.BLUE ? config.capture.captureStepSeconds : 0;
	}

	private CapturePointState captureStateOf(CapturePointService capturePointService, LaneId laneId) {
		return capturePointService == null ? new CapturePointState(laneId) : capturePointService.getState(laneId);
	}

	private boolean contains(SystemConfig.LaneRegionConfig region, double x, double y, double z) {
		return region != null
			&& x >= region.minX && x <= region.maxX
			&& y >= region.minY && y <= region.maxY
			&& z >= region.minZ && z <= region.maxZ;
	}

	private boolean contains(SystemConfig.CaptureRegionConfig region, double x, double y, double z) {
		return region != null
			&& x >= region.minX && x <= region.maxX
			&& y >= region.minY && y <= region.maxY
			&& z >= region.minZ && z <= region.maxZ;
	}

	private SystemConfig.CaptureRegionConfig captureRegionOf(LaneId laneId, SystemConfig config) {
		return switch (laneId) {
			case LANE_1 -> config.capture.lane1;
			case LANE_2 -> config.capture.lane2;
			case LANE_3 -> config.capture.lane3;
		};
	}

	private SystemConfig.LaneRegionConfig resolveLaneRegion(LaneId laneId, SystemConfig config) {
		return switch (laneId) {
			case LANE_1 -> config.inGame.lane1Region;
			case LANE_2 -> config.inGame.lane2Region;
			case LANE_3 -> config.inGame.lane3Region;
		};
	}
}
