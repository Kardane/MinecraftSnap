package karn.minecraftsnap.lane;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.config.StatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LaneRuntimeRegistryTest {
	@TempDir
	Path tempDir;

	@Test
	void refreshSyncsLaneRegionAndCaptureState() {
		var config = new SystemConfig();
		config.capture.lane1.enabled = true;
		config.capture.lane1.minX = 16.0;
		config.capture.lane1.maxX = 20.0;
		config.capture.lane1.minY = 10.0;
		config.capture.lane1.maxY = 20.0;
		config.capture.lane1.minZ = -2.0;
		config.capture.lane1.maxZ = 2.0;

		var capturePointService = new CapturePointService(
			new MatchManager(),
			new StatsRepository(tempDir.resolve("stats.json"), LoggerFactory.getLogger("test"))
		);
		capturePointService.getState(LaneId.LANE_1).getProgress().start(TeamId.RED);

		var registry = new LaneRuntimeRegistry();
		registry.refresh(null, config, new MatchManager(), capturePointService);

		var runtime = registry.get(LaneId.LANE_1);
		assertEquals(config.inGame.lane2Region, runtime.laneRegion());
		assertEquals(config.capture.lane1, runtime.captureRegion());
		assertEquals(LaneCaptureStatus.RED_PROGRESS, runtime.captureStatus());
		assertEquals(1, runtime.redCaptureScore());
		assertEquals(0, runtime.blueCaptureScore());
	}
}
