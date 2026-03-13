package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePointStateTest {
	@Test
	void neutralToRedAfterFiveSeconds() {
		var state = new CapturePointState(LaneId.LANE_1);

		for (int i = 0; i < 99; i++) {
			assertFalse(state.update(TeamId.RED, false, 5));
		}
		assertTrue(state.update(TeamId.RED, false, 5));
		assertEquals(CaptureOwner.RED, state.getOwner());
	}

	@Test
	void contestedResetsProgress() {
		var state = new CapturePointState(LaneId.LANE_1);
		state.update(TeamId.RED, false, 5);
		state.update(TeamId.RED, false, 5);

		state.update(null, true, 5);

		assertTrue(state.getProgress().isContested());
		assertNull(state.getProgress().getTeamId());
		assertEquals(0, state.getProgress().getSeconds());
	}

	@Test
	void redToBlueRequiresTwoSteps() {
		var state = new CapturePointState(LaneId.LANE_1);
		for (int i = 0; i < 100; i++) {
			state.update(TeamId.RED, false, 5);
		}
		assertEquals(CaptureOwner.RED, state.getOwner());

		for (int i = 0; i < 100; i++) {
			state.update(TeamId.BLUE, false, 5);
		}
		assertEquals(CaptureOwner.NEUTRAL, state.getOwner());

		for (int i = 0; i < 100; i++) {
			state.update(TeamId.BLUE, false, 5);
		}
		assertEquals(CaptureOwner.BLUE, state.getOwner());
	}
}
