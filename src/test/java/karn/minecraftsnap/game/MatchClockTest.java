package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchClockTest {
	@Test
	void expiresOnLastSecond() {
		var clock = new MatchClock(2);

		assertFalse(clock.tickSecond());
		assertTrue(clock.tickSecond());
	}
}
