package karn.minecraftsnap.ui;

public final class BossBarFormatter {
	private BossBarFormatter() {
	}

	public static String format(String template, int remainingSeconds, int redScore, int blueScore) {
		return format(template, remainingSeconds, redScore, blueScore, "--:--");
	}

	public static String format(String template, int remainingSeconds, int redScore, int blueScore, String nextRevealTime) {
		return template
			.replace("{time}", formatTime(remainingSeconds))
			.replace("{red_score}", Integer.toString(redScore))
			.replace("{blue_score}", Integer.toString(blueScore))
			.replace("{next_reveal_time}", nextRevealTime == null || nextRevealTime.isBlank() ? "--:--" : nextRevealTime);
	}

	public static String formatTime(int remainingSeconds) {
		int minutes = Math.max(remainingSeconds, 0) / 60;
		int seconds = Math.max(remainingSeconds, 0) % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	public static float percent(int remainingSeconds, int totalSeconds) {
		if (totalSeconds <= 0) {
			return 0.0f;
		}

		return Math.clamp((float) remainingSeconds / (float) totalSeconds, 0.0f, 1.0f);
	}

	public static float percentTicks(int remainingTicks, int totalTicks) {
		if (totalTicks <= 0) {
			return 0.0f;
		}

		return Math.clamp((float) remainingTicks / (float) totalTicks, 0.0f, 1.0f);
	}
}
