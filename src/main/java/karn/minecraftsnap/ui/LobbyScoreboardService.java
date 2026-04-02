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
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LobbyScoreboardService {
	private static final String OBJECTIVE_NAME = "minecraftsnap_lobby";
	private static final String[] LINE_HOLDERS = {
		"mcsnap_line_0",
		"mcsnap_line_1",
		"mcsnap_line_2",
		"mcsnap_line_3",
		"mcsnap_line_4",
		"mcsnap_line_5",
		"mcsnap_line_6",
		"mcsnap_line_7",
		"mcsnap_line_8",
		"mcsnap_line_9",
		"mcsnap_line_10",
		"mcsnap_line_11",
		"mcsnap_line_12",
		"mcsnap_line_13",
		"mcsnap_line_14"
	};

	private final TextTemplateResolver textTemplateResolver;

	public LobbyScoreboardService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	static BlankNumberFormat sidebarNumberFormat() {
		return BlankNumberFormat.INSTANCE;
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
				sidebarNumberFormat()
			);
		} else {
			objective.setDisplayName(textTemplateResolver.format(textConfig().lobbyScoreboardTitle));
			objective.setNumberFormat(sidebarNumberFormat());
		}

		scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
		if (matchManager.getPhase() == MatchPhase.GAME_RUNNING) {
			renderRunningSidebar(scoreboard, objective, matchManager);
			return;
		}
		var rankedPlayers = rankEntries(statsRepository.allEntries().stream()
			.map(entry -> {
				var stats = entry.stats();
				return new LobbyRankingEntry(
					stats.lastKnownName == null || stats.lastKnownName.isBlank() ? entry.playerId().toString() : stats.lastKnownName,
					stats.ladder,
					stats.wins,
					stats.losses
				);
			})
			.toList());

		for (int i = 0; i < LINE_HOLDERS.length; i++) {
			if (i < rankedPlayers.size()) {
				var player = rankedPlayers.get(i);
				updateLine(scoreboard, objective, i, textTemplateResolver.format(textConfig().lobbyScoreboardPlayerTemplate
					.replace("{rank}", Integer.toString(i + 1))
					.replace("{player}", player.playerName())
					.replace("{ladder}", Integer.toString(player.ladder()))));
			} else {
				updateLine(scoreboard, objective, i, Text.literal(" "));
			}
		}
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
		var biomeName = textConfig().runningSidebarHiddenBiomeName;
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
			return textConfig().runningSidebarNeutralIcon;
		}
		var state = mod.getCapturePointService().getState(laneId);
		if (state == null) {
			return textConfig().runningSidebarNeutralIcon;
		}
		if (state.getProgress().isContested()) {
			return textConfig().runningSidebarContestedIcon;
		}
		return switch (state.getOwner()) {
			case RED -> textConfig().runningSidebarRedIcon;
			case BLUE -> textConfig().runningSidebarBlueIcon;
			case NEUTRAL -> textConfig().runningSidebarNeutralIcon;
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

	static List<LobbyRankingEntry> rankEntries(List<LobbyRankingEntry> entries) {
		if (entries == null) {
			return List.of();
		}
		return entries.stream()
			.sorted((left, right) -> {
				int ladderCompare = Integer.compare(right.ladder(), left.ladder());
				if (ladderCompare != 0) {
					return ladderCompare;
				}
				int winRateCompare = Double.compare(winRate(right), winRate(left));
				if (winRateCompare != 0) {
					return winRateCompare;
				}
				int gamesCompare = Integer.compare(right.games(), left.games());
				if (gamesCompare != 0) {
					return gamesCompare;
				}
				return left.playerName().toLowerCase(Locale.ROOT).compareTo(right.playerName().toLowerCase(Locale.ROOT));
			})
			.limit(LINE_HOLDERS.length)
			.toList();
	}

	static double winRate(LobbyRankingEntry entry) {
		return entry.games() <= 0 ? 0.0 : (double) entry.wins() / (double) entry.games();
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

	record LobbyRankingEntry(String playerName, int ladder, int wins, int losses) {
		int games() {
			return wins + losses;
		}
	}

}
