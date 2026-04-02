package karn.minecraftsnap.game;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.UUID;

public class CaptainManaService {
	public static final int STARTING_MANA = 3;
	public static final int DEFAULT_MANA_RECOVERY_SECONDS = 10;
	public static final int MIN_MANA_RECOVERY_SECONDS = 5;
	public static final int FULL_RECOVERY_TEAM_SIZE = 4;
	private final Map<UUID, CaptainState> captainStates = new HashMap<>();
	private final Map<UUID, FactionId> captainFactions = new HashMap<>();

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
		captainFactions.clear();
	}

	public void tickSecond(int baseMaxMana, int elapsedSeconds) {
		tickSecond(baseMaxMana, elapsedSeconds, DEFAULT_MANA_RECOVERY_SECONDS);
	}

	public void tickSecond(int baseMaxMana, int elapsedSeconds, int recoverySeconds) {
		tickSecond(baseMaxMana, elapsedSeconds, ignored -> recoverySeconds);
	}

	public void tickSecond(int baseMaxMana, int elapsedSeconds, ToIntFunction<UUID> recoverySecondsResolver) {
		for (var entry : captainStates.entrySet()) {
			var captainId = entry.getKey();
			var state = entry.getValue();
			var normalizedRecoverySeconds = Math.max(1, recoverySecondsResolver == null ? DEFAULT_MANA_RECOVERY_SECONDS : recoverySecondsResolver.applyAsInt(captainId));
			var expectedMax = baseMaxMana + elapsedSeconds / 60;
			if (expectedMax > state.getMaxMana()) {
				var gainedMana = expectedMax - state.getMaxMana();
				state.setMaxMana(expectedMax);
				state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + gainedMana));
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
				state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + manaRecoveryAmount(captainId, 1)));
				state.setSecondsUntilNextMana(normalizedRecoverySeconds);
				continue;
			}

			state.setSecondsUntilNextMana(remaining);
		}
	}

	public void initializeCaptain(UUID captainId) {
		initializeCaptain(captainId, null, DEFAULT_MANA_RECOVERY_SECONDS);
	}

	public void initializeCaptain(UUID captainId, int recoverySeconds) {
		initializeCaptain(captainId, null, recoverySeconds);
	}

	public void initializeCaptain(UUID captainId, FactionId factionId, int recoverySeconds) {
		var state = getOrCreate(captainId);
		setCaptainFaction(captainId, factionId);
		state.setMaxMana(STARTING_MANA);
		state.setCurrentMana(STARTING_MANA);
		state.setSecondsUntilNextMana(Math.max(1, recoverySeconds));
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

	public static boolean shouldRefillOnReveal(TeamId teamId, int redScore, int blueScore) {
		if (teamId == null || redScore == blueScore) {
			return true;
		}
		return switch (teamId) {
			case RED -> redScore < blueScore;
			case BLUE -> blueScore < redScore;
		};
	}

	public boolean trySpendForSpawn(UUID captainId, int cost) {
		var state = getOrCreate(captainId);
		if (state.getCurrentMana() < cost) {
			return false;
		}
		state.setCurrentMana(state.getCurrentMana() - cost);
		return true;
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

	public void restoreMana(UUID captainId, int amount) {
		if (amount <= 0) {
			return;
		}
		var state = getOrCreate(captainId);
		state.setCurrentMana(Math.min(state.getMaxMana(), state.getCurrentMana() + manaRecoveryAmount(captainId, amount)));
	}

	public void clearRuntimeState(UUID captainId) {
		var state = getOrCreate(captainId);
		state.clearMonsterWeather();
		state.clearNetherPortal();
	}

	void setCaptainFaction(UUID captainId, FactionId factionId) {
		if (captainId == null) {
			return;
		}
		if (factionId == null) {
			captainFactions.remove(captainId);
			return;
		}
		captainFactions.put(captainId, factionId);
	}

	int manaRecoveryAmount(UUID captainId, int baseAmount) {
		if (baseAmount <= 0) {
			return 0;
		}
		return captainFactions.get(captainId) == FactionId.NETHER ? baseAmount + 1 : baseAmount;
	}

	public static int recoverySecondsForTeamSize(int nonCaptainTeammates) {
		var extraTeammates = Math.max(0, nonCaptainTeammates - FULL_RECOVERY_TEAM_SIZE);
		return Math.max(MIN_MANA_RECOVERY_SECONDS, DEFAULT_MANA_RECOVERY_SECONDS - extraTeammates);
	}
}
