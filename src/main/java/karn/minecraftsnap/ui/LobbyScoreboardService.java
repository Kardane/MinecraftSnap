package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;

public class LobbyScoreboardService {
	private static final String OBJECTIVE_NAME = "minecraftsnap_lobby";
	private static final String[] LINE_HOLDERS = {
		"mcsnap_line_0",
		"mcsnap_line_1",
		"mcsnap_line_2",
		"mcsnap_line_3",
		"mcsnap_line_4",
		"mcsnap_line_5",
		"mcsnap_line_6"
	};

	private final TextTemplateResolver textTemplateResolver;

	public LobbyScoreboardService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void sync(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository) {
		var scoreboard = server.getScoreboard();
		syncPlayerTeams(scoreboard, matchManager, statsRepository, server);

		if (matchManager.getPhase() == MatchPhase.GAME_RUNNING || matchManager.getPhase() == MatchPhase.GAME_END) {
			scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
			return;
		}

		var objective = scoreboard.getNullableObjective(OBJECTIVE_NAME);
		if (objective == null) {
			objective = scoreboard.addObjective(
				OBJECTIVE_NAME,
				ScoreboardCriterion.DUMMY,
				textTemplateResolver.format("&6MCsnap 로비"),
				ScoreboardCriterion.RenderType.INTEGER,
				false,
				null
			);
		} else {
			objective.setDisplayName(textTemplateResolver.format("&6MCsnap 로비"));
		}

		scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
		updateLine(scoreboard, objective, 0, textTemplateResolver.format("&f페이즈: &e" + matchManager.getPhase().getDisplayName()));

		var players = server.getPlayerManager().getPlayerList().stream()
			.sorted(Comparator.comparingInt((ServerPlayerEntity player) ->
				statsRepository.getLadder(player.getUuid(), player.getName().getString())).reversed()
				.thenComparing(player -> player.getName().getString()))
			.limit(5)
			.toList();

		for (int i = 0; i < 5; i++) {
			if (i < players.size()) {
				var player = players.get(i);
				var ladder = statsRepository.getLadder(player.getUuid(), player.getName().getString());
				updateLine(scoreboard, objective, i + 1, textTemplateResolver.format("&f" + (i + 1) + ". &e" + player.getName().getString() + " &7- &b" + ladder));
			} else {
				updateLine(scoreboard, objective, i + 1, Text.literal(" "));
			}
		}

		updateLine(scoreboard, objective, 6, textTemplateResolver.format("&7/mcsnap wiki 로 안내 확인"));
	}

	private void syncPlayerTeams(Scoreboard scoreboard, MatchManager matchManager, StatsRepository statsRepository, MinecraftServer server) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			var entryName = player.getNameForScoreboard();
			var currentTeam = scoreboard.getScoreHolderTeam(entryName);
			if (currentTeam != null && currentTeam.getName().startsWith("mcsnap_p_")) {
				scoreboard.removeScoreHolderFromTeam(entryName, currentTeam);
			}

			var teamName = "mcsnap_p_" + player.getUuidAsString().replace("-", "").substring(0, 12);
			var team = scoreboard.getTeam(teamName);
			if (team == null) {
				team = scoreboard.addTeam(teamName);
			}

			var state = matchManager.getPlayerState(player.getUuid());
			var ladder = statsRepository.getLadder(player.getUuid(), player.getName().getString());
			team.setPrefix(textTemplateResolver.format("&7[" + ladder + "] "));
			team.setSuffix(textTemplateResolver.format(state.getRoleType() == RoleType.CAPTAIN ? " &6[대장]" : ""));
			team.setColor(colorFor(state.getTeamId()));
			scoreboard.addScoreHolderToTeam(entryName, team);
		}
	}

	private void updateLine(Scoreboard scoreboard, net.minecraft.scoreboard.ScoreboardObjective objective, int index, Text displayText) {
		var holder = ScoreHolder.fromName(LINE_HOLDERS[index]);
		var score = scoreboard.getOrCreateScore(holder, objective);
		score.setScore(LINE_HOLDERS.length - index);
		score.setDisplayText(displayText);
	}

	private Formatting colorFor(TeamId teamId) {
		if (teamId == TeamId.RED) {
			return Formatting.RED;
		}
		if (teamId == TeamId.BLUE) {
			return Formatting.BLUE;
		}
		return Formatting.GRAY;
	}
}
