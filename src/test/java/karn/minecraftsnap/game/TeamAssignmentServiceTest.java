package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamAssignmentServiceTest {
	@Test
	void greedyBalanceSplitsHighLadderPlayers() {
		var service = new TeamAssignmentService();
		var players = List.of(
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p1", 400, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p2", 300, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p3", 200, "none", null, false),
			new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "p4", 100, "none", null, false)
		);

		var result = service.assignTeams(players);
		int red = players.stream().filter(it -> result.get(it.playerId()) == TeamId.RED).mapToInt(TeamAssignmentService.PlayerCandidate::ladder).sum();
		int blue = players.stream().filter(it -> result.get(it.playerId()) == TeamId.BLUE).mapToInt(TeamAssignmentService.PlayerCandidate::ladder).sum();

		assertEquals(500, red);
		assertEquals(500, blue);
	}

	@Test
	void lockedTeamRespected() {
		var service = new TeamAssignmentService();
		var locked = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "locked", 300, "none", TeamId.RED, true);
		var free = new TeamAssignmentService.PlayerCandidate(UUID.randomUUID(), "free", 200, "none", null, false);

		var result = service.assignTeams(List.of(locked, free));

		assertEquals(TeamId.RED, result.get(locked.playerId()));
	}
}
