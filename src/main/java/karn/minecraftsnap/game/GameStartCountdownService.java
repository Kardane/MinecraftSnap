package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;

import java.util.function.Consumer;

public class GameStartCountdownService {
	private final MatchManager matchManager;
	private final Consumer<Integer> countdownSender;
	private final Consumer<Integer> soundSender;
	private int lastRemaining = Integer.MIN_VALUE;

	public GameStartCountdownService(MatchManager matchManager, Consumer<Integer> countdownSender, Consumer<Integer> soundSender) {
		this.matchManager = matchManager;
		this.countdownSender = countdownSender;
		this.soundSender = soundSender;
	}

	public void tick(SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_START) {
			lastRemaining = Integer.MIN_VALUE;
			return;
		}
		if (matchManager.getPhaseTicks() % 20L != 0L) {
			return;
		}
		var remaining = remainingSeconds(matchManager.getPhaseTicks(), systemConfig.gameStart.waitSeconds);
		if (remaining <= 0 || remaining == lastRemaining) {
			return;
		}
		countdownSender.accept(remaining);
		soundSender.accept(remaining);
		lastRemaining = remaining;
	}

	static int remainingSeconds(long phaseTicks, int waitSeconds) {
		return Math.max(0, waitSeconds - (int) (phaseTicks / 20L));
	}
}
