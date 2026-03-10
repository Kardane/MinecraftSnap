package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchManagerTest {
	@Test
	void allPointsHoldTriggersWinner() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);

		for (int i = 0; i < 29; i++) {
			manager.recordAllPointsHeld(TeamId.RED, 30);
		}

		assertEquals(MatchPhase.GAME_RUNNING, manager.getPhase());
		manager.recordAllPointsHeld(TeamId.RED, 30);
		assertEquals(MatchPhase.GAME_END, manager.getPhase());
		assertEquals(TeamId.RED, manager.getWinnerTeam());
	}

	@Test
	void scoreWinnerResolvedOnTimeout() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);
		manager.addScore(TeamId.BLUE, 3);

		for (int i = 0; i < manager.getTotalSeconds(); i++) {
			manager.tick();
			for (int tick = 0; tick < 19; tick++) {
				manager.tick();
			}
		}

		assertEquals(MatchPhase.GAME_END, manager.getPhase());
		assertEquals(TeamId.BLUE, manager.getWinnerTeam());
	}

	@Test
	void drawKeepsWinnerNull() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);

		for (int i = 0; i < manager.getTotalSeconds(); i++) {
			for (int tick = 0; tick < 20; tick++) {
				manager.tick();
			}
		}

		assertEquals(MatchPhase.GAME_END, manager.getPhase());
		assertNull(manager.getWinnerTeam());
	}
}
