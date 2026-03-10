package karn.minecraftsnap.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchManager {
	private final Map<UUID, PlayerMatchState> playerStates = new HashMap<>();
	private MatchPhase phase = MatchPhase.LOBBY;
	private MatchClock clock = new MatchClock(9 * 60);
	private int redScore;
	private int blueScore;
	private long serverTicks;
	private MinecraftServer server;

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
			setPhase(MatchPhase.GAME_END);
		}
	}

	public void setPhase(MatchPhase phase) {
		this.phase = phase;
		if (phase == MatchPhase.GAME_RUNNING) {
			redScore = 0;
			blueScore = 0;
			clock.reset(clock.getTotalSeconds());
		} else if (phase == MatchPhase.LOBBY) {
			redScore = 0;
			blueScore = 0;
			playerStates.values().forEach(PlayerMatchState::clear);
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

	public void setCaptain(TeamId teamId, ServerPlayerEntity player) {
		getOrCreateState(player.getUuid()).setTeam(teamId, RoleType.CAPTAIN);
	}

	public void setRole(ServerPlayerEntity player, TeamId teamId, RoleType roleType) {
		getOrCreateState(player.getUuid()).setTeam(teamId, roleType);
	}

	public PlayerMatchState getPlayerState(UUID playerId) {
		return getOrCreateState(playerId);
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
}
