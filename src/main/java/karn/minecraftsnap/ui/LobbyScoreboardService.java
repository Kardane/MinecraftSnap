package karn.minecraftsnap.ui;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
		clearPlayerTeams(scoreboard, server);

		if (shouldHideSidebar(matchManager.getPhase())) {
			scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
			return;
		}

		var objective = scoreboard.getNullableObjective(OBJECTIVE_NAME);
		if (objective == null) {
			objective = scoreboard.addObjective(
				OBJECTIVE_NAME,
				ScoreboardCriterion.DUMMY,
				textTemplateResolver.format(textConfig().lobbyScoreboardTitle),
				ScoreboardCriterion.RenderType.INTEGER,
				false,
				null
			);
		} else {
			objective.setDisplayName(textTemplateResolver.format(textConfig().lobbyScoreboardTitle));
		}

		scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
		updateLine(scoreboard, objective, 0, textTemplateResolver.format(textConfig().lobbyScoreboardPhaseTemplate.replace("{phase}", matchManager.getPhase().getDisplayName())));

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
				updateLine(scoreboard, objective, i + 1, textTemplateResolver.format(textConfig().lobbyScoreboardPlayerTemplate
					.replace("{rank}", Integer.toString(i + 1))
					.replace("{player}", player.getName().getString())
					.replace("{ladder}", Integer.toString(ladder))));
			} else {
				updateLine(scoreboard, objective, i + 1, Text.literal(" "));
			}
		}

		updateLine(scoreboard, objective, 6, textTemplateResolver.format(textConfig().lobbyScoreboardWikiHint));
	}

	private void clearPlayerTeams(Scoreboard scoreboard, MinecraftServer server) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			var entryName = player.getNameForScoreboard();
			var currentTeam = scoreboard.getScoreHolderTeam(entryName);
			if (currentTeam != null && currentTeam.getName().startsWith("mcsnap_p_")) {
				scoreboard.removeScoreHolderFromTeam(entryName, currentTeam);
			}
		}
	}

	static boolean shouldHideSidebar(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING || phase == MatchPhase.GAME_END;
	}

	private void updateLine(Scoreboard scoreboard, net.minecraft.scoreboard.ScoreboardObjective objective, int index, Text displayText) {
		var holder = ScoreHolder.fromName(LINE_HOLDERS[index]);
		var score = scoreboard.getOrCreateScore(holder, objective);
		score.setScore(LINE_HOLDERS.length - index);
		score.setDisplayText(displayText);
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

}
