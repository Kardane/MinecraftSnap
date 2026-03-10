package karn.minecraftsnap.game;

public enum RoleType {
	CAPTAIN("사령관"),
	UNIT("유닛"),
	SPECTATOR("관전자"),
	NONE("미배정");

	private final String displayName;

	RoleType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
