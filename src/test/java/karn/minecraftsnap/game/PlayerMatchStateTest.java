package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerMatchStateTest {
	@Test
	void changingCurrentUnitClearsRuntimeState() {
		var state = new PlayerMatchState();
		state.setLastLaneId(LaneId.LANE_2);
		state.setUnitRuntimeLong("test_key", 7L);
		state.setCurrentUnitId("zombie");

		assertNull(state.getLastLaneId());
		assertNull(state.getUnitRuntimeLong("test_key"));
	}

	@Test
	void clearingCurrentUnitClearsRuntimeState() {
		var state = new PlayerMatchState();
		state.setCurrentUnitId("slime");
		state.setLastLaneId(LaneId.LANE_1);
		state.setUnitRuntimeLong("slime_airborne", 1L);

		state.setCurrentUnitId(null);

		assertNull(state.getLastLaneId());
		assertNull(state.getUnitRuntimeLong("slime_airborne"));
		assertEquals(0, state.getAdvanceExp());
	}
}
