package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CaptainSelectionService {
	private static final int RANDOM_TIE_THRESHOLD = 100;
	private final Random random;

	public CaptainSelectionService() {
		this(new Random());
	}

	CaptainSelectionService(Random random) {
		this.random = random;
	}

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

			var teamCandidates = candidates.stream()
				.filter(candidate -> assignments.get(candidate.playerId()) == teamId)
				.sorted(comparator)
				.toList();
			selectCaptain(teamCandidates).ifPresent(candidate -> captains.put(teamId, candidate.playerId()));
		}

		return captains;
	}

	private java.util.Optional<TeamAssignmentService.PlayerCandidate> selectCaptain(List<TeamAssignmentService.PlayerCandidate> teamCandidates) {
		if (teamCandidates.isEmpty()) {
			return java.util.Optional.empty();
		}
		var best = teamCandidates.getFirst();
		var eligible = teamCandidates.stream()
			.filter(candidate -> preferenceRank(candidate.preference()) == preferenceRank(best.preference()))
			.filter(candidate -> Math.abs(best.ladder() - candidate.ladder()) <= RANDOM_TIE_THRESHOLD)
			.toList();
		if (eligible.size() <= 1) {
			return java.util.Optional.of(best);
		}
		return java.util.Optional.of(eligible.get(random.nextInt(eligible.size())));
	}

	private int preferenceRank(String preference) {
		if ("captain".equalsIgnoreCase(preference)) {
			return 0;
		}
		return 1;
	}
}
