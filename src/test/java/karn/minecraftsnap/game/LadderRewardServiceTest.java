package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LadderRewardServiceTest {

	@Test
	void unitLadderFormulaCalculatesCorrectly() {
		// 5 kills, 24 capture score: 5 + 24/12 + 5 = 12
		assertEquals(12, LadderRewardService.calculateUnitAmount(5, 24));
		
		// 0 kills, 0 capture score: 0 + 0 + 5 = 5
		assertEquals(5, LadderRewardService.calculateUnitAmount(0, 0));
		
		// 10 kills, 6 capture score: 10 + 0.5 + 5 = 15.5 -> round to 16
		assertEquals(16, LadderRewardService.calculateUnitAmount(10, 6));

		// 2 kills, 60 capture score: 2 + 5 + 5 = 12
		assertEquals(12, LadderRewardService.calculateUnitAmount(2, 60));
	}

	@Test
	void captainLadderFormulaCalculatesCorrectly() {
		// 100 vs 20: 80 + 30 = 110
		assertEquals(110, LadderRewardService.calculateCaptainAmount(100, 20));

		// 50 vs 50: 0 + 30 = 30
		assertEquals(30, LadderRewardService.calculateCaptainAmount(50, 50));

		// 40 vs 45: 5 + 30 = 35
		assertEquals(35, LadderRewardService.calculateCaptainAmount(40, 45));
	}
}
