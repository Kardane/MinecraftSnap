package karn.minecraftsnap.game;

public class MatchClock {
	private int totalTicks;
	private int remainingTicks;

	public MatchClock(int totalSeconds) {
		reset(totalSeconds);
	}

	public void reset(int totalSeconds) {
		totalTicks = Math.max(0, totalSeconds * 20);
		remainingTicks = totalTicks;
	}

	public boolean tick() {
		if (remainingTicks <= 0) {
			return true;
		}

		remainingTicks--;
		return remainingTicks <= 0;
	}

	public int getTotalSeconds() {
		return (totalTicks + 19) / 20;
	}

	public int getRemainingSeconds() {
		return (remainingTicks + 19) / 20;
	}

	public boolean reduceRemainingSeconds(int amount) {
		return reduceRemainingTicks(amount * 20);
	}

	public boolean reduceRemainingTicks(int amount) {
		if (amount <= 0) {
			return remainingTicks <= 0;
		}
		remainingTicks = Math.max(0, remainingTicks - amount);
		return remainingTicks <= 0;
	}

	public boolean adjustDurationTicks(int deltaTicks) {
		if (deltaTicks == 0) {
			return remainingTicks <= 0;
		}
		var elapsedTicks = getElapsedTicks();
		totalTicks = Math.max(elapsedTicks, totalTicks + deltaTicks);
		remainingTicks = totalTicks - elapsedTicks;
		return remainingTicks <= 0;
	}

	public int getTotalTicks() {
		return totalTicks;
	}

	public int getRemainingTicks() {
		return remainingTicks;
	}

	public int getElapsedTicks() {
		return Math.max(0, totalTicks - remainingTicks);
	}

	public int getElapsedSeconds() {
		return getElapsedTicks() / 20;
	}
}
