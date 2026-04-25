package karn.minecraftsnap.game;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CaptainSelectionService {
	private static final int RANDOM_TIE_THRESHOLD = 10;
	private static final int MIN_TOTAL_GAMES_FOR_AUTO_CAPTAIN = 6;
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
			selectCaptain(eligibleCaptainCandidates(teamCandidates)).ifPresent(candidate -> captains.put(teamId, candidate.playerId()));
		}

		return captains;
	}

	private void selectBalancedAutoCaptains(
		List<TeamAssignmentService.PlayerCandidate> candidates,
		Map<UUID, TeamId> assignments,
		Map<TeamId, UUID> captains,
		Comparator<TeamAssignmentService.PlayerCandidate> comparator
	) {
		var redCandidates = eligibleCaptainCandidates(teamCandidates(candidates, assignments, TeamId.RED, comparator));
		var blueCandidates = eligibleCaptainCandidates(teamCandidates(candidates, assignments, TeamId.BLUE, comparator));
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
			.filter(candidate -> captainPriorityRank(candidate) == captainPriorityRank(best))
			.filter(candidate -> Math.abs(best.totalGames() - candidate.totalGames()) <= RANDOM_TIE_THRESHOLD)
			.toList();
		if (eligible.size() <= 1) {
			return java.util.Optional.of(best);
		}
		return java.util.Optional.of(eligible.get(random.nextInt(eligible.size())));
	}

	private Comparator<TeamAssignmentService.PlayerCandidate> comparator() {
		return Comparator
			.comparingInt(this::captainPriorityRank)
			.thenComparing(Comparator.comparingInt(TeamAssignmentService.PlayerCandidate::totalGames).reversed())
			.thenComparing(TeamAssignmentService.PlayerCandidate::playerName);
	}

	private List<TeamAssignmentService.PlayerCandidate> eligibleCaptainCandidates(List<TeamAssignmentService.PlayerCandidate> teamCandidates) {
		var eligible = teamCandidates.stream()
			.filter(this::isEligibleAutoCaptain)
			.toList();
		return eligible.isEmpty() ? teamCandidates : eligible;
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
		CaptainPair bestPair = null;
		int bestPrioritySum = Integer.MAX_VALUE;
		int bestWorstPriority = Integer.MAX_VALUE;
		int bestGap = Integer.MAX_VALUE;
		int bestCombinedGames = Integer.MIN_VALUE;
		for (var red : redCandidates) {
			var redPriority = captainPriorityRank(red);
			for (var blue : blueCandidates) {
				var bluePriority = captainPriorityRank(blue);
				var gap = Math.abs(red.totalGames() - blue.totalGames());
				var prioritySum = redPriority + bluePriority;
				var worstPriority = Math.max(redPriority, bluePriority);
				var combinedGames = red.totalGames() + blue.totalGames();
				if (prioritySum < bestPrioritySum
					|| (prioritySum == bestPrioritySum && worstPriority < bestWorstPriority)
					|| (prioritySum == bestPrioritySum && worstPriority == bestWorstPriority && gap < bestGap)
					|| (prioritySum == bestPrioritySum && worstPriority == bestWorstPriority && gap == bestGap && combinedGames > bestCombinedGames)) {
					bestPair = new CaptainPair(red, blue);
					bestPrioritySum = prioritySum;
					bestWorstPriority = worstPriority;
					bestGap = gap;
					bestCombinedGames = combinedGames;
				}
			}
		}
		return bestPair;
	}

	private boolean isEligibleAutoCaptain(TeamAssignmentService.PlayerCandidate candidate) {
		return candidate != null && candidate.totalGames() >= MIN_TOTAL_GAMES_FOR_AUTO_CAPTAIN;
	}

	private int captainPriorityRank(TeamAssignmentService.PlayerCandidate candidate) {
		if (candidate == null) {
			return Integer.MAX_VALUE;
		}
		if ("avoid_captain".equalsIgnoreCase(candidate.preference())) {
			return 4;
		}
		if ("captain".equalsIgnoreCase(candidate.preference()) && !candidate.recentCaptain()) {
			return 0;
		}
		if (!candidate.recentCaptain()) {
			return "captain".equalsIgnoreCase(candidate.preference()) ? 0 : 1;
		}
		if ("captain".equalsIgnoreCase(candidate.preference())) {
			return 2;
		}
		return 3;
	}

	private record CaptainPair(
		TeamAssignmentService.PlayerCandidate red,
		TeamAssignmentService.PlayerCandidate blue
	) {
	}
}
