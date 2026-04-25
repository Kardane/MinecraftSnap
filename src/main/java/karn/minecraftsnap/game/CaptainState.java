package karn.minecraftsnap.game;

public class CaptainState {
	private int currentMana = 3;
	private int baseMaxMana = 3;
	private int bonusMaxMana = 0;
	private int secondsUntilNextMana = 10;
	private int skillCooldownSeconds;
	private String monsterWeatherType = "";
	private long monsterWeatherEndTick;
	private int monsterWeatherLastProcessedSecond = -1;
	private LaneId netherPortalLaneId;
	private long netherPortalEndTick;
	private int netherPortalLastProcessedSecond = -1;

	public int getCurrentMana() {
		return currentMana;
	}

	public void setCurrentMana(int currentMana) {
		this.currentMana = currentMana;
	}

	public int getMaxMana() {
		return baseMaxMana + bonusMaxMana;
	}

	public void setMaxMana(int maxMana) {
		this.baseMaxMana = maxMana;
		clampCurrentMana();
	}

	public int getBaseMaxMana() {
		return baseMaxMana;
	}

	public void setBonusMaxMana(int bonusMaxMana) {
		this.bonusMaxMana = Math.max(0, bonusMaxMana);
		clampCurrentMana();
	}

	public void addBonusMaxMana(int amount) {
		if (amount <= 0) {
			return;
		}
		this.bonusMaxMana += amount;
	}

	public int getBonusMaxMana() {
		return bonusMaxMana;
	}

	public int getSecondsUntilNextMana() {
		return secondsUntilNextMana;
	}

	public void setSecondsUntilNextMana(int secondsUntilNextMana) {
		this.secondsUntilNextMana = secondsUntilNextMana;
	}



	public int getSkillCooldownSeconds() {
		return skillCooldownSeconds;
	}

	public void setSkillCooldownSeconds(int skillCooldownSeconds) {
		this.skillCooldownSeconds = skillCooldownSeconds;
	}

	public String getMonsterWeatherType() {
		return monsterWeatherType;
	}

	public void setMonsterWeatherType(String monsterWeatherType) {
		this.monsterWeatherType = monsterWeatherType == null ? "" : monsterWeatherType;
	}

	public long getMonsterWeatherEndTick() {
		return monsterWeatherEndTick;
	}

	public void setMonsterWeatherEndTick(long monsterWeatherEndTick) {
		this.monsterWeatherEndTick = monsterWeatherEndTick;
	}

	public int getMonsterWeatherLastProcessedSecond() {
		return monsterWeatherLastProcessedSecond;
	}

	public void setMonsterWeatherLastProcessedSecond(int monsterWeatherLastProcessedSecond) {
		this.monsterWeatherLastProcessedSecond = monsterWeatherLastProcessedSecond;
	}

	public LaneId getNetherPortalLaneId() {
		return netherPortalLaneId;
	}

	public void setNetherPortalLaneId(LaneId netherPortalLaneId) {
		this.netherPortalLaneId = netherPortalLaneId;
	}

	public long getNetherPortalEndTick() {
		return netherPortalEndTick;
	}

	public void setNetherPortalEndTick(long netherPortalEndTick) {
		this.netherPortalEndTick = netherPortalEndTick;
	}

	public int getNetherPortalLastProcessedSecond() {
		return netherPortalLastProcessedSecond;
	}

	public void setNetherPortalLastProcessedSecond(int netherPortalLastProcessedSecond) {
		this.netherPortalLastProcessedSecond = netherPortalLastProcessedSecond;
	}

	public void clearMonsterWeather() {
		monsterWeatherType = "";
		monsterWeatherEndTick = 0L;
		monsterWeatherLastProcessedSecond = -1;
	}

	public void clearNetherPortal() {
		netherPortalLaneId = null;
		netherPortalEndTick = 0L;
		netherPortalLastProcessedSecond = -1;
	}

	private void clampCurrentMana() {
		if (currentMana > getMaxMana()) {
			currentMana = getMaxMana();
		}
	}
}
