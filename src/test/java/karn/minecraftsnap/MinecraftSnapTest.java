package karn.minecraftsnap;

import karn.minecraftsnap.game.MatchPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftSnapTest {
	@Test
	void captainItemsCanBeUsedDuringGameStartAndRunningOnly() {
		assertTrue(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_START));
		assertTrue(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_RUNNING));
		assertFalse(MinecraftSnap.canCaptainUseItems(MatchPhase.FACTION_SELECT));
		assertFalse(MinecraftSnap.canCaptainUseItems(MatchPhase.GAME_END));
	}
}
