package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;

public class FactionStructureResetService {
	private static final String DEFAULT_STRUCTURE_ID = "minecraft:default";
	private static final long RESET_INTERVAL_TICKS = 60L;

	private final LaneStructureService laneStructureService;
	private int nextLaneIndex;
	private long nextResetTick;
	private boolean active;
	private boolean complete;

	public FactionStructureResetService(LaneStructureService laneStructureService) {
		this.laneStructureService = laneStructureService;
	}

	public void start(long currentTick) {
		nextLaneIndex = 0;
		nextResetTick = currentTick;
		active = true;
		complete = false;
	}

	public void tick(net.minecraft.server.MinecraftServer server, SystemConfig systemConfig, long currentTick) {
		if (!active || laneStructureService == null || systemConfig == null || currentTick < nextResetTick) {
			return;
		}
		var lanes = LaneId.values();
		if (nextLaneIndex >= lanes.length) {
			active = false;
			complete = true;
			return;
		}

		var laneId = lanes[nextLaneIndex];
		laneStructureService.forcePlaceStructure(
			server,
			systemConfig.world,
			laneId,
			DEFAULT_STRUCTURE_ID,
			laneStructureService.originFor(laneRegionOf(laneId, systemConfig))
		);
		nextLaneIndex++;
		nextResetTick = currentTick + RESET_INTERVAL_TICKS;
		if (nextLaneIndex >= lanes.length) {
			active = false;
			complete = true;
		}
	}

	public void cancel() {
		active = false;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isComplete() {
		return complete;
	}

	private SystemConfig.LaneRegionConfig laneRegionOf(LaneId laneId, SystemConfig systemConfig) {
		return switch (laneId) {
			case LANE_1 -> systemConfig.inGame.lane1Region;
			case LANE_2 -> systemConfig.inGame.lane2Region;
			case LANE_3 -> systemConfig.inGame.lane3Region;
		};
	}
}
