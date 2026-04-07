package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CaptainSelectionService {
	private static final int RANDOM_TIE_THRESHOLD = 100;
	private static final int CAPTAIN_LADDER_GAP_LIMIT = 100;
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

		var comparator = comparator();
		if (!captains.containsKey(TeamId.RED) && !captains.containsKey(TeamId.BLUE)) {
			selectBalancedAutoCaptains(candidates, assignments, captains, comparator);
		}

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

	private void selectBalancedAutoCaptains(
		List<TeamAssignmentService.PlayerCandidate> candidates,
		Map<UUID, TeamId> assignments,
		Map<TeamId, UUID> captains,
		Comparator<TeamAssignmentService.PlayerCandidate> comparator
	) {
		var redCandidates = teamCandidates(candidates, assignments, TeamId.RED, comparator);
		var blueCandidates = teamCandidates(candidates, assignments, TeamId.BLUE, comparator);
		var pair = selectBalancedPair(redCandidates, blueCandidates);
		if (pair == null) {
			return;
		}
		captains.put(TeamId.RED, pair.red().playerId());
		captains.put(TeamId.BLUE, pair.blue().playerId());
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

	private Comparator<TeamAssignmentService.PlayerCandidate> comparator() {
		return Comparator
			.comparingInt((TeamAssignmentService.PlayerCandidate candidate) -> preferenceRank(candidate.preference()))
			.thenComparing(Comparator.comparingInt(TeamAssignmentService.PlayerCandidate::ladder).reversed())
			.thenComparing(TeamAssignmentService.PlayerCandidate::playerName);
	}

	private List<TeamAssignmentService.PlayerCandidate> teamCandidates(
		List<TeamAssignmentService.PlayerCandidate> candidates,
		Map<UUID, TeamId> assignments,
		TeamId teamId,
		Comparator<TeamAssignmentService.PlayerCandidate> comparator
	) {
		return candidates.stream()
			.filter(candidate -> assignments.get(candidate.playerId()) == teamId)
			.sorted(comparator)
			.toList();
	}

	private CaptainPair selectBalancedPair(
		List<TeamAssignmentService.PlayerCandidate> redCandidates,
		List<TeamAssignmentService.PlayerCandidate> blueCandidates
	) {
		if (redCandidates.isEmpty() || blueCandidates.isEmpty()) {
			return null;
		}
		int redBestPreference = preferenceRank(redCandidates.getFirst().preference());
		int blueBestPreference = preferenceRank(blueCandidates.getFirst().preference());
		CaptainPair bestPair = null;
		int bestGap = Integer.MAX_VALUE;
		int bestCombinedLadder = Integer.MIN_VALUE;
		for (var red : redCandidates) {
			if (preferenceRank(red.preference()) != redBestPreference) {
				break;
			}
			for (var blue : blueCandidates) {
				if (preferenceRank(blue.preference()) != blueBestPreference) {
					break;
				}
				var gap = Math.abs(red.ladder() - blue.ladder());
				if (gap > CAPTAIN_LADDER_GAP_LIMIT) {
					continue;
				}
				var combinedLadder = red.ladder() + blue.ladder();
				if (gap < bestGap || (gap == bestGap && combinedLadder > bestCombinedLadder)) {
					bestPair = new CaptainPair(red, blue);
					bestGap = gap;
					bestCombinedLadder = combinedLadder;
				}
			}
		}
		return bestPair;
	}

	private int preferenceRank(String preference) {
		if ("captain".equalsIgnoreCase(preference)) {
			return 0;
		}
		if ("avoid_captain".equalsIgnoreCase(preference)) {
			return 2;
		}
		return 1;
	}

	private record CaptainPair(
		TeamAssignmentService.PlayerCandidate red,
		TeamAssignmentService.PlayerCandidate blue
	) {
	}
}
