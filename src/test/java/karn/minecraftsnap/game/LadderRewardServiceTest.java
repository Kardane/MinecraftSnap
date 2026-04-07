package karn.minecraftsnap.game;

import karn.minecraftsnap.config.ServerStatsRepository;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LadderRewardServiceTest {
	@Test
	void winningUnitPerformanceIncreasesReward() {
		var config = new SystemConfig.LadderRewardConfig();

		assertEquals(5, LadderRewardService.calculateWinningUnitAmount(0, 0, config));
		assertEquals(10, LadderRewardService.calculateWinningUnitAmount(3, 24, config));
	}

	@Test
	void losingUnitPerformanceReducesPenaltyWithoutBecomingReward() {
		var config = new SystemConfig.LadderRewardConfig();

		assertEquals(5, LadderRewardService.calculateLosingUnitAmount(0, 0, config));
		assertEquals(0, LadderRewardService.calculateLosingUnitAmount(3, 24, config));
		assertEquals(0, LadderRewardService.calculateLosingUnitAmount(20, 120, config));
	}

	@Test
	void earlySurrenderStillHalvesUnitDelta() {
		assertEquals(2, LadderRewardService.adjustMatchLadderDeltaForEarlySurrender(5, 120, true));
		assertEquals(5, LadderRewardService.adjustMatchLadderDeltaForEarlySurrender(5, 181, true));
		assertEquals(5, LadderRewardService.adjustMatchLadderDeltaForEarlySurrender(5, 120, false));
	}

	@Test
	void unitKillAndDeathStatsAreRecordedWhenServerStatsRepositoryIsWired(@TempDir Path tempDir) {
		var logger = LoggerFactory.getLogger(LadderRewardServiceTest.class);
		var matchManager = new MatchManager();
		var statsRepository = new StatsRepository(tempDir.resolve("stats.json"), logger);
		var serverStatsRepository = new ServerStatsRepository(tempDir.resolve("server_stats.json"), logger);
		statsRepository.load();
		serverStatsRepository.load();
		var service = new InGameRuleService(
			matchManager,
			statsRepository,
			new TextTemplateResolver(),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			serverStatsRepository
		);
		var killerId = UUID.randomUUID();
		var victimId = UUID.randomUUID();
		matchManager.setRole(killerId, TeamId.RED, RoleType.UNIT);
		matchManager.setCurrentUnit(killerId, "skeleton");
		matchManager.setRole(victimId, TeamId.BLUE, RoleType.UNIT);
		matchManager.setCurrentUnit(victimId, "zombie");

		service.recordKillAndDeath(victimId, "피해자", killerId, "공격자");

		assertEquals(1, serverStatsRepository.findUnitStats("skeleton").kills);
		assertEquals(1, serverStatsRepository.findUnitStats("zombie").deaths);
	}
}
