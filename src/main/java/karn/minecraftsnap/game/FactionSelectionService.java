package karn.minecraftsnap.game;

import java.util.EnumMap;
import java.util.Map;

public class FactionSelectionService {
	public EnumMap<TeamId, FactionId> newSelectionMap() {
		return new EnumMap<>(TeamId.class);
	}

	public boolean isComplete(Map<TeamId, FactionId> selections) {
		return selections.containsKey(TeamId.RED) && selections.containsKey(TeamId.BLUE);
	}

	public void fillMissingWithDefault(Map<TeamId, FactionId> selections) {
		selections.putIfAbsent(TeamId.RED, FactionId.VILLAGER);
		selections.putIfAbsent(TeamId.BLUE, FactionId.VILLAGER);
	}
}
