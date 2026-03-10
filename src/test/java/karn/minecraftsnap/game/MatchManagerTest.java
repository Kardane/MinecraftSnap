package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

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

	@Test
	void selectedFactionAppliedOnGameStart() {
		var manager = new MatchManager();
		var playerId = UUID.randomUUID();
		manager.setRole(playerId, TeamId.RED, RoleType.UNIT);
		manager.setFactionSelection(TeamId.RED, FactionId.MONSTER);
		manager.startGameRunning();

		assertEquals(FactionId.MONSTER, manager.getPlayerState(playerId).getFactionId());
	}

	@Test
	void teamSelectClearsPreviousFactionState() {
		var manager = new MatchManager();
		var playerId = UUID.randomUUID();
		manager.setRole(playerId, TeamId.BLUE, RoleType.CAPTAIN);
		manager.getPlayerState(playerId).setFactionId(FactionId.NETHER);
		manager.setFactionSelection(TeamId.BLUE, FactionId.NETHER);

		manager.setPhase(MatchPhase.TEAM_SELECT);

		assertNull(manager.getPlayerState(playerId).getFactionId());
		assertNull(manager.getFactionSelection(TeamId.BLUE));
	}

	@Test
	void gameStartPhaseDoesNotAdvanceClock() {
		var manager = new MatchManager();
		manager.enterGameStart();

		for (int i = 0; i < 60; i++) {
			manager.tick();
		}

		assertEquals(MatchPhase.GAME_START, manager.getPhase());
		assertEquals(manager.getTotalSeconds(), manager.getRemainingSeconds());
	}

	@Test
	void startGameRunningRevealsOnlyFirstLane() {
		var manager = new MatchManager();

		manager.startGameRunning();

		assertEquals(MatchPhase.GAME_RUNNING, manager.getPhase());
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_1));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_2));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_3));
	}

	@Test
	void enteringGameStartClearsLaneRevealState() {
		var manager = new MatchManager();
		manager.revealLane(LaneId.LANE_1);
		manager.revealLane(LaneId.LANE_2);

		manager.enterGameStart();

		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_1));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_2));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_3));
	}
}
