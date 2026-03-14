package karn.minecraftsnap.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaptainManaService {
	public static final int STARTING_MANA = 3;
	public static final int DEFAULT_MANA_RECOVERY_SECONDS = 10;
	private final Map<UUID, CaptainState> captainStates = new HashMap<>();

	public CaptainState getOrCreate(UUID captainId) {
		return captainStates.computeIfAbsent(captainId, ignored -> {
			var state = new CaptainState();
			state.setCurrentMana(STARTING_MANA);
			state.setMaxMana(STARTING_MANA);
			state.setSecondsUntilNextMana(DEFAULT_MANA_RECOVERY_SECONDS);
			return state;
		});
	}

	public void clear() {
		captainStates.clear();
	}

	public void tickSecond(int baseMaxMana, int elapsedSeconds) {
		tickSecond(baseMaxMana, elapsedSeconds, DEFAULT_MANA_RECOVERY_SECONDS);
	}

	public void tickSecond(int baseMaxMana, int elapsedSeconds, int recoverySeconds) {
		var normalizedRecoverySeconds = Math.max(1, recoverySeconds);
		for (var state : captainStates.values()) {
			var expectedMax = baseMaxMana + elapsedSeconds / 60;
			if (expectedMax > state.getMaxMana()) {
				var gainedMana = expectedMax - state.getMaxMana();
				state.setMaxMana(expectedMax);
				state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + gainedMana));
			}

			if (state.getSpawnCooldownSeconds() > 0) {
				state.setSpawnCooldownSeconds(state.getSpawnCooldownSeconds() - 1);
			}
			if (state.getSkillCooldownSeconds() > 0) {
				state.setSkillCooldownSeconds(state.getSkillCooldownSeconds() - 1);
			}

			if (state.getCurrentMana() >= state.getMaxMana()) {
				state.setSecondsUntilNextMana(normalizedRecoverySeconds);
				continue;
			}

			var remaining = state.getSecondsUntilNextMana() - 1;
			if (remaining <= 0) {
				state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + 1));
				state.setSecondsUntilNextMana(normalizedRecoverySeconds);
				continue;
			}

			state.setSecondsUntilNextMana(remaining);
		}
	}

	public void initializeCaptain(UUID captainId) {
		initializeCaptain(captainId, DEFAULT_MANA_RECOVERY_SECONDS);
	}

	public void initializeCaptain(UUID captainId, int recoverySeconds) {
		var state = getOrCreate(captainId);
		state.setMaxMana(STARTING_MANA);
		state.setCurrentMana(STARTING_MANA);
		state.setSecondsUntilNextMana(Math.max(1, recoverySeconds));
		state.setSpawnCooldownSeconds(0);
		state.setSkillCooldownSeconds(0);
	}

	public void refillMana(UUID captainId) {
		refillMana(captainId, DEFAULT_MANA_RECOVERY_SECONDS);
	}

	public void refillMana(UUID captainId, int recoverySeconds) {
		var state = getOrCreate(captainId);
		state.setCurrentMana(state.getMaxMana());
		state.setSecondsUntilNextMana(Math.max(1, recoverySeconds));
	}

	public boolean trySpendForSpawn(UUID captainId, int cost, int cooldownSeconds) {
		var state = getOrCreate(captainId);
		if (state.getCurrentMana() < cost || state.getSpawnCooldownSeconds() > 0) {
			return false;
		}
		state.setCurrentMana(state.getCurrentMana() - cost);
		state.setSpawnCooldownSeconds(cooldownSeconds);
		return true;
	}

	public void reduceSpawnCooldown(UUID captainId, int seconds) {
		var state = getOrCreate(captainId);
		state.setSpawnCooldownSeconds(Math.max(0, state.getSpawnCooldownSeconds() - seconds));
	}

	public void triggerSkillCooldown(UUID captainId, int cooldownSeconds) {
		getOrCreate(captainId).setSkillCooldownSeconds(cooldownSeconds);
	}

	public boolean trySpendForSkill(UUID captainId, int cost, int cooldownSeconds) {
		var state = getOrCreate(captainId);
		if (state.getCurrentMana() < cost || state.getSkillCooldownSeconds() > 0) {
			return false;
		}
		state.setCurrentMana(state.getCurrentMana() - cost);
		state.setSkillCooldownSeconds(cooldownSeconds);
		return true;
	}

	public void refundSpawnResources(UUID captainId, int manaAmount, int cooldownSeconds) {
		var state = getOrCreate(captainId);
		if (manaAmount > 0) {
			state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + manaAmount));
		}
		if (cooldownSeconds > 0) {
			state.setSpawnCooldownSeconds(Math.max(0, state.getSpawnCooldownSeconds() - cooldownSeconds));
		}
	}

	public void clearRuntimeState(UUID captainId) {
		var state = getOrCreate(captainId);
		state.clearMonsterWeather();
		state.clearNetherPortal();
	}
}
