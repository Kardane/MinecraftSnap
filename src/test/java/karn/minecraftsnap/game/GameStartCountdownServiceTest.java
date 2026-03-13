package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameStartCountdownServiceTest {
	@Test
	void remainingSecondsUsesGameStartWaitSeconds() {
		assertEquals(15, GameStartCountdownService.remainingSeconds(0L, 15));
		assertEquals(14, GameStartCountdownService.remainingSeconds(20L, 15));
		assertEquals(1, GameStartCountdownService.remainingSeconds(280L, 15));
		assertEquals(0, GameStartCountdownService.remainingSeconds(400L, 15));
	}

	@Test
	void tickingCountdownTriggersSoundEverySecond() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_START);
		var soundCalls = new int[1];
		var service = new GameStartCountdownService(manager, remaining -> {
		}, remaining -> soundCalls[0]++);

		service.tick(new SystemConfig());

		assertEquals(1, soundCalls[0]);
	}
}
