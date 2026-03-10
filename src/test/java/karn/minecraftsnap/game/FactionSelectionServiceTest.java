package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactionSelectionServiceTest {
	@Test
	void missingFactionDefaultsToVillager() {
		var service = new FactionSelectionService();
		var selections = new EnumMap<TeamId, FactionId>(TeamId.class);
		selections.put(TeamId.RED, FactionId.MONSTER);

		service.fillMissingWithDefault(selections);

		assertEquals(FactionId.MONSTER, selections.get(TeamId.RED));
		assertEquals(FactionId.VILLAGER, selections.get(TeamId.BLUE));
	}
}
