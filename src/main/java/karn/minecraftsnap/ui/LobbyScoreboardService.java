package karn.minecraftsnap.ui;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.game.LaneId;
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
		if (matchManager.getPhase() == MatchPhase.GAME_RUNNING) {
			renderRunningSidebar(scoreboard, objective, matchManager);
			return;
		}
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

	private void renderRunningSidebar(Scoreboard scoreboard, net.minecraft.scoreboard.ScoreboardObjective objective, MatchManager matchManager) {
		updateLine(scoreboard, objective, 0, formatRunningLane(matchManager, LaneId.LANE_1));
		updateLine(scoreboard, objective, 1, formatRunningLane(matchManager, LaneId.LANE_2));
		updateLine(scoreboard, objective, 2, formatRunningLane(matchManager, LaneId.LANE_3));
		for (int i = 3; i < LINE_HOLDERS.length; i++) {
			scoreboard.removeScore(ScoreHolder.fromName(LINE_HOLDERS[i]), objective);
		}
	}

	private Text formatRunningLane(MatchManager matchManager, LaneId laneId) {
		var biomeName = "???";
		if (matchManager.isLaneRevealed(laneId)) {
			var assignedBiomeId = matchManager.getAssignedBiomeId(laneId);
			var mod = MinecraftSnap.getInstance();
			if (assignedBiomeId != null && mod != null) {
				biomeName = mod.getBiomeCatalog().biomes.stream()
					.filter(entry -> assignedBiomeId.equals(entry.id))
					.findFirst()
					.map(entry -> entry.displayName)
					.orElse(assignedBiomeId);
			}
		}
		var icon = runningIcon(matchManager, laneId);
		return textTemplateResolver.format(textConfig().runningSidebarLaneTemplate
			.replace("{icon}", icon)
			.replace("{biome}", biomeName));
	}

	private String runningIcon(MatchManager matchManager, LaneId laneId) {
		var mod = MinecraftSnap.getInstance();
		if (mod == null || mod.getCapturePointService() == null) {
			return "&f⬛";
		}
		var state = mod.getCapturePointService().getState(laneId);
		if (state == null) {
			return "&f⬛";
		}
		if (state.getProgress().isContested()) {
			return "&e⬛";
		}
		return switch (state.getOwner()) {
			case RED -> "&c⬛";
			case BLUE -> "&9⬛";
			case NEUTRAL -> "&f⬛";
		};
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
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_END;
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
