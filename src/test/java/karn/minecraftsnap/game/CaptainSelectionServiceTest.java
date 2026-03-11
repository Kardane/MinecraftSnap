package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptainSelectionServiceTest {
	@Test
	void captainPreferenceWinsInsideTeam() {
		var service = new CaptainSelectionService();
		var preferredCaptain = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "captain", 250, "captain", TeamId.RED, false);
		var normal = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "normal", 300, "none", TeamId.RED, false);

		var result = service.selectCaptains(List.of(preferredCaptain, normal), Map.of(
			preferredCaptain.playerId(), TeamId.RED,
			normal.playerId(), TeamId.RED
		));

		assertEquals(preferredCaptain.playerId(), result.get(TeamId.RED));
	}

	@Test
	void forcedCaptainStaysCaptain() {
		var service = new CaptainSelectionService();
		var forced = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "forced", 100, "unit:piglin", TeamId.BLUE, true);
		var preferred = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "preferred", 300, "captain", TeamId.BLUE, false);

		var result = service.selectCaptains(List.of(forced, preferred), Map.of(
			forced.playerId(), TeamId.BLUE,
			preferred.playerId(), TeamId.BLUE
		));

		assertEquals(forced.playerId(), result.get(TeamId.BLUE));
	}

	@Test
	void unitPreferenceDoesNotBeatCaptainPreference() {
		var service = new CaptainSelectionService();
		var preferredCaptain = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "captain", 100, "captain", TeamId.RED, false);
		var preferredUnit = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "unit", 500, "unit:villager", TeamId.RED, false);

		var result = service.selectCaptains(List.of(preferredCaptain, preferredUnit), Map.of(
			preferredCaptain.playerId(), TeamId.RED,
			preferredUnit.playerId(), TeamId.RED
		));

		assertEquals(preferredCaptain.playerId(), result.get(TeamId.RED));
	}
}
