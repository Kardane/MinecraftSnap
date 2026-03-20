package karn.minecraftsnap.unit.nether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NetherFactionUnitTest {
	@Test
	void piglinMatchesDocumentSpec() {
		var unit = new PiglinUnit();
		var definition = unit.definition();

		assertEquals(2, definition.cost());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:golden_sword", definition.mainHand().itemId);
		assertEquals("", definition.offHand().itemId);
		assertEquals("", definition.abilityName());
		assertEquals(0, definition.abilityCooldownSeconds());
		assertEquals(3, unit.supportGoldCount());
		assertEquals(1, unit.bonusGoldOnKill());
	}

	@Test
	void zombifiedPiglinMatchesDocumentSpec() {
		var unit = new ZombifiedPiglinUnit();
		var definition = unit.definition();

		assertEquals(2, definition.cost());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:golden_sword", definition.mainHand().itemId);
		assertEquals("", definition.abilityName());
		assertEquals(0, definition.abilityCooldownSeconds());
		assertEquals(0.1D, unit.summonChance());
		assertEquals(5.0D, unit.summonRange());
		assertFalse(unit.hasTriggeringAttacker(null));
	}

	@Test
	void blazeMatchesDocumentSpec() {
		var unit = new BlazeUnit();
		var definition = unit.definition();

		assertEquals(3, definition.cost());
		assertEquals(14.0, definition.maxHealth());
		assertEquals(1.2, definition.moveSpeedScale());
		assertEquals("minecraft:blaze_rod", definition.mainHand().itemId);
		assertEquals("minecraft:blaze_rod", definition.abilityItemSpec().itemId);
		assertEquals(9, definition.abilityCooldownSeconds());
		assertEquals(2.0D, unit.weaponAttackDamage());
		assertEquals(1.0D, unit.weaponAttackSpeed());
		assertEquals(2, unit.weaponFireAspectLevel());
		assertEquals(40, unit.fireResistanceDurationTicks());
		assertEquals(3, unit.shotCount());
		assertEquals(8L, unit.shotIntervalTicks());
	}

	@Test
	void magmaCubeMatchesDocumentSpec() {
		var unit = new MagmaCubeUnit();
		var definition = unit.definition();

		assertEquals(2, definition.cost());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:magma_cream", definition.mainHand().itemId);
		assertEquals(3.0D, unit.weaponAttackDamage());
		assertEquals(2.0D, unit.weaponAttackSpeed());
		assertEquals(1, unit.weaponFireAspectLevel());
		assertEquals(0.8D, unit.jumpStrengthValue());
		assertEquals(40, unit.fireResistanceDurationTicks());
		assertEquals(4, unit.spawnedMagmaCubeCount());
		assertEquals(2, unit.spawnedMagmaCubeSize());
	}

	@Test
	void piglinBruteMatchesDocumentSpec() {
		var unit = new PiglinBruteUnit();
		var definition = unit.definition();

		assertEquals(5, definition.cost());
		assertEquals(30.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:golden_axe", definition.mainHand().itemId);
		assertEquals("minecraft:golden_axe", definition.abilityItemSpec().itemId);
		assertEquals(24, definition.abilityCooldownSeconds());
		assertEquals(3, unit.supportGoldCount());
		assertEquals(1, unit.weaponSharpnessLevel());
		assertEquals(60, unit.buffDurationTicks());
	}
}
