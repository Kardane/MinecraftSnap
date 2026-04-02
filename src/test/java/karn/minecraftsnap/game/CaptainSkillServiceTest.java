package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptainSkillServiceTest {
	@Test
	void revealManaRefillTargetsOnlyTrailingTeam() {
		assertEquals(false, CaptainManaService.shouldRefillOnReveal(TeamId.RED, 10, 8));
		assertEquals(true, CaptainManaService.shouldRefillOnReveal(TeamId.BLUE, 10, 8));
		assertEquals(true, CaptainManaService.shouldRefillOnReveal(TeamId.RED, 12, 12));
		assertEquals(true, CaptainManaService.shouldRefillOnReveal(TeamId.BLUE, 12, 12));
	}

	@Test
	void skillCooldownMatchesFactionSpec() {
		assertEquals(80, CaptainSkillService.skillCooldownFor(FactionId.VILLAGER));
		assertEquals(60, CaptainSkillService.skillCooldownFor(FactionId.MONSTER));
		assertEquals(90, CaptainSkillService.skillCooldownFor(FactionId.NETHER));
	}

	@Test
	void captainSkillUnlocksAfterOneMinute() {
		assertEquals(false, CaptainSkillService.isSkillUnlocked(-1));
		assertEquals(false, CaptainSkillService.isSkillUnlocked(0));
		assertEquals(false, CaptainSkillService.isSkillUnlocked(59));
		assertEquals(true, CaptainSkillService.isSkillUnlocked(60));
		assertEquals(true, CaptainSkillService.isSkillUnlocked(61));
	}

	@Test
	void delayedAmmoRestockWaitsOneSecondAfterAmmoDisappears() {
		assertEquals(true, UnitSpawnService.usesDelayedAmmoRestock("skeleton"));
		assertEquals(true, UnitSpawnService.usesDelayedAmmoRestock("stray"));
		assertEquals(true, UnitSpawnService.usesDelayedAmmoRestock("bogged"));
		assertEquals(false, UnitSpawnService.usesDelayedAmmoRestock("pillager"));

		assertEquals(120L, UnitSpawnService.nextAmmoRestockTick("skeleton", false, null, 100L));
		assertEquals(120L, UnitSpawnService.nextAmmoRestockTick("skeleton", false, 120L, 101L));
		assertEquals(null, UnitSpawnService.nextAmmoRestockTick("skeleton", true, 120L, 110L));
		assertEquals(true, UnitSpawnService.shouldRestockAmmo("skeleton", false, 120L, 120L));
		assertEquals(false, UnitSpawnService.shouldRestockAmmo("skeleton", false, 120L, 119L));
	}
}
