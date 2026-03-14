package karn.minecraftsnap.audio;

import karn.minecraftsnap.game.MatchPhase;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseMusicServiceTest {
	@Test
	void phaseModeMatchesAudioPlan() {
		assertEquals(PhaseMusicService.PhaseMode.LOBBY, PhaseMusicService.phaseMode(MatchPhase.LOBBY));
		assertEquals(PhaseMusicService.PhaseMode.READY, PhaseMusicService.phaseMode(MatchPhase.TEAM_SELECT));
		assertEquals(PhaseMusicService.PhaseMode.READY, PhaseMusicService.phaseMode(MatchPhase.FACTION_SELECT));
		assertEquals(PhaseMusicService.PhaseMode.NONE, PhaseMusicService.phaseMode(MatchPhase.GAME_START));
		assertEquals(PhaseMusicService.PhaseMode.GAME, PhaseMusicService.phaseMode(MatchPhase.GAME_RUNNING));
		assertEquals(PhaseMusicService.PhaseMode.NONE, PhaseMusicService.phaseMode(MatchPhase.GAME_END));
	}

	@Test
	void startCuePlaysOnlyWhenGameActuallyStarts() {
		assertTrue(PhaseMusicService.shouldPlayStartCue(MatchPhase.GAME_START, MatchPhase.GAME_RUNNING));
		assertFalse(PhaseMusicService.shouldPlayStartCue(MatchPhase.LOBBY, MatchPhase.GAME_RUNNING));
	}

	@Test
	void endCuePlaysOnGameEndEntry() {
		assertTrue(PhaseMusicService.shouldPlayEndCue(MatchPhase.GAME_RUNNING, MatchPhase.GAME_END));
		assertTrue(PhaseMusicService.shouldPlayEndCue(MatchPhase.GAME_START, MatchPhase.GAME_END));
		assertFalse(PhaseMusicService.shouldPlayEndCue(MatchPhase.GAME_END, MatchPhase.GAME_END));
	}

	@Test
	void lobbyTrackSelectionAvoidsImmediateRepeatWhenPossible() {
		for (int seed = 0; seed < 20; seed++) {
			int next = PhaseMusicService.nextLobbyTrackIndex(0, 2, new Random(seed));
			assertNotEquals(0, next);
		}
	}
}
