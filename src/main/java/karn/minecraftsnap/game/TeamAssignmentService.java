package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamAssignmentService {
	public Map<UUID, TeamId> assignTeams(List<PlayerCandidate> players) {
		Map<UUID, TeamId> assignments = new HashMap<>();
		int redLadder = 0;
		int blueLadder = 0;
		int redCount = 0;
		int blueCount = 0;

		for (var player : players) {
			if (player.lockedTeam() == null) {
				continue;
			}

			assignments.put(player.playerId(), player.lockedTeam());
			if (player.lockedTeam() == TeamId.RED) {
				redLadder += player.ladder();
				redCount++;
			} else {
				blueLadder += player.ladder();
				blueCount++;
			}
		}

		var remaining = players.stream()
			.filter(player -> player.lockedTeam() == null)
			.sorted(Comparator.comparingInt(PlayerCandidate::ladder).reversed()
				.thenComparing(PlayerCandidate::playerName))
			.toList();

		for (var player : remaining) {
			boolean assignRed = redLadder < blueLadder
				|| redLadder == blueLadder && redCount <= blueCount;
			var teamId = assignRed ? TeamId.RED : TeamId.BLUE;
			assignments.put(player.playerId(), teamId);

			if (teamId == TeamId.RED) {
				redLadder += player.ladder();
				redCount++;
			} else {
				blueLadder += player.ladder();
				blueCount++;
			}
		}

		return assignments;
	}

	public record PlayerCandidate(
		UUID playerId,
		String playerName,
		int ladder,
		String preference,
		TeamId lockedTeam,
		boolean lockedCaptain
	) {
	}
}
