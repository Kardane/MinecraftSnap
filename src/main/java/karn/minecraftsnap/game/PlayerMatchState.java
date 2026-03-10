package karn.minecraftsnap.game;

public class PlayerMatchState {
	private TeamId teamId;
	private RoleType roleType = RoleType.NONE;
	private FactionId factionId;
	private String preferredUnitId;

	public TeamId getTeamId() {
		return teamId;
	}

	public void setTeam(TeamId teamId, RoleType roleType) {
		this.teamId = teamId;
		this.roleType = roleType;
	}

	public void clear() {
		this.teamId = null;
		this.roleType = RoleType.NONE;
		this.factionId = null;
		this.preferredUnitId = null;
	}

	public RoleType getRoleType() {
		return roleType;
	}

	public boolean canUseTeamChat() {
		return teamId != null && roleType != RoleType.SPECTATOR && roleType != RoleType.NONE;
	}

	public boolean isUnit() {
		return teamId != null && roleType == RoleType.UNIT;
	}

	public FactionId getFactionId() {
		return factionId;
	}

	public void setFactionId(FactionId factionId) {
		this.factionId = factionId;
	}

	public String getPreferredUnitId() {
		return preferredUnitId;
	}

	public void setPreferredUnitId(String preferredUnitId) {
		this.preferredUnitId = preferredUnitId;
	}
}
