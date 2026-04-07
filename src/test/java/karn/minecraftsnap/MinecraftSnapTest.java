package karn.minecraftsnap;

import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
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
	void captainItemDropIsAlwaysBlocked() {
		assertTrue(MinecraftSnap.shouldBlockCaptainItemDrop(RoleType.CAPTAIN));
		assertFalse(MinecraftSnap.shouldBlockCaptainItemDrop(RoleType.UNIT));
		assertFalse(MinecraftSnap.shouldBlockCaptainItemDrop(RoleType.NONE));
	}

	@Test
	void unitShortcutOnlyUsesUnitActionDuringGameRunning() {
		assertTrue(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_RUNNING, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_START, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.LOBBY, karn.minecraftsnap.game.RoleType.UNIT, "villager", false));
		assertFalse(MinecraftSnap.shouldUseUnitShortcut(MatchPhase.GAME_RUNNING, karn.minecraftsnap.game.RoleType.CAPTAIN, "villager", false));
	}

	@Test
	void fallDamageIsEnabledOnlyDuringLiveGamePhases() {
		assertFalse(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.LOBBY));
		assertFalse(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.TEAM_SELECT));
		assertFalse(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.FACTION_SELECT));
		assertTrue(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.GAME_START));
		assertTrue(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.GAME_RUNNING));
		assertFalse(MinecraftSnap.isFallDamageEnabledForPhase(MatchPhase.GAME_END));
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

	@Test
	void playTimeTracksOnlyActiveParticipantsDuringLivePhases() {
		var unit = new PlayerMatchState();
		unit.setTeam(TeamId.RED, RoleType.UNIT);
		var captain = new PlayerMatchState();
		captain.setTeam(TeamId.BLUE, RoleType.CAPTAIN);
		var spectator = new PlayerMatchState();
		spectator.setTeam(TeamId.RED, RoleType.SPECTATOR);

		assertTrue(MinecraftSnap.shouldTrackPlayTime(MatchPhase.GAME_START, unit, false));
		assertTrue(MinecraftSnap.shouldTrackPlayTime(MatchPhase.GAME_RUNNING, captain, false));
		assertFalse(MinecraftSnap.shouldTrackPlayTime(MatchPhase.LOBBY, unit, false));
		assertFalse(MinecraftSnap.shouldTrackPlayTime(MatchPhase.GAME_RUNNING, spectator, false));
		assertFalse(MinecraftSnap.shouldTrackPlayTime(MatchPhase.GAME_RUNNING, unit, true));
	}

	@Test
	void ladderChangeFormattingUsesSignedDeltaAndFixedSubtitleShape() {
		assertEquals("+12", MinecraftSnap.formatSignedLadderDelta(12));
		assertEquals("-7", MinecraftSnap.formatSignedLadderDelta(-7));
		assertEquals("0", MinecraftSnap.formatSignedLadderDelta(0));
		assertEquals("(310) -> (322)", MinecraftSnap.formatLadderChangeSubtitle(310, 322));
	}

	@Test
	void taggedGhastOwnerTakesPriorityForProjectileHooks() {
		var taggedOwner = java.util.UUID.randomUUID();
		var currentOwner = java.util.UUID.randomUUID();

		assertEquals(taggedOwner, MinecraftSnap.resolveProjectileHookOwnerId(currentOwner, taggedOwner));
		assertEquals(currentOwner, MinecraftSnap.resolveProjectileHookOwnerId(currentOwner, null));
		assertEquals(taggedOwner, MinecraftSnap.resolveProjectileHookOwnerId(null, taggedOwner));
	}
}
