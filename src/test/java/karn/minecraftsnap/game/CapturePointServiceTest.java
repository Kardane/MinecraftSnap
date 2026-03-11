package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePointServiceTest {
	@Test
	void containsReturnsTrueOnlyInsideCuboidCaptureRegion() {
		var region = SystemConfig.CaptureRegionConfig.create("test", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);

		assertTrue(CapturePointService.contains(region, new Vec3d(0.0, 64.0, 0.0)));
		assertFalse(CapturePointService.contains(region, new Vec3d(5.0, 64.0, 0.0)));
	}

	@Test
	void captureRegionMustStayInsideLaneRegion() {
		var lane = SystemConfig.LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		var inside = SystemConfig.CaptureRegionConfig.create("inside", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);
		var outside = SystemConfig.CaptureRegionConfig.create("outside", -20.0, 60.0, -4.0, 4.0, 68.0, 4.0);

		assertTrue(CapturePointService.contains(lane, inside));
		assertFalse(CapturePointService.contains(lane, outside));
	}
}
