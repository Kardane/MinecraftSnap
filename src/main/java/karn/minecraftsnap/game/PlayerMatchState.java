package karn.minecraftsnap.game;

public class PlayerMatchState {
	private TeamId teamId;
	private RoleType roleType = RoleType.NONE;

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
	}

	public RoleType getRoleType() {
		return roleType;
	}

	public boolean canUseTeamChat() {
		return teamId != null && roleType != RoleType.SPECTATOR && roleType != RoleType.NONE;
	}
}
