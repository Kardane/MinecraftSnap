package karn.minecraftsnap.game;

public class CaptainState {
	private int currentMana = 3;
	private int maxMana = 3;
	private int secondsUntilNextMana = 15;
	private int spawnCooldownSeconds;
	private int skillCooldownSeconds;

	public int getCurrentMana() {
		return currentMana;
	}

	public void setCurrentMana(int currentMana) {
		this.currentMana = currentMana;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
		if (currentMana > maxMana) {
			currentMana = maxMana;
		}
	}

	public int getSecondsUntilNextMana() {
		return secondsUntilNextMana;
	}

	public void setSecondsUntilNextMana(int secondsUntilNextMana) {
		this.secondsUntilNextMana = secondsUntilNextMana;
	}

	public int getSpawnCooldownSeconds() {
		return spawnCooldownSeconds;
	}

	public void setSpawnCooldownSeconds(int spawnCooldownSeconds) {
		this.spawnCooldownSeconds = spawnCooldownSeconds;
	}

	public int getSkillCooldownSeconds() {
		return skillCooldownSeconds;
	}

	public void setSkillCooldownSeconds(int skillCooldownSeconds) {
		this.skillCooldownSeconds = skillCooldownSeconds;
	}
}
