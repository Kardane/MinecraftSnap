package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class VictoryCountdownService {
	private final MatchManager matchManager;
	private final BiConsumer<TeamId, Integer> subtitleSender;
	private final IntConsumer soundSender;
	private TeamId lastTeam;
	private int lastRemaining = Integer.MIN_VALUE;

	public VictoryCountdownService(MatchManager matchManager, BiConsumer<TeamId, Integer> subtitleSender) {
		this(matchManager, subtitleSender, remaining -> {
		});
	}

	public VictoryCountdownService(MatchManager matchManager, BiConsumer<TeamId, Integer> subtitleSender, IntConsumer soundSender) {
		this.matchManager = matchManager;
		this.subtitleSender = subtitleSender;
		this.soundSender = soundSender;
	}

	public void tick(SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			reset();
			return;
		}
		var teamId = matchManager.getAllPointsHeldTeam();
		if (teamId == null) {
			reset();
			return;
		}
		var remaining = remainingSeconds(matchManager.getAllPointsHeldSeconds(), systemConfig.capture.allPointsHoldSeconds);
		if (teamId == lastTeam && remaining == lastRemaining) {
			return;
		}
		subtitleSender.accept(teamId, remaining);
		soundSender.accept(remaining);
		lastTeam = teamId;
		lastRemaining = remaining;
	}

	static int remainingSeconds(int heldSeconds, int requiredSeconds) {
		if (requiredSeconds <= 0 || heldSeconds <= 0) {
			return 0;
		}
		return Math.max(0, requiredSeconds - heldSeconds + 1);
	}

	private void reset() {
		lastTeam = null;
		lastRemaining = Integer.MIN_VALUE;
	}
}
