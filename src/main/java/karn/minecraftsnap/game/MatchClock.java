package karn.minecraftsnap.game;

public class MatchClock {
	private int totalSeconds;
	private int remainingSeconds;

	public MatchClock(int totalSeconds) {
		this.totalSeconds = totalSeconds;
		this.remainingSeconds = totalSeconds;
	}

	public void reset(int totalSeconds) {
		this.totalSeconds = totalSeconds;
		this.remainingSeconds = totalSeconds;
	}

	public boolean tickSecond() {
		if (remainingSeconds <= 0) {
			return true;
		}

		remainingSeconds--;
		return remainingSeconds <= 0;
	}

	public int getTotalSeconds() {
		return totalSeconds;
	}

	public int getRemainingSeconds() {
		return remainingSeconds;
	}
}
