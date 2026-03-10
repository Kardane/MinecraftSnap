package karn.minecraftsnap.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchManager {
	private final Map<UUID, PlayerMatchState> playerStates = new HashMap<>();
	private final Map<LaneId, Boolean> laneActiveStates = new EnumMap<>(LaneId.class);
	private MatchPhase phase = MatchPhase.LOBBY;
	private MatchClock clock = new MatchClock(9 * 60);
	private int redScore;
	private int blueScore;
	private TeamId winnerTeam;
	private TeamId allPointsHeldTeam;
	private int allPointsHeldSeconds;
	private long serverTicks;
	private MinecraftServer server;

	public MatchManager() {
		for (var laneId : LaneId.values()) {
			laneActiveStates.put(laneId, false);
		}
	}

	public void bindServer(MinecraftServer server) {
		this.server = server;
	}

	public void applyGameDuration(int seconds) {
		if (phase != MatchPhase.GAME_RUNNING) {
			clock.reset(seconds);
		}
	}

	public void handleJoin(ServerPlayerEntity player) {
		getOrCreateState(player.getUuid());
	}

	public void handleDisconnect(ServerPlayerEntity player) {
		getOrCreateState(player.getUuid());
	}

	public void tick() {
		serverTicks++;

		if (phase != MatchPhase.GAME_RUNNING || serverTicks % 20L != 0L) {
			return;
		}

		if (clock.tickSecond()) {
			decideWinnerByScore();
			setPhase(MatchPhase.GAME_END);
		}
	}

	public void setPhase(MatchPhase phase) {
		this.phase = phase;
		if (phase == MatchPhase.GAME_RUNNING) {
			redScore = 0;
			blueScore = 0;
			winnerTeam = null;
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			clock.reset(clock.getTotalSeconds());
			deactivateAllLanes();
			activateLane(LaneId.LANE_1);
			assignUnassignedPlayersAsUnits();
		} else if (phase == MatchPhase.LOBBY) {
			redScore = 0;
			blueScore = 0;
			winnerTeam = null;
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			deactivateAllLanes();
			playerStates.values().forEach(PlayerMatchState::clear);
		} else if (phase == MatchPhase.GAME_END) {
			deactivateAllLanes();
		}
	}

	public MatchPhase getPhase() {
		return phase;
	}

	public int getRedScore() {
		return redScore;
	}

	public int getBlueScore() {
		return blueScore;
	}

	public int getRemainingSeconds() {
		return clock.getRemainingSeconds();
	}

	public int getTotalSeconds() {
		return clock.getTotalSeconds();
	}

	public long getServerTicks() {
		return serverTicks;
	}

	public void addScore(TeamId teamId, int amount) {
		if (teamId == TeamId.RED) {
			redScore += amount;
		} else if (teamId == TeamId.BLUE) {
			blueScore += amount;
		}
	}

	public void setCaptain(TeamId teamId, ServerPlayerEntity player) {
		getOrCreateState(player.getUuid()).setTeam(teamId, RoleType.CAPTAIN);
	}

	public void setRole(ServerPlayerEntity player, TeamId teamId, RoleType roleType) {
		getOrCreateState(player.getUuid()).setTeam(teamId, roleType);
	}

	public PlayerMatchState getPlayerState(UUID playerId) {
		return getOrCreateState(playerId);
	}

	public void activateLane(LaneId laneId) {
		laneActiveStates.put(laneId, true);
	}

	public void deactivateLane(LaneId laneId) {
		laneActiveStates.put(laneId, false);
	}

	public boolean isLaneActive(LaneId laneId) {
		return laneActiveStates.getOrDefault(laneId, false);
	}

	public TeamId getWinnerTeam() {
		return winnerTeam;
	}

	public void recordAllPointsHeld(TeamId teamId, int requiredSeconds) {
		if (teamId == null) {
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			return;
		}

		if (allPointsHeldTeam != teamId) {
			allPointsHeldTeam = teamId;
			allPointsHeldSeconds = 1;
		} else {
			allPointsHeldSeconds++;
		}

		if (allPointsHeldSeconds >= requiredSeconds) {
			winnerTeam = teamId;
			setPhase(MatchPhase.GAME_END);
		}
	}

	public Collection<ServerPlayerEntity> getOnlineTeamMembers(TeamId teamId) {
		if (server == null) {
			return List.of();
		}

		return server.getPlayerManager().getPlayerList().stream()
			.filter(player -> {
				var state = playerStates.get(player.getUuid());
				return state != null && state.getTeamId() == teamId && state.canUseTeamChat();
			})
			.toList();
	}

	private PlayerMatchState getOrCreateState(UUID playerId) {
		return playerStates.computeIfAbsent(playerId, ignored -> new PlayerMatchState());
	}

	private void deactivateAllLanes() {
		for (var laneId : LaneId.values()) {
			laneActiveStates.put(laneId, false);
		}
	}

	private void assignUnassignedPlayersAsUnits() {
		if (server == null) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = getOrCreateState(player.getUuid());
			if (state.getRoleType() != RoleType.NONE) {
				continue;
			}

			var targetTeam = countUnits(TeamId.RED) <= countUnits(TeamId.BLUE) ? TeamId.RED : TeamId.BLUE;
			state.setTeam(targetTeam, RoleType.UNIT);
		}
	}

	private long countUnits(TeamId teamId) {
		return playerStates.values().stream()
			.filter(state -> state.getTeamId() == teamId && state.getRoleType() == RoleType.UNIT)
			.count();
	}

	private void decideWinnerByScore() {
		if (redScore > blueScore) {
			winnerTeam = TeamId.RED;
		} else if (blueScore > redScore) {
			winnerTeam = TeamId.BLUE;
		} else {
			winnerTeam = null;
		}
	}
}
