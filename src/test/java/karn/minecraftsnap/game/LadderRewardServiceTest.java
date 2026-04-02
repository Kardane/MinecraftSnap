package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LadderRewardServiceTest {
	private final karn.minecraftsnap.config.SystemConfig.LadderRewardConfig config = new karn.minecraftsnap.config.SystemConfig.LadderRewardConfig();

	@Test
	void unitLadderFormulaCalculatesCorrectly() {
		// 5 kills, 24 capture score: 5 + 24/12 + 5 = 12
		assertEquals(12, LadderRewardService.calculateUnitAmount(5, 24, config));
		
		// 0 kills, 0 capture score: 0 + 0 + 5 = 5
		assertEquals(5, LadderRewardService.calculateUnitAmount(0, 0, config));
		
		// 10 kills, 6 capture score: 10 + 0.5 + 5 = 15.5 -> round to 16
		assertEquals(16, LadderRewardService.calculateUnitAmount(10, 6, config));

		// 2 kills, 60 capture score: 2 + 5 + 5 = 12
		assertEquals(12, LadderRewardService.calculateUnitAmount(2, 60, config));
	}

	@Test
	void captainLadderFormulaCalculatesCorrectly() {
		// 100 vs 20: 80 + 30 = 110 -> capped to 100
		assertEquals(100, LadderRewardService.calculateCaptainAmount(100, 20, config));

		// 50 vs 50: 0 + 30 = 30
		assertEquals(30, LadderRewardService.calculateCaptainAmount(50, 50, config));

		// 40 vs 45: 5 + 30 = 35
		assertEquals(35, LadderRewardService.calculateCaptainAmount(40, 45, config));
	}

	@Test
	void ladderFormulaUsesConfiguredWeights() {
		config.captainBase = 12;
		config.captainScoreGapMultiplier = 2;
		config.unitBase = 3.5f;
		config.unitKillWeight = 1.5f;
		config.unitCaptureScoreDivisor = 6.0f;

		assertEquals(100, LadderRewardService.calculateCaptainAmount(100, 20, config));
		assertEquals(15, LadderRewardService.calculateUnitAmount(5, 24, config));
	}

	@Test
	void unitLadderRewardIsCappedAtOneHundred() {
		config.unitBase = 50.0f;
		config.unitKillWeight = 10.0f;
		config.unitCaptureScoreDivisor = 1.0f;

		assertEquals(100, LadderRewardService.calculateUnitAmount(10, 80, config));
	}
}
