package karn.minecraftsnap.game;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class UnitSpawnQueueService {
	private final Map<TeamId, Deque<UUID>> queues = new EnumMap<>(TeamId.class);
	private final Set<UUID> suppressedPlayers = new HashSet<>();
	private final Random random;

	public UnitSpawnQueueService() {
		this(new Random());
	}

	UnitSpawnQueueService(Random random) {
		this.random = random;
	}

	public void resetForMatch(MatchManager matchManager) {
		clear();
		for (var teamId : TeamId.values()) {
			var eligible = new ArrayList<>(eligibleUnitIds(matchManager, teamId));
			java.util.Collections.shuffle(eligible, random);
			queues.put(teamId, new ArrayDeque<>(eligible));
		}
	}

	public void clear() {
		queues.clear();
		suppressedPlayers.clear();
	}

	public void removePlayer(UUID playerId) {
		if (playerId == null) {
			return;
		}
		for (var queue : queues.values()) {
			queue.remove(playerId);
		}
		suppressedPlayers.add(playerId);
	}

	public void enqueueReturnedUnit(MatchManager matchManager, TeamId teamId, UUID playerId) {
		if (matchManager == null || teamId == null || playerId == null || !isEligible(matchManager, teamId, playerId)) {
			return;
		}
		var queue = queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>());
		suppressedPlayers.remove(playerId);
		queue.remove(playerId);
		queue.addLast(playerId);
	}

	public void enqueueSpectatorUnit(MatchManager matchManager, TeamId teamId, UUID playerId) {
		if (matchManager == null || teamId == null || playerId == null) {
			return;
		}
		var state = matchManager.getPlayerState(playerId);
		if (state.getTeamId() != teamId || state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() != null) {
			return;
		}
		var queue = queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>());
		suppressedPlayers.remove(playerId);
		queue.remove(playerId);
		queue.addLast(playerId);
	}

	public UUID peekNextPlayer(MatchManager matchManager, TeamId teamId) {
		var queue = queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>());
		pruneInvalid(matchManager, teamId, queue);
		return queue.peekFirst();
	}

	public void consume(TeamId teamId, UUID playerId) {
		if (teamId == null || playerId == null) {
			return;
		}
		queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>()).remove(playerId);
	}

	public int queuePosition(MatchManager matchManager, TeamId teamId, UUID playerId) {
		if (teamId == null || playerId == null) {
			return 0;
		}
		var queue = queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>());
		pruneInvalid(matchManager, teamId, queue);
		int position = 1;
		for (var queuedId : queue) {
			if (playerId.equals(queuedId)) {
				return position;
			}
			position++;
		}
		return 0;
	}

	public int queueDisplayPosition(MatchManager matchManager, TeamId teamId, UUID playerId) {
		if (teamId == null || playerId == null) {
			return 0;
		}
		var queue = queues.computeIfAbsent(teamId, ignored -> new ArrayDeque<>());
		pruneDisplayInvalid(matchManager, teamId, queue);
		if (shouldDisplayWaitingUnit(matchManager, teamId, playerId) && !queue.contains(playerId)) {
			suppressedPlayers.remove(playerId);
			queue.addLast(playerId);
		}
		int position = 1;
		for (var queuedId : queue) {
			if (playerId.equals(queuedId)) {
				return position;
			}
			position++;
		}
		return 0;
	}

	private void pruneInvalid(MatchManager matchManager, TeamId teamId, Deque<UUID> queue) {
		if (matchManager == null || queue == null) {
			return;
		}
		queue.removeIf(playerId -> !isEligible(matchManager, teamId, playerId));
		var knownIds = new LinkedHashSet<>(queue);
		for (var eligibleId : eligibleUnitIds(matchManager, teamId)) {
			if (knownIds.add(eligibleId)) {
				queue.addLast(eligibleId);
			}
		}
	}

	private void pruneDisplayInvalid(MatchManager matchManager, TeamId teamId, Deque<UUID> queue) {
		if (matchManager == null || queue == null) {
			return;
		}
		queue.removeIf(playerId -> !isDisplayEligible(matchManager, teamId, playerId));
		var knownIds = new LinkedHashSet<>(queue);
		for (var eligibleId : eligibleDisplayUnitIds(matchManager, teamId)) {
			if (knownIds.add(eligibleId)) {
				queue.addLast(eligibleId);
			}
		}
	}

	private Set<UUID> eligibleUnitIds(MatchManager matchManager, TeamId teamId) {
		var result = new LinkedHashSet<UUID>();
		if (matchManager == null || teamId == null) {
			return result;
		}
		Collection<ServerPlayerEntity> onlinePlayers = matchManager.getOnlineTeamPlayers(teamId);
		if (!onlinePlayers.isEmpty()) {
			for (var player : onlinePlayers) {
				if (!suppressedPlayers.contains(player.getUuid()) && isEligible(matchManager, teamId, player.getUuid())) {
					result.add(player.getUuid());
				}
			}
			return result;
		}
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			var state = entry.getValue();
			if (!suppressedPlayers.contains(entry.getKey())
				&& state.getTeamId() == teamId
				&& state.getRoleType() == RoleType.UNIT
				&& state.getCurrentUnitId() == null) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private Set<UUID> eligibleDisplayUnitIds(MatchManager matchManager, TeamId teamId) {
		var result = new LinkedHashSet<UUID>();
		if (matchManager == null || teamId == null) {
			return result;
		}
		Collection<ServerPlayerEntity> onlinePlayers = matchManager.getOnlineTeamPlayers(teamId);
		for (var player : onlinePlayers) {
			if (!suppressedPlayers.contains(player.getUuid()) && isDisplayEligible(matchManager, teamId, player.getUuid())) {
				result.add(player.getUuid());
			}
		}
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			var state = entry.getValue();
			if (!suppressedPlayers.contains(entry.getKey())
				&& state.getTeamId() == teamId
				&& state.getRoleType() == RoleType.UNIT
				&& state.getCurrentUnitId() == null) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	private boolean isEligible(MatchManager matchManager, TeamId teamId, UUID playerId) {
		var state = matchManager.getPlayerState(playerId);
		if (state.getTeamId() != teamId || state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() != null) {
			return false;
		}
		var server = matchManager.getServer();
		if (server == null) {
			return true;
		}
		var player = server.getPlayerManager().getPlayer(playerId);
		return player != null && player.isSpectator();
	}

	private boolean isDisplayEligible(MatchManager matchManager, TeamId teamId, UUID playerId) {
		var state = matchManager.getPlayerState(playerId);
		if (state.getTeamId() != teamId || state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() != null) {
			return false;
		}
		var server = matchManager.getServer();
		if (server == null) {
			return true;
		}
		return server.getPlayerManager().getPlayer(playerId) != null;
	}

	private boolean shouldDisplayWaitingUnit(MatchManager matchManager, TeamId teamId, UUID playerId) {
		return matchManager != null
			&& !suppressedPlayers.contains(playerId)
			&& isDisplayEligible(matchManager, teamId, playerId);
	}
}
