package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptainManaServiceTest {
	@Test
	void restoresOneManaEveryFifteenSeconds() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(1);

		for (int second = 1; second <= 15; second++) {
			service.tickSecond(3, second);
		}

		assertEquals(2, state.getCurrentMana());
		assertEquals(15, state.getSecondsUntilNextMana());
	}

	@Test
	void increasesMaxManaEveryMinute() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);

		service.tickSecond(3, 60);
		service.tickSecond(3, 120);

		assertEquals(5, state.getMaxMana());
	}

	@Test
	void refillOnBiomeRevealSetsCurrentManaToMax() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(0);
		state.setMaxMana(6);

		service.refillMana(captainId);

		assertEquals(6, state.getCurrentMana());
		assertEquals(15, state.getSecondsUntilNextMana());
	}

	@Test
	void spendingManaStartsSpawnCooldown() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);

		var spent = service.trySpendForSpawn(captainId, 2, 5);

		assertEquals(true, spent);
		assertEquals(1, state.getCurrentMana());
		assertEquals(5, state.getSpawnCooldownSeconds());
	}

	@Test
	void fullManaDoesNotStoreInstantRecoveryTick() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);

		for (int second = 1; second <= 14; second++) {
			service.tickSecond(3, second);
		}
		state.setCurrentMana(2);
		service.tickSecond(3, 15);

		assertEquals(2, state.getCurrentMana());
		assertEquals(14, state.getSecondsUntilNextMana());
	}
}
