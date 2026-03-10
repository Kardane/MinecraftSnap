package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiomeRevealServiceTest {
	@Test
	void scheduledRevealOpensLanesAtConfiguredSeconds() {
		var manager = new MatchManager();
		var service = new BiomeRevealService(manager, new TextTemplateResolver());
		var config = new SystemConfig();

		service.syncRevealState(0, config.biomeReveal);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_1));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_2));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_3));

		service.syncRevealState(180, config.biomeReveal);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_2));

		service.syncRevealState(360, config.biomeReveal);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_3));
	}

	@Test
	void hiddenLaneCanBeForcedBackToUnrevealed() {
		var manager = new MatchManager();
		var service = new BiomeRevealService(manager, new TextTemplateResolver());
		var config = new SystemConfig();

		service.syncRevealState(360, config.biomeReveal);
		manager.setLaneRevealOverride(LaneId.LANE_2, false);
		service.syncRevealState(400, config.biomeReveal);

		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_2));
		assertEquals(false, manager.isLaneActive(LaneId.LANE_2));
	}
}
