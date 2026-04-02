package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchClockTest {
	@Test
	void expiresOnLastSecond() {
		var clock = new MatchClock(2);

		for (int i = 0; i < 39; i++) {
			assertFalse(clock.tick());
		}
		assertTrue(clock.tick());
	}

	@Test
	void adjustDurationTicksKeepsElapsedTimeFixed() {
		var clock = new MatchClock(10);
		for (int i = 0; i < 45; i++) {
			clock.tick();
		}

		assertFalse(clock.adjustDurationTicks(30));
		assertEquals(185, clock.getRemainingTicks());
		assertEquals(230, clock.getTotalTicks());
		assertEquals(2, clock.getElapsedSeconds());
	}

	@Test
	void reduceRemainingTicksCanExpireImmediately() {
		var clock = new MatchClock(1);

		assertTrue(clock.reduceRemainingTicks(20));
		assertEquals(0, clock.getRemainingTicks());
	}
}
