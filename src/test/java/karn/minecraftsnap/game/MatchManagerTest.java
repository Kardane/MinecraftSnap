package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchManagerTest {
	@Test
	void adjustDurationTicksExtendsRemainingTimeWithoutChangingElapsedSeconds() {
		var manager = new MatchManager();
		manager.applyGameDuration(10);
		manager.setPhase(MatchPhase.GAME_RUNNING);
		for (int i = 0; i < 45; i++) {
			manager.tick();
		}

		manager.adjustDurationTicks(30);

		assertEquals(2, manager.getElapsedSeconds());
		assertEquals(185, manager.getRemainingTicks());
	}

	@Test
	void adjustDurationTicksToZeroEndsMatchByScore() {
		var manager = new MatchManager();
		manager.applyGameDuration(10);
		manager.setPhase(MatchPhase.GAME_RUNNING);
		manager.addScore(TeamId.RED, 1);

		manager.adjustDurationTicks(-manager.getRemainingTicks());

		assertEquals(MatchPhase.GAME_END, manager.getPhase());
		assertEquals(TeamId.RED, manager.getWinnerTeam());
	}

	@Test
	void surrenderDeclaresOpponentWinner() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);

		manager.declareSurrender(TeamId.RED);

		assertEquals(MatchPhase.GAME_END, manager.getPhase());
		assertEquals(TeamId.BLUE, manager.getWinnerTeam());
	}

	@Test
	void allPointsHoldTriggersWinner() {
		var manager = new MatchManager();
		manager.setPhase(MatchPhase.GAME_RUNNING);

		for (int i = 0; i < 29; i++) {
			manager.recordAllPointsHeld(TeamId.RED, 30);
		}

		assertEquals(MatchPhase.GAME_RUNNING, manager.getPhase());
		assertEquals(TeamId.RED, manager.getAllPointsHeldTeam());
		assertEquals(29, manager.getAllPointsHeldSeconds());
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

	@Test
	void manualCaptainAssignmentDemotesPreviousCaptainToUnit() {
		var manager = new MatchManager();
		var firstCaptain = UUID.randomUUID();
		var secondCaptain = UUID.randomUUID();
		manager.setRole(firstCaptain, TeamId.RED, RoleType.CAPTAIN);
		manager.getPlayerState(firstCaptain).setCurrentUnitId("villager");
		manager.setRole(secondCaptain, TeamId.RED, RoleType.UNIT);

		manager.setCaptain(TeamId.RED, secondCaptain);

		assertEquals(RoleType.UNIT, manager.getPlayerState(firstCaptain).getRoleType());
		assertEquals(TeamId.RED, manager.getPlayerState(firstCaptain).getTeamId());
		assertNull(manager.getPlayerState(firstCaptain).getCurrentUnitId());
		assertEquals(RoleType.CAPTAIN, manager.getPlayerState(secondCaptain).getRoleType());
		assertEquals(TeamId.RED, manager.getPlayerState(secondCaptain).getTeamId());
	}

	@Test
	void manualCaptainAssignmentDemotesEveryOtherCaptainOnSameTeam() {
		var manager = new MatchManager();
		var firstCaptain = UUID.randomUUID();
		var secondCaptain = UUID.randomUUID();
		var selectedCaptain = UUID.randomUUID();
		manager.setRole(firstCaptain, TeamId.BLUE, RoleType.CAPTAIN);
		manager.setRole(secondCaptain, TeamId.BLUE, RoleType.CAPTAIN);
		manager.setRole(selectedCaptain, TeamId.BLUE, RoleType.UNIT);

		manager.setCaptain(TeamId.BLUE, selectedCaptain);

		assertEquals(RoleType.UNIT, manager.getPlayerState(firstCaptain).getRoleType());
		assertEquals(RoleType.UNIT, manager.getPlayerState(secondCaptain).getRoleType());
		assertEquals(RoleType.CAPTAIN, manager.getPlayerState(selectedCaptain).getRoleType());
		assertEquals(selectedCaptain, manager.getCaptainId(TeamId.BLUE));
		assertEquals(1, manager.getCaptainIds().stream()
			.filter(captainId -> manager.getPlayerState(captainId).getTeamId() == TeamId.BLUE)
			.count());
	}

	@Test
	void clearPlayerAssignmentsResetsTeamRoleAndFaction() {
		var manager = new MatchManager();
		var playerId = UUID.randomUUID();
		manager.setRole(playerId, TeamId.BLUE, RoleType.UNIT);
		manager.getPlayerState(playerId).setFactionId(FactionId.NETHER);
		manager.getPlayerState(playerId).setCurrentUnitId("piglin");
		manager.setFactionSelection(TeamId.BLUE, FactionId.NETHER);

		manager.clearPlayerAssignments();

		assertNull(manager.getPlayerState(playerId).getTeamId());
		assertEquals(RoleType.NONE, manager.getPlayerState(playerId).getRoleType());
		assertNull(manager.getPlayerState(playerId).getFactionId());
		assertNull(manager.getPlayerState(playerId).getCurrentUnitId());
		assertNull(manager.getFactionSelection(TeamId.BLUE));
	}
}
