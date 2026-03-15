package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TeamAssignmentService {
	private static final int RANDOM_TIE_THRESHOLD = 100;
	private final Random random;

	public TeamAssignmentService() {
		this(new Random());
	}

	TeamAssignmentService(Random random) {
		this.random = random;
	}

	public Map<UUID, TeamId> assignTeams(List<PlayerCandidate> players) {
		Map<UUID, TeamId> assignments = new HashMap<>();
		int maxTeamSize = (players.size() + 1) / 2;
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
			var redProjectedDiff = Math.abs((redLadder + player.ladder()) - blueLadder);
			var blueProjectedDiff = Math.abs(redLadder - (blueLadder + player.ladder()));
			boolean assignRed;
			if (redCount >= maxTeamSize) {
				assignRed = false;
			} else if (blueCount >= maxTeamSize) {
				assignRed = true;
			} else if (redCount > blueCount) {
				assignRed = false;
			} else if (blueCount > redCount) {
				assignRed = true;
			} else if (Math.abs(redProjectedDiff - blueProjectedDiff) <= RANDOM_TIE_THRESHOLD) {
				assignRed = random.nextBoolean();
			} else {
				assignRed = redProjectedDiff < blueProjectedDiff
					|| redProjectedDiff == blueProjectedDiff && redCount <= blueCount;
			}
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
