package karn.minecraftsnap.game;

import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class SurrenderVoteService {
	private static final int EARLY_SURRENDER_LIMIT_SECONDS = 60;
	private static final int VOTE_TIMEOUT_SECONDS = 30;

	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Consumer<String> broadcastSink;
	private final Map<TeamId, ActiveVote> votesByTeam = new EnumMap<>(TeamId.class);

	public SurrenderVoteService(MatchManager matchManager, TextTemplateResolver textTemplateResolver, Consumer<String> broadcastSink) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.broadcastSink = broadcastSink;
	}

	public VoteResult vote(Voter voter) {
		if (voter == null || voter.teamId() == null || matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return new VoteResult(false, false, false, 0, 0, 0, 0);
		}

		var eligible = eligiblePlayers(voter.teamId());
		var currentElapsedSeconds = matchManager.getElapsedSeconds();
		var vote = votesByTeam.get(voter.teamId());
		if (vote != null && currentElapsedSeconds - vote.startedAtSeconds() >= VOTE_TIMEOUT_SECONDS) {
			votesByTeam.remove(voter.teamId());
			vote = null;
		}
		if (vote == null) {
			vote = new ActiveVote(currentElapsedSeconds, currentElapsedSeconds <= EARLY_SURRENDER_LIMIT_SECONDS, new HashSet<>());
			votesByTeam.put(voter.teamId(), vote);
		}

		vote.voterIds().retainAll(eligible.keySet());
		var added = vote.voterIds().add(voter.playerId());
		var requiredVotes = requiredVotes(eligible, vote.earlySurrender());
		var unitVoteCount = countUnitVotes(vote.voterIds(), eligible);
		var totalUnitCount = countUnits(eligible);
		var result = new VoteResult(
			!added,
			hasPassed(vote.voterIds(), eligible, vote.earlySurrender()),
			vote.earlySurrender(),
			vote.voterIds().size(),
			requiredVotes,
			unitVoteCount,
			totalUnitCount
		);
		if (broadcastSink != null) {
			broadcastSink.accept(result.currentVotes() + "/" + result.requiredVotes());
		}
		if (result.completed()) {
			matchManager.declareSurrender(voter.teamId());
			clear();
		}
		return result;
	}

	public void clear() {
		votesByTeam.clear();
	}

	public void clearPlayer(UUID playerId) {
		if (playerId == null) {
			return;
		}
		votesByTeam.values().forEach(vote -> vote.voterIds().remove(playerId));
	}

	public int currentVotes(TeamId teamId) {
		var vote = votesByTeam.get(teamId);
		return vote == null ? 0 : vote.voterIds().size();
	}

	public int requiredVotes(Map<UUID, RoleType> eligiblePlayers, boolean earlySurrender) {
		int eligibleCount = eligiblePlayers.size();
		if (eligibleCount <= 0) {
			return 1;
		}
		if (earlySurrender) {
			return eligibleCount;
		}
		return eligibleCount / 2 + 1;
	}

	private boolean hasPassed(Set<UUID> votes, Map<UUID, RoleType> eligiblePlayers, boolean earlySurrender) {
		if (earlySurrender) {
			return votes.size() >= requiredVotes(eligiblePlayers, true);
		}
		if (votes.size() >= requiredVotes(eligiblePlayers, false)) {
			return true;
		}
		int totalUnitCount = countUnits(eligiblePlayers);
		return totalUnitCount > 0 && countUnitVotes(votes, eligiblePlayers) >= totalUnitCount;
	}

	private int countUnits(Map<UUID, RoleType> eligiblePlayers) {
		int count = 0;
		for (var roleType : eligiblePlayers.values()) {
			if (roleType == RoleType.UNIT) {
				count++;
			}
		}
		return count;
	}

	private int countUnitVotes(Set<UUID> votes, Map<UUID, RoleType> eligiblePlayers) {
		int count = 0;
		for (var voterId : votes) {
			if (eligiblePlayers.get(voterId) == RoleType.UNIT) {
				count++;
			}
		}
		return count;
	}

	private Map<UUID, RoleType> eligiblePlayers(TeamId teamId) {
		Collection<ServerPlayerEntity> onlinePlayers = matchManager.getOnlineTeamPlayers(teamId);
		if (!onlinePlayers.isEmpty()) {
			var players = new java.util.LinkedHashMap<UUID, RoleType>();
			for (var player : onlinePlayers) {
				var state = matchManager.getPlayerState(player.getUuid());
				if (state.getRoleType() != RoleType.NONE && state.getRoleType() != RoleType.SPECTATOR) {
					players.put(player.getUuid(), state.getRoleType());
				}
			}
			return players;
		}

		var players = new java.util.LinkedHashMap<UUID, RoleType>();
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			var state = entry.getValue();
			if (state.getTeamId() == teamId && state.getRoleType() != RoleType.NONE && state.getRoleType() != RoleType.SPECTATOR) {
				players.put(entry.getKey(), state.getRoleType());
			}
		}
		return players;
	}

	private record ActiveVote(int startedAtSeconds, boolean earlySurrender, Set<UUID> voterIds) {
	}

	public record Voter(UUID playerId, TeamId teamId, String playerName) {
	}

	public record VoteResult(
		boolean duplicate,
		boolean completed,
		boolean earlySurrender,
		int currentVotes,
		int requiredVotes,
		int unitVotes,
		int totalUnits
	) {
	}
}
