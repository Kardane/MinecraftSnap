package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CaptainSelectionService {
	public Map<TeamId, UUID> selectCaptains(List<TeamAssignmentService.PlayerCandidate> candidates, Map<UUID, TeamId> assignments) {
		Map<TeamId, UUID> captains = new EnumMap<>(TeamId.class);

		for (var candidate : candidates) {
			if (!candidate.lockedCaptain() || candidate.lockedTeam() == null) {
				continue;
			}

			captains.put(candidate.lockedTeam(), candidate.playerId());
		}

		var comparator = Comparator
			.comparingInt((TeamAssignmentService.PlayerCandidate candidate) -> preferenceRank(candidate.preference()))
			.thenComparing(Comparator.comparingInt(TeamAssignmentService.PlayerCandidate::ladder).reversed())
			.thenComparing(TeamAssignmentService.PlayerCandidate::playerName);

		for (var teamId : TeamId.values()) {
			if (captains.containsKey(teamId)) {
				continue;
			}

			candidates.stream()
				.filter(candidate -> assignments.get(candidate.playerId()) == teamId)
				.min(comparator)
				.ifPresent(candidate -> captains.put(teamId, candidate.playerId()));
		}

		return captains;
	}

	private int preferenceRank(String preference) {
		if ("captain".equalsIgnoreCase(preference)) {
			return 0;
		}
		if ("unit".equalsIgnoreCase(preference)) {
			return 2;
		}
		return 1;
	}
}
