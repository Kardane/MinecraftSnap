package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptainManaServiceTest {
	@Test
	void restoresOneManaEveryTenSeconds() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(1);

		for (int second = 1; second <= 10; second++) {
			service.tickSecond(3, second, 10);
		}

		assertEquals(2, state.getCurrentMana());
		assertEquals(10, state.getSecondsUntilNextMana());
	}

	@Test
	void netherCaptainRecoversOneAdditionalManaEveryTickCycle() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		service.setCaptainFaction(captainId, FactionId.NETHER);
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(0);
		state.setMaxMana(5);

		for (int second = 1; second <= 10; second++) {
			service.tickSecond(3, second, 10);
		}

		assertEquals(2, state.getCurrentMana());
		assertEquals(10, state.getSecondsUntilNextMana());
	}

	@Test
	void increasesMaxManaEveryMinuteAndRefillsGainedMana() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(1);

		service.tickSecond(3, 60, 10);
		service.tickSecond(3, 120, 10);

		assertEquals(5, state.getMaxMana());
		assertEquals(3, state.getCurrentMana());
	}

	@Test
	void refillOnBiomeRevealSetsCurrentManaToMax() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(0);
		state.setMaxMana(6);

		service.refillMana(captainId, 10);

		assertEquals(6, state.getCurrentMana());
		assertEquals(10, state.getSecondsUntilNextMana());
	}

	@Test
	void spendingManaForSpawnDoesNotStartCooldown() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);

		var spent = service.trySpendForSpawn(captainId, 2);

		assertTrue(spent);
		assertEquals(1, state.getCurrentMana());
	}

	@Test
	void fullManaDoesNotStoreInstantRecoveryTick() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);

		for (int second = 1; second <= 9; second++) {
			service.tickSecond(3, second, 10);
		}
		state.setCurrentMana(2);
		service.tickSecond(3, 10, 10);

		assertEquals(2, state.getCurrentMana());
		assertEquals(9, state.getSecondsUntilNextMana());
	}

	@Test
	void spendingSkillManaStartsSkillCooldown() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(4);
		state.setMaxMana(4);

		var spent = service.trySpendForSkill(captainId, 4, 60);

		assertTrue(spent);
		assertEquals(0, state.getCurrentMana());
		assertEquals(60, state.getSkillCooldownSeconds());
	}

	@Test
	void restoreManaClampsManaToMax() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(2);

		service.restoreMana(captainId, 5);

		assertEquals(3, state.getCurrentMana());
	}

	@Test
	void netherCaptainRestoreManaGetsOneAdditionalMana() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		service.setCaptainFaction(captainId, FactionId.NETHER);
		var state = service.getOrCreate(captainId);
		state.setCurrentMana(0);
		state.setMaxMana(5);

		service.restoreMana(captainId, 1);

		assertEquals(2, state.getCurrentMana());
	}

	@Test
	void cannotSpendSkillManaWhileSkillCooldownActive() {
		var service = new CaptainManaService();
		var captainId = UUID.randomUUID();
		var state = service.getOrCreate(captainId);
		state.setSkillCooldownSeconds(5);

		var spent = service.trySpendForSkill(captainId, 1, 10);

		assertFalse(spent);
		assertEquals(3, state.getCurrentMana());
		assertEquals(5, state.getSkillCooldownSeconds());
	}

	@Test
	void recoverySecondsShrinkWithMoreTeammatesUntilFiveSeconds() {
		assertEquals(10, CaptainManaService.recoverySecondsForTeamSize(0));
		assertEquals(10, CaptainManaService.recoverySecondsForTeamSize(4));
		assertEquals(9, CaptainManaService.recoverySecondsForTeamSize(5));
		assertEquals(8, CaptainManaService.recoverySecondsForTeamSize(6));
		assertEquals(7, CaptainManaService.recoverySecondsForTeamSize(7));
		assertEquals(6, CaptainManaService.recoverySecondsForTeamSize(8));
		assertEquals(5, CaptainManaService.recoverySecondsForTeamSize(9));
		assertEquals(5, CaptainManaService.recoverySecondsForTeamSize(20));
	}
}
