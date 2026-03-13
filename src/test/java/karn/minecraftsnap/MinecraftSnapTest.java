package karn.minecraftsnap;

import karn.minecraftsnap.game.MatchPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftSnapTest {
	@Test
	void captainItemsCanBeUsedDuringGameStartAndRunningOnly() {
		assertTrue(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_START));
		assertTrue(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_RUNNING));
		assertFalse(MinecraftSnap.canCaptainUseItems(MatchPhase.FACTION_SELECT));
		assertFalse(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_END));
	}

	@Test
	void unitMatchLadderAmountStaysWithinConfiguredRange() {
		assertEquals(10, MinecraftSnap.unitMatchLadderAmount(0, 0));
		assertEquals(10, MinecraftSnap.unitMatchLadderAmount(0, 5));
		assertEquals(15, MinecraftSnap.unitMatchLadderAmount(5, 10));
		assertEquals(20, MinecraftSnap.unitMatchLadderAmount(10, 10));
	}
}
