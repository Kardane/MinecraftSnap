package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptainSkillServiceTest {
	@Test
	void skillCostMatchesFactionSpec() {
		assertEquals(4, CaptainSkillService.skillCostFor(FactionId.VILLAGER));
		assertEquals(4, CaptainSkillService.skillCostFor(FactionId.MONSTER));
		assertEquals(5, CaptainSkillService.skillCostFor(FactionId.NETHER));
	}

	@Test
	void skillCooldownMatchesFactionSpec() {
		assertEquals(60, CaptainSkillService.skillCooldownFor(FactionId.VILLAGER));
		assertEquals(60, CaptainSkillService.skillCooldownFor(FactionId.MONSTER));
		assertEquals(45, CaptainSkillService.skillCooldownFor(FactionId.NETHER));
	}

	@Test
	void refundAmountFloorsFortyPercent() {
		assertEquals(0, CaptainSkillService.refundAmount(1));
		assertEquals(1, CaptainSkillService.refundAmount(4));
		assertEquals(2, CaptainSkillService.refundAmount(5));
		assertEquals(12, CaptainSkillService.refundAmount(30));
	}
}
