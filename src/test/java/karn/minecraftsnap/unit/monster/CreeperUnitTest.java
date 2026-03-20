package karn.minecraftsnap.unit.monster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreeperUnitTest {
	@Test
	void baseCreeperMatchesDocumentSpec() {
		var unit = new CreeperUnit();
		var definition = unit.definition();

		assertEquals(5, definition.cost());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:tnt", definition.mainHand().itemId);
		assertEquals("minecraft:tnt", definition.abilityItemSpec().itemId);
		assertEquals(20, definition.abilityCooldownSeconds());
		assertEquals(30L, unit.selfDestructDelayTicks());
		assertEquals(900, definition.advanceOptions().getFirst().requiredTicks);
		assertTrue(definition.advanceOptions().getFirst().biomes.isEmpty());
		assertEquals(1, definition.advanceOptions().getFirst().weathers.size());
		assertEquals("thunder", definition.advanceOptions().getFirst().weathers.getFirst());
		assertEquals(5.0D, unit.blastRadius());
		assertEquals(50.0f, unit.blastDamage());
		assertTrue(unit.activeDisguise().entityNbt.contains("ignited:1b"));
	}

	@Test
	void chargedCreeperUsesPoweredDisguiseAndBuffedExplosion() {
		var unit = new ChargedCreeperUnit();
		var definition = unit.definition();

		assertEquals(20, definition.abilityCooldownSeconds());
		assertEquals(7.0D, unit.blastRadius());
		assertEquals(100.0f, unit.blastDamage());
		assertTrue(unit.restingDisguise().entityNbt.contains("powered:1b"));
		assertTrue(unit.activeDisguise().entityNbt.contains("powered:1b"));
		assertTrue(unit.activeDisguise().entityNbt.contains("ignited:1b"));
	}
}
