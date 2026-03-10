package karn.minecraftsnap.game;

public enum TeamId {
	RED("레드"),
	BLUE("블루");

	private final String displayName;

	TeamId(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
