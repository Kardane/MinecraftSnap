package karn.minecraftsnap;

import karn.minecraftsnap.game.MatchPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void unitShortcutOnlyUsesUnitActionDuringGameRunning() {
		assertTrue(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_RUNNING, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_START, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.LOBBY, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_RUNNING, karn.minecraftsnap.game.RoleType.CAPTAIN, "villager", false));
	}

	@Test
	void captainSkillUnlockAnnouncementOnlyTriggersOnceAfterOneMinute() {
		assertFalse(MinecraftSnap.shouldAnnounceCaptainSkillUnlock(MatchPhase.GAME_START, 60, false));
		assertFalse(MinecraftSnap.shouldAnnounceCaptainSkillUnlock(MatchPhase.GAME_RUNNING, 59, false));
		assertTrue(MinecraftSnap.shouldAnnounceCaptainSkillUnlock(MatchPhase.GAME_RUNNING, 60, false));
		assertTrue(MinecraftSnap.shouldAnnounceCaptainSkillUnlock(MatchPhase.GAME_RUNNING, 120, false));
		assertFalse(MinecraftSnap.shouldAnnounceCaptainSkillUnlock(MatchPhase.GAME_RUNNING, 60, true));
	}

	@Test
	void captainManaRecoveryCountsOnlyNonCaptainTeammates() {
		var manager = new karn.minecraftsnap.game.MatchManager();
		var captain = java.util.UUID.randomUUID();
		var unit1 = java.util.UUID.randomUUID();
		var unit2 = java.util.UUID.randomUUID();
		var spectator = java.util.UUID.randomUUID();

		manager.setRole(captain, karn.minecraftsnap.game.TeamId.RED, karn.minecraftsnap.game.RoleType.CAPTAIN);
		manager.setRole(unit1, karn.minecraftsnap.game.TeamId.RED, karn.minecraftsnap.game.RoleType.UNIT);
		manager.setRole(unit2, karn.minecraftsnap.game.TeamId.RED, karn.minecraftsnap.game.RoleType.UNIT);
		manager.setRole(spectator, karn.minecraftsnap.game.TeamId.RED, karn.minecraftsnap.game.RoleType.SPECTATOR);

		assertEquals(3, MinecraftSnap.countCaptainRecoveryTeammates(manager, karn.minecraftsnap.game.TeamId.RED));
		assertEquals(10, MinecraftSnap.captainManaRecoverySeconds(manager, karn.minecraftsnap.game.TeamId.RED));
	}
}
