package karn.minecraftsnap.ui;

public final class BossBarFormatter {
	private BossBarFormatter() {
	}

	public static String format(String template, int remainingSeconds, int redScore, int blueScore) {
		return template
			.replace("{time}", formatTime(remainingSeconds))
			.replace("{red_score}", Integer.toString(redScore))
			.replace("{blue_score}", Integer.toString(blueScore));
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
}
