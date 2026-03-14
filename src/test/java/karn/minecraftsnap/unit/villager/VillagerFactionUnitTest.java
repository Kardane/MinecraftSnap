package karn.minecraftsnap.unit.villager;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerFactionUnitTest {
	@Test
	void villagerMatchesDocumentSpec() {
		var unit = new VillagerUnit();
		var definition = unit.definition();

		assertEquals("멍청이 주민", definition.displayName());
		assertEquals(1, definition.cost());
		assertEquals(5, definition.spawnCooldownSeconds());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(0.8, definition.moveSpeedScale());
		assertEquals("minecraft:wooden_sword", definition.mainHand().itemId);
		assertEquals("minecraft:bread", definition.offHand().itemId);
		assertEquals("minecraft:bread", definition.abilityItemSpec().itemId);
		assertEquals(8, definition.abilityCooldownSeconds());
		assertEquals(5.0f, unit.healAmount());
	}

	@Test
	void armorerMatchesDocumentSpec() {
		var unit = new ArmorerVillagerUnit();
		var definition = unit.definition();

		assertEquals("갑옷 대장장이 주민", definition.displayName());
		assertEquals(2, definition.cost());
		assertEquals(10, definition.spawnCooldownSeconds());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(0.8, definition.moveSpeedScale());
		assertEquals("minecraft:wooden_sword", definition.mainHand().itemId);
		assertEquals("minecraft:shield", definition.offHand().itemId);
		assertEquals("", definition.helmet().itemId);
		assertEquals("minecraft:iron_chestplate", definition.chest().itemId);
		assertEquals("", definition.legs().itemId);
		assertEquals("", definition.boots().itemId);
		assertFalse(definition.hasActiveSkill());
	}

	@Test
	void pillagerMatchesDocumentSpecAndCreatesSingleRocketPayload() {
		var unit = new PillagerUnit();
		var definition = unit.definition();
		var fireworks = unit.skillRocketPayload(new Random(1234L));

		assertEquals("약탈자", definition.displayName());
		assertEquals(3, definition.cost());
		assertEquals(15, definition.spawnCooldownSeconds());
		assertEquals(18.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:crossbow", definition.mainHand().itemId);
		assertEquals("", definition.offHand().itemId);
		assertEquals("minecraft:firework_star", definition.abilityItemSpec().itemId);
		assertEquals(10, definition.abilityCooldownSeconds());
		assertNotNull(fireworks);
		assertEquals(1, fireworks.flightDuration());
		assertEquals(2, fireworks.explosions().size());
		assertFalse(fireworks.explosions().getFirst().colors().isEmpty());
	}

	@Test
	void vindicatorMatchesDocumentSpec() {
		var unit = new VindicatorUnit();
		var definition = unit.definition();

		assertEquals("변명자", definition.displayName());
		assertEquals(4, definition.cost());
		assertEquals(25, definition.spawnCooldownSeconds());
		assertEquals(26.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:iron_axe", definition.mainHand().itemId);
		assertEquals("minecraft:iron_axe", definition.abilityItemSpec().itemId);
		assertEquals(8, definition.abilityCooldownSeconds());
		assertEquals(0.8D, unit.dashHorizontalStrength());
		assertEquals(0.1D, unit.dashVerticalBoost());
	}
}
