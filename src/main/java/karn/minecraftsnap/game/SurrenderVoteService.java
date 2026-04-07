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
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Consumer<String> broadcastSink;
	private final Map<TeamId, Set<UUID>> votesByTeam = new EnumMap<>(TeamId.class);

	public SurrenderVoteService(MatchManager matchManager, TextTemplateResolver textTemplateResolver, Consumer<String> broadcastSink) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.broadcastSink = broadcastSink;
	}

	public VoteResult vote(Voter voter) {
		if (voter == null || voter.teamId() == null || matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return new VoteResult(false, false, 0, 0);
		}

		var eligible = eligiblePlayerIds(voter.teamId());
		var currentVotes = votesByTeam.computeIfAbsent(voter.teamId(), ignored -> new HashSet<>());
		currentVotes.retainAll(eligible);
		var requiredVotes = requiredVotes(eligible.size());
		var added = currentVotes.add(voter.playerId());
		var result = new VoteResult(!added, currentVotes.size() >= requiredVotes, currentVotes.size(), requiredVotes);
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
		votesByTeam.values().forEach(voters -> voters.remove(playerId));
	}

	public int currentVotes(TeamId teamId) {
		return votesByTeam.getOrDefault(teamId, Set.of()).size();
	}

	public int requiredVotes(int eligibleCount) {
		if (eligibleCount <= 0) {
			return 1;
		}
		return (eligibleCount * 2 + 2) / 3;
	}

	private Set<UUID> eligiblePlayerIds(TeamId teamId) {
		Collection<ServerPlayerEntity> onlinePlayers = matchManager.getOnlineTeamPlayers(teamId);
		if (!onlinePlayers.isEmpty()) {
			var ids = new HashSet<UUID>();
			for (var player : onlinePlayers) {
				ids.add(player.getUuid());
			}
			return ids;
		}

		var ids = new HashSet<UUID>();
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			if (entry.getValue().getTeamId() == teamId && entry.getValue().getRoleType() != RoleType.NONE) {
				ids.add(entry.getKey());
			}
		}
		return ids;
	}

	public record Voter(UUID playerId, TeamId teamId, String playerName) {
	}

	public record VoteResult(boolean duplicate, boolean completed, int currentVotes, int requiredVotes) {
	}
}
