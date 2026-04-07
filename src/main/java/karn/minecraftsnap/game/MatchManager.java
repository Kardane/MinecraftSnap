package karn.minecraftsnap.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchManager {
	private final Map<UUID, PlayerMatchState> playerStates = new HashMap<>();
	private final Map<LaneId, Boolean> laneActiveStates = new EnumMap<>(LaneId.class);
	private final Map<LaneId, Boolean> laneRevealedStates = new EnumMap<>(LaneId.class);
	private final Map<LaneId, Boolean> laneRevealOverrides = new EnumMap<>(LaneId.class);
	private final Map<LaneId, String> laneAssignedBiomeIds = new EnumMap<>(LaneId.class);
	private final Map<TeamId, FactionId> factionSelections = new EnumMap<>(TeamId.class);
	private MatchPhase phase = MatchPhase.LOBBY;
	private MatchClock clock = new MatchClock(9 * 60);
	private int redScore;
	private int blueScore;
	private TeamId winnerTeam;
	private TeamId surrenderingTeam;
	private TeamId allPointsHeldTeam;
	private int allPointsHeldSeconds;
	private long serverTicks;
	private long phaseTicks;
	private MinecraftServer server;
	private final VanillaPlayerTeamService vanillaPlayerTeamService = new VanillaPlayerTeamService();

	public MatchManager() {
		for (var laneId : LaneId.values()) {
			laneActiveStates.put(laneId, false);
			laneRevealedStates.put(laneId, false);
		}
	}

	public void bindServer(MinecraftServer server) {
		this.server = server;
		if (server != null) {
			vanillaPlayerTeamService.ensureManagedTeams(server.getScoreboard());
		}
	}

	public MinecraftServer getServer() {
		return server;
	}

	public void applyGameDuration(int seconds) {
		if (phase != MatchPhase.GAME_RUNNING) {
			clock.reset(seconds);
		}
	}

	public void handleJoin(ServerPlayerEntity player) {
		getOrCreateState(player.getUuid());
		syncPlayerTeamFromScoreboard(player);
	}

	public void handleDisconnect(ServerPlayerEntity player) {
		getOrCreateState(player.getUuid());
	}

	public void tick() {
		serverTicks++;
		phaseTicks++;

		if (phase != MatchPhase.GAME_RUNNING) {
			return;
		}

		if (clock.tick()) {
			decideWinnerByScore();
			setPhase(MatchPhase.GAME_END);
		}
	}

	public void setPhase(MatchPhase phase) {
		this.phase = phase;
		this.phaseTicks = 0L;
		if (phase == MatchPhase.GAME_START) {
			redScore = 0;
			blueScore = 0;
			winnerTeam = null;
			surrenderingTeam = null;
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			clock.reset(clock.getTotalSeconds());
			hideAllLanes();
			fillMissingRoles();
			applySelectedFactionsToPlayers();
			playerStates.values().forEach(PlayerMatchState::resetMatchPerformance);
		} else if (phase == MatchPhase.GAME_RUNNING) {
			redScore = 0;
			blueScore = 0;
			winnerTeam = null;
			surrenderingTeam = null;
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			clock.reset(clock.getTotalSeconds());
			hideAllLanes();
			fillMissingRoles();
			applySelectedFactionsToPlayers();
			playerStates.values().forEach(PlayerMatchState::resetMatchPerformance);
			revealLane(LaneId.LANE_1);
		} else if (phase == MatchPhase.LOBBY) {
			redScore = 0;
			blueScore = 0;
			winnerTeam = null;
			surrenderingTeam = null;
			allPointsHeldTeam = null;
			allPointsHeldSeconds = 0;
			hideAllLanes();
			clearAssignedBiomes();
			factionSelections.clear();
			clearOnlineScoreboardTeams();
			playerStates.values().forEach(PlayerMatchState::clear);
		} else if (phase == MatchPhase.TEAM_SELECT) {
			surrenderingTeam = null;
			factionSelections.clear();
			hideAllLanes();
			clearAssignedBiomes();
			playerStates.values().forEach(state -> {
				state.setFactionId(null);
				state.resetMatchPerformance();
			});
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

	public int getRemainingTicks() {
		return clock.getRemainingTicks();
	}

	public void reduceRemainingSeconds(int seconds) {
		if ((phase != MatchPhase.GAME_RUNNING && phase != MatchPhase.GAME_START) || seconds <= 0) {
			return;
		}
		if (clock.reduceRemainingSeconds(seconds)) {
			decideWinnerByScore();
			setPhase(MatchPhase.GAME_END);
		}
	}

	public int getTotalSeconds() {
		return clock.getTotalSeconds();
	}

	public int getTotalTicks() {
		return clock.getTotalTicks();
	}

	public int getElapsedSeconds() {
		return clock.getElapsedSeconds();
	}

	public void adjustDurationTicks(int deltaTicks) {
		if ((phase != MatchPhase.GAME_RUNNING && phase != MatchPhase.GAME_START) || deltaTicks == 0) {
			return;
		}
		if (clock.adjustDurationTicks(deltaTicks)) {
			decideWinnerByScore();
			setPhase(MatchPhase.GAME_END);
		}
	}

	public long getServerTicks() {
		return serverTicks;
	}

	public long getPhaseTicks() {
		return phaseTicks;
	}

	public void addScore(TeamId teamId, int amount) {
		if (teamId == TeamId.RED) {
			redScore += amount;
		} else if (teamId == TeamId.BLUE) {
			blueScore += amount;
		}
	}

	public void setCaptain(TeamId teamId, ServerPlayerEntity player) {
		setCaptain(teamId, player == null ? null : player.getUuid());
	}

	public void setCaptain(TeamId teamId, UUID playerId) {
		if (teamId == null || playerId == null) {
			return;
		}
		playerStates.entrySet().stream()
			.filter(entry -> entry.getValue().getTeamId() == teamId)
			.filter(entry -> entry.getValue().getRoleType() == RoleType.CAPTAIN)
			.map(Map.Entry::getKey)
			.filter(existingCaptainId -> !existingCaptainId.equals(playerId))
			.toList()
			.forEach(existingCaptainId -> {
				setRole(existingCaptainId, teamId, RoleType.UNIT);
				getOrCreateState(existingCaptainId).setCurrentUnitId(null);
			});
		setRole(playerId, teamId, RoleType.CAPTAIN);
		getOrCreateState(playerId).setCurrentUnitId(null);
	}

	public void setRole(ServerPlayerEntity player, TeamId teamId, RoleType roleType) {
		if (player == null) {
			return;
		}
		if (teamId == null) {
			vanillaPlayerTeamService.clearPlayer(player);
		} else {
			vanillaPlayerTeamService.assignPlayer(player, teamId);
		}
		applyRoleState(player.getUuid(), teamId, roleType);
	}

	public void setRole(UUID playerId, TeamId teamId, RoleType roleType) {
		if (playerId == null) {
			return;
		}
		var player = onlinePlayer(playerId);
		if (player != null) {
			setRole(player, teamId, roleType);
			return;
		}
		applyRoleState(playerId, teamId, roleType);
	}

	public PlayerMatchState getPlayerState(UUID playerId) {
		return getOrCreateState(playerId);
	}

	public void syncPersistentState(UUID playerId, int emeralds, int goldIngots) {
		var state = getOrCreateState(playerId);
		state.setEmeralds(emeralds);
		state.setGoldIngots(goldIngots);
	}

	public Map<UUID, PlayerMatchState> getPlayerStatesSnapshot() {
		return new LinkedHashMap<>(playerStates);
	}

	public void activateLane(LaneId laneId) {
		laneActiveStates.put(laneId, true);
	}

	public void deactivateLane(LaneId laneId) {
		laneActiveStates.put(laneId, false);
	}

	public void revealLane(LaneId laneId) {
		laneRevealedStates.put(laneId, true);
		laneActiveStates.put(laneId, true);
	}

	public void hideLane(LaneId laneId) {
		laneRevealedStates.put(laneId, false);
		laneActiveStates.put(laneId, false);
	}

	public void setLaneRevealOverride(LaneId laneId, boolean revealed) {
		laneRevealOverrides.put(laneId, revealed);
		if (revealed) {
			revealLane(laneId);
		} else {
			hideLane(laneId);
		}
	}

	public boolean isLaneActive(LaneId laneId) {
		return laneActiveStates.getOrDefault(laneId, false);
	}

	public boolean isLaneRevealed(LaneId laneId) {
		return laneRevealedStates.getOrDefault(laneId, false);
	}

	public Boolean getLaneRevealOverride(LaneId laneId) {
		return laneRevealOverrides.get(laneId);
	}

	public void setAssignedBiomeId(LaneId laneId, String biomeId) {
		if (biomeId == null) {
			laneAssignedBiomeIds.remove(laneId);
			return;
		}
		laneAssignedBiomeIds.put(laneId, biomeId);
	}

	public String getAssignedBiomeId(LaneId laneId) {
		return laneAssignedBiomeIds.get(laneId);
	}

	public Map<LaneId, String> getAssignedBiomeIdsSnapshot() {
		return new EnumMap<>(laneAssignedBiomeIds);
	}

	public TeamId getWinnerTeam() {
		return winnerTeam;
	}

	public TeamId getSurrenderingTeam() {
		return surrenderingTeam;
	}

	public TeamId getAllPointsHeldTeam() {
		return allPointsHeldTeam;
	}

	public int getAllPointsHeldSeconds() {
		return allPointsHeldSeconds;
	}

	public void clearPlayerAssignments() {
		clearOnlineScoreboardTeams();
		playerStates.values().forEach(PlayerMatchState::clearTeamAssignment);
		factionSelections.clear();
	}

	public void syncOnlinePlayersFromScoreboard() {
		if (server == null) {
			return;
		}
		vanillaPlayerTeamService.ensureManagedTeams(server.getScoreboard());
		for (var player : server.getPlayerManager().getPlayerList()) {
			syncPlayerTeamFromScoreboard(player);
		}
	}

	public Map<TeamId, FactionId> getFactionSelectionsSnapshot() {
		return new EnumMap<>(factionSelections);
	}

	public void setFactionSelection(TeamId teamId, FactionId factionId) {
		factionSelections.put(teamId, factionId);
	}

	public FactionId getFactionSelection(TeamId teamId) {
		return factionSelections.get(teamId);
	}

	public UUID getCaptainId(TeamId teamId) {
		return playerStates.entrySet().stream()
			.filter(entry -> entry.getValue().getTeamId() == teamId && entry.getValue().getRoleType() == RoleType.CAPTAIN)
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	public Collection<UUID> getCaptainIds() {
		return playerStates.entrySet().stream()
			.filter(entry -> entry.getValue().getRoleType() == RoleType.CAPTAIN && entry.getValue().getTeamId() != null)
			.map(Map.Entry::getKey)
			.toList();
	}

	public TeamId getCaptainTeam(UUID captainId) {
		var state = getOrCreateState(captainId);
		return state.getRoleType() == RoleType.CAPTAIN ? state.getTeamId() : null;
	}

	public UUID getCaptainIdByFaction(FactionId factionId) {
		if (factionId == null) {
			return null;
		}
		for (var teamId : TeamId.values()) {
			if (factionId == factionSelections.get(teamId)) {
				return getCaptainId(teamId);
			}
		}
		return null;
	}

	public Collection<UUID> getCaptainIdsByFaction(FactionId factionId) {
		if (factionId == null) {
			return List.of();
		}
		return java.util.Arrays.stream(TeamId.values())
			.filter(teamId -> factionId == factionSelections.get(teamId))
			.map(this::getCaptainId)
			.filter(java.util.Objects::nonNull)
			.toList();
	}

	public boolean isFactionSelectionComplete() {
		return factionSelections.containsKey(TeamId.RED) && factionSelections.containsKey(TeamId.BLUE);
	}

	public boolean isPregameDamageBlocked() {
		return phase != MatchPhase.GAME_RUNNING;
	}

	public void enterGameStart() {
		setPhase(MatchPhase.GAME_START);
	}

	public void startGameRunning() {
		setPhase(MatchPhase.GAME_RUNNING);
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

	public Collection<ServerPlayerEntity> getOnlinePlayers() {
		if (server == null) {
			return List.of();
		}

		return List.copyOf(server.getPlayerManager().getPlayerList());
	}

	public Collection<ServerPlayerEntity> getOnlineTeamPlayers(TeamId teamId) {
		if (server == null) {
			return List.of();
		}

		return server.getPlayerManager().getPlayerList().stream()
			.filter(player -> getOrCreateState(player.getUuid()).getTeamId() == teamId)
			.toList();
	}

	public Collection<ServerPlayerEntity> getOnlineSpectatorUnits(TeamId teamId) {
		if (server == null) {
			return List.of();
		}

		return server.getPlayerManager().getPlayerList().stream()
			.filter(player -> {
				var state = getOrCreateState(player.getUuid());
				return state.getTeamId() == teamId
					&& state.getRoleType() == RoleType.UNIT
					&& player.isSpectator();
			})
			.toList();
	}

	public void setCurrentUnit(UUID playerId, String unitId) {
		var state = getOrCreateState(playerId);
		state.setCurrentUnitId(unitId);
		state.resetAdvanceState();
	}

	public void clearCurrentUnit(UUID playerId) {
		var state = getOrCreateState(playerId);
		state.setCurrentUnitId(null);
		state.resetAdvanceState();
	}

	private PlayerMatchState getOrCreateState(UUID playerId) {
		return playerStates.computeIfAbsent(playerId, ignored -> new PlayerMatchState());
	}

	private void syncPlayerTeamFromScoreboard(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		var teamId = vanillaPlayerTeamService.resolveTeam(player);
		var state = getOrCreateState(player.getUuid());
		if (teamId == null) {
			state.clearTeamAssignment();
			return;
		}
		VanillaPlayerTeamService.syncState(state, teamId);
		if (state.getFactionId() == null) {
			state.setFactionId(factionSelections.get(teamId));
		}
	}

	private void applyRoleState(UUID playerId, TeamId teamId, RoleType roleType) {
		var state = getOrCreateState(playerId);
		if (teamId == null || roleType == null || roleType == RoleType.NONE) {
			state.clearTeamAssignment();
			return;
		}
		VanillaPlayerTeamService.syncState(state, teamId);
		state.setTeam(teamId, roleType);
		state.setFactionId(factionSelections.get(teamId));
		if (roleType != RoleType.UNIT) {
			state.setCurrentUnitId(null);
		}
	}

	private ServerPlayerEntity onlinePlayer(UUID playerId) {
		return server == null || playerId == null ? null : server.getPlayerManager().getPlayer(playerId);
	}

	private void clearOnlineScoreboardTeams() {
		if (server == null) {
			return;
		}
		vanillaPlayerTeamService.clearAllOnlinePlayers(server);
	}

	private void deactivateAllLanes() {
		for (var laneId : LaneId.values()) {
			laneActiveStates.put(laneId, false);
		}
	}

	private void hideAllLanes() {
		laneRevealOverrides.clear();
		for (var laneId : LaneId.values()) {
			laneActiveStates.put(laneId, false);
			laneRevealedStates.put(laneId, false);
		}
	}

	private void clearAssignedBiomes() {
		laneAssignedBiomeIds.clear();
	}

	private void fillMissingRoles() {
		if (server == null) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = getOrCreateState(player.getUuid());
			if (state.getTeamId() == null) {
				var targetTeam = countUnits(TeamId.RED) <= countUnits(TeamId.BLUE) ? TeamId.RED : TeamId.BLUE;
				state.setTeam(targetTeam, RoleType.UNIT);
				continue;
			}

			if (state.getRoleType() != RoleType.NONE) {
				continue;
			}

			state.setTeam(state.getTeamId(), RoleType.UNIT);
		}
	}

	private void applySelectedFactionsToPlayers() {
		for (var state : playerStates.values()) {
			if (state.getTeamId() == null) {
				state.setFactionId(null);
				continue;
			}

			state.setFactionId(factionSelections.get(state.getTeamId()));
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

	public void declareSurrender(TeamId surrenderingTeam) {
		if (surrenderingTeam == null) {
			return;
		}
		this.surrenderingTeam = surrenderingTeam;
		winnerTeam = surrenderingTeam == TeamId.RED ? TeamId.BLUE : TeamId.RED;
		setPhase(MatchPhase.GAME_END);
	}
}
