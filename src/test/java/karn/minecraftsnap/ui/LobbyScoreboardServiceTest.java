package karn.minecraftsnap.ui;

import karn.minecraftsnap.game.MatchPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyScoreboardServiceTest {
	@Test
	void hidesSidebarDuringGamePhases() {
		assertTrue(LobbyScoreboardService.shouldHideSidebar(MatchPhase.GAME_START));
		assertFalse(LobbyScoreboardService.shouldHideSidebar(MatchPhase.GAME_RUNNING));
		assertTrue(LobbyScoreboardService.shouldHideSidebar(MatchPhase.GAME_END));
		assertFalse(LobbyScoreboardService.shouldHideSidebar(MatchPhase.FACTION_SELECT));
	}
}
