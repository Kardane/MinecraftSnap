package karn.minecraftsnap.game;

public enum CaptureOwner {
	RED,
	NEUTRAL,
	BLUE;

	public static CaptureOwner fromTeam(TeamId teamId) {
		if (teamId == TeamId.RED) {
			return RED;
		}
		if (teamId == TeamId.BLUE) {
			return BLUE;
		}
		return NEUTRAL;
	}
}
