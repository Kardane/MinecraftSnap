package karn.minecraftsnap.game;

public class PlayerMatchState {
	private TeamId teamId;
	private RoleType roleType = RoleType.NONE;
	private FactionId factionId;
	private String preferredUnitId;
	private String currentUnitId;
	private int emeralds;
	private int goldIngots;
	private boolean advanceAvailable;
	private int advanceExp;
	private String advanceTargetUnitId;
	private LaneId lastLaneId;

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
		this.currentUnitId = null;
		this.advanceAvailable = false;
		this.advanceExp = 0;
		this.advanceTargetUnitId = null;
		this.lastLaneId = null;
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

	public boolean isCaptain() {
		return teamId != null && roleType == RoleType.CAPTAIN;
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

	public String getCurrentUnitId() {
		return currentUnitId;
	}

	public void setCurrentUnitId(String currentUnitId) {
		if ((this.currentUnitId == null && currentUnitId != null)
			|| (this.currentUnitId != null && !this.currentUnitId.equals(currentUnitId))) {
			resetAdvanceState();
		}
		this.currentUnitId = currentUnitId;
		if (currentUnitId == null) {
			this.lastLaneId = null;
			resetAdvanceState();
		}
	}

	public int getEmeralds() {
		return emeralds;
	}

	public void setEmeralds(int emeralds) {
		this.emeralds = emeralds;
	}

	public void addEmeralds(int amount) {
		this.emeralds += amount;
	}

	public int getGoldIngots() {
		return goldIngots;
	}

	public void setGoldIngots(int goldIngots) {
		this.goldIngots = goldIngots;
	}

	public void addGoldIngots(int amount) {
		this.goldIngots += amount;
	}

	public boolean isAdvanceAvailable() {
		return advanceAvailable;
	}

	public void setAdvanceAvailable(boolean advanceAvailable) {
		this.advanceAvailable = advanceAvailable;
	}

	public int getAdvanceExp() {
		return advanceExp;
	}

	public void setAdvanceExp(int advanceExp) {
		this.advanceExp = advanceExp;
	}

	public String getAdvanceTargetUnitId() {
		return advanceTargetUnitId;
	}

	public void setAdvanceTargetUnitId(String advanceTargetUnitId) {
		this.advanceTargetUnitId = advanceTargetUnitId;
	}

	public void resetAdvanceState() {
		this.advanceAvailable = false;
		this.advanceExp = 0;
		this.advanceTargetUnitId = null;
	}

	public LaneId getLastLaneId() {
		return lastLaneId;
	}

	public void setLastLaneId(LaneId lastLaneId) {
		this.lastLaneId = lastLaneId;
	}
}
