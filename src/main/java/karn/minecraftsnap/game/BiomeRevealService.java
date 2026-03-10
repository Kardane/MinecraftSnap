package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BiomeRevealService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;

	public BiomeRevealService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
	}

	public List<LaneId> tick(MinecraftServer server, SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return List.of();
		}

		var elapsedSeconds = matchManager.getTotalSeconds() - matchManager.getRemainingSeconds();
		var revealed = syncRevealState(elapsedSeconds, systemConfig.biomeReveal);
		for (var laneId : revealed) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(
				systemConfig.biomeReveal.messageTemplate,
				Map.of("{lane}", laneLabel(laneId))
			), false);
		}
		return revealed;
	}

	public List<LaneId> syncRevealState(int elapsedSeconds, SystemConfig.BiomeRevealConfig config) {
		List<LaneId> revealed = new ArrayList<>();
		revealIfDue(LaneId.LANE_1, elapsedSeconds, config.lane1RevealSecond, revealed);
		revealIfDue(LaneId.LANE_2, elapsedSeconds, config.lane2RevealSecond, revealed);
		revealIfDue(LaneId.LANE_3, elapsedSeconds, config.lane3RevealSecond, revealed);
		return revealed;
	}

	private void revealIfDue(LaneId laneId, int elapsedSeconds, int revealSecond, List<LaneId> revealed) {
		var override = matchManager.getLaneRevealOverride(laneId);
		if (override != null) {
			if (override) {
				matchManager.revealLane(laneId);
			}
			return;
		}

		if (elapsedSeconds < revealSecond || matchManager.isLaneRevealed(laneId)) {
			return;
		}

		matchManager.revealLane(laneId);
		revealed.add(laneId);
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번";
			case LANE_2 -> "2번";
			case LANE_3 -> "3번";
		};
	}
}
