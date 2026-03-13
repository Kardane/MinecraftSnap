package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignmentServiceTest {
	@Test
	void greedyBalanceSplitsHighLadderPlayers() {
		var service = new TeamAssignmentService(new Random(0L));
		var players = List.of(
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p1", 400, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p2", 300, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p3", 200, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p4", 100, "none", null, false)
		);

		var result = service.assignTeams(players);
		int red = players.stream().filter(it -> result.get(it.playerId()) == TeamId.RED).mapToInt(TeamAssignmentService.PlayerCandidate::ladder).sum();
		int blue = players.stream().filter(it -> result.get(it.playerId()) == TeamId.BLUE).mapToInt(TeamAssignmentService.PlayerCandidate::ladder).sum();

		assertEquals(1000, red + blue);
		assertTrue(Math.abs(red - blue) <= 100);
	}

	@Test
	void lockedTeamRespected() {
		var service = new TeamAssignmentService();
		var locked = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "locked", 300, "none", TeamId.RED, true);
		var free = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "free", 200, "none", null, false);

		var result = service.assignTeams(List.of(locked, free));

		assertEquals(TeamId.RED, result.get(locked.playerId()));
	}

	@Test
	void similarLadderUsesRandomTieBreak() {
		var players = List.of(
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p1", 250, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p2", 250, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p3", 250, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p4", 250, "none", null, false)
		);

		var first = new TeamAssignmentService(new Random(0L)).assignTeams(players);
		var second = new TeamAssignmentService(new Random(1L)).assignTeams(players);

		assertNotEquals(first, second);
	}
}
