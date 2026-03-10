package karn.minecraftsnap.game;

public enum MatchPhase {
	LOBBY("로비"),
	TEAM_SELECT("팀 설정"),
	FACTION_SELECT("팩션 선택"),
	GAME_RUNNING("게임 진행"),
	GAME_END("게임 종료");

	private final String displayName;

	MatchPhase(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
