package karn.minecraftsnap.ui;

import karn.minecraftsnap.game.MatchPhase;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void ranksPlayersByLadderThenWinRateThenGames() {
		var ranked = LobbyScoreboardService.rankEntries(java.util.List.of(
			new LobbyScoreboardService.LobbyRankingEntry("alpha", 500, 6, 4),
			new LobbyScoreboardService.LobbyRankingEntry("bravo", 600, 1, 9),
			new LobbyScoreboardService.LobbyRankingEntry("charlie", 500, 8, 2),
			new LobbyScoreboardService.LobbyRankingEntry("delta", 500, 8, 1)
		));

		assertEquals("bravo", ranked.get(0).playerName());
		assertEquals("delta", ranked.get(1).playerName());
		assertEquals("charlie", ranked.get(2).playerName());
		assertEquals("alpha", ranked.get(3).playerName());
	}

	@Test
	void sidebarRankingIsLimitedToFifteenPlayers() {
		var entries = java.util.stream.IntStream.range(0, 20)
			.mapToObj(index -> new LobbyScoreboardService.LobbyRankingEntry("p" + index, 1000 - index, 1, 0))
			.toList();

		var ranked = LobbyScoreboardService.rankEntries(entries);

		assertEquals(15, ranked.size());
		assertEquals("p0", ranked.getFirst().playerName());
		assertEquals("p14", ranked.getLast().playerName());
	}

	@Test
	void sidebarUsesBlankNumberFormatToHideVanillaScores() {
		assertEquals(BlankNumberFormat.INSTANCE, LobbyScoreboardService.sidebarNumberFormat());
	}
}
