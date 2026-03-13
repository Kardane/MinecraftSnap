package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnitRegistryTest {
	@Test
	void loadsUnitsFromJavaDefinitions() {
		var registry = new UnitRegistry();

		assertEquals(16, registry.all().size());
		assertEquals(4, registry.byFaction(FactionId.VILLAGER).size());
		assertEquals(8, registry.allByFaction(FactionId.MONSTER).size());
		assertEquals(4, registry.byFaction(FactionId.NETHER).size());
		assertFalse(registry.get("villager").descriptionLines().isEmpty());
		assertEquals("주민&우민", registry.getFactionSpec(FactionId.VILLAGER).displayName());
		assertFalse(registry.get("zombie").advanceOptions().isEmpty());
	}
}
