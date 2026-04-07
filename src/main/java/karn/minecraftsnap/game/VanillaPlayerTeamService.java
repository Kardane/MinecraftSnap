package karn.minecraftsnap.game;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class VanillaPlayerTeamService {
	public static final String RED_TEAM_NAME = "mcsnap_red";
	public static final String BLUE_TEAM_NAME = "mcsnap_blue";

	public void ensureManagedTeams(Scoreboard scoreboard) {
		if (scoreboard == null) {
			return;
		}
		configureTeam(scoreboard, RED_TEAM_NAME, Text.literal("레드"), Formatting.RED);
		configureTeam(scoreboard, BLUE_TEAM_NAME, Text.literal("블루"), Formatting.BLUE);
	}

	public void assignPlayer(ServerPlayerEntity player, TeamId teamId) {
		if (player == null) {
			return;
		}
		assignScoreHolder(player.getServer().getScoreboard(), player.getNameForScoreboard(), teamId);
	}

	public void assignScoreHolder(Scoreboard scoreboard, String scoreHolder, TeamId teamId) {
		if (scoreboard == null || scoreHolder == null || scoreHolder.isBlank() || teamId == null) {
			return;
		}
		ensureManagedTeams(scoreboard);
		clearScoreHolder(scoreboard, scoreHolder);
		var team = scoreboard.getTeam(teamName(teamId));
		if (team != null) {
			scoreboard.addScoreHolderToTeam(scoreHolder, team);
		}
	}

	public void clearPlayer(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		clearScoreHolder(player.getServer().getScoreboard(), player.getNameForScoreboard());
	}

	public void clearAllOnlinePlayers(MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			clearPlayer(player);
		}
	}

	public void clearScoreHolder(Scoreboard scoreboard, String scoreHolder) {
		if (scoreboard == null || scoreHolder == null || scoreHolder.isBlank()) {
			return;
		}
		var currentTeam = scoreboard.getScoreHolderTeam(scoreHolder);
		if (currentTeam != null) {
			scoreboard.removeScoreHolderFromTeam(scoreHolder, currentTeam);
		}
	}

	public TeamId resolveTeam(ServerPlayerEntity player) {
		if (player == null) {
			return null;
		}
		return resolveTeam(player.getServer().getScoreboard(), player.getNameForScoreboard());
	}

	public TeamId resolveTeam(Scoreboard scoreboard, String scoreHolder) {
		if (scoreboard == null || scoreHolder == null || scoreHolder.isBlank()) {
			return null;
		}
		var team = scoreboard.getScoreHolderTeam(scoreHolder);
		if (team == null) {
			return null;
		}
		return teamId(team.getName());
	}

	public static void syncState(PlayerMatchState state, TeamId teamId) {
		if (state == null) {
			return;
		}
		if (teamId == null) {
			state.clearTeamAssignment();
			return;
		}
		if (state.getTeamId() != teamId) {
			state.setCurrentUnitId(null);
			state.setFactionId(null);
		}
		state.setTeam(teamId, state.getRoleType());
	}

	private void configureTeam(Scoreboard scoreboard, String name, Text displayName, Formatting color) {
		var team = scoreboard.getTeam(name);
		if (team == null) {
			team = scoreboard.addTeam(name);
		}
		team.setDisplayName(displayName);
		team.setColor(color);
	}

	private String teamName(TeamId teamId) {
		return teamId == TeamId.BLUE ? BLUE_TEAM_NAME : RED_TEAM_NAME;
	}

	private TeamId teamId(String teamName) {
		if (RED_TEAM_NAME.equals(teamName)) {
			return TeamId.RED;
		}
		if (BLUE_TEAM_NAME.equals(teamName)) {
			return TeamId.BLUE;
		}
		return null;
	}
}
