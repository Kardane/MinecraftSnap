package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VictoryCountdownServiceTest {
	@Test
	void remainingSecondsCountsDownFromHoldWindow() {
		assertEquals(30, VictoryCountdownService.remainingSeconds(1, 30));
		assertEquals(16, VictoryCountdownService.remainingSeconds(15, 30));
		assertEquals(1, VictoryCountdownService.remainingSeconds(30, 30));
		assertEquals(0, VictoryCountdownService.remainingSeconds(0, 30));
	}

	@Test
	void tickingCountdownAlsoTriggersSound() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);
		manager.recordAllPointsHeld(TeamId.RED, 30);
		var soundCalls = new int[1];
		var service = new VictoryCountdownService(manager, (teamId, remaining) -> {
		}, remaining -> soundCalls[0]++);

		service.tick(new SystemConfig());

		assertEquals(1, soundCalls[0]);
	}
}
