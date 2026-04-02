package karn.minecraftsnap.game;

import karn.minecraftsnap.config.UnitConfigEntry;
import karn.minecraftsnap.config.UnitExtraAttributes;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.config.TextConfigFile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnitRegistryTest {
	@Test
	void loadsUnitsFromJavaDefinitions() {
		var registry = new UnitRegistry();

		assertEquals(28, registry.all().size());
		assertEquals(7, registry.byFaction(FactionId.VILLAGER).size());
		assertEquals(13, registry.allByFaction(FactionId.MONSTER).size());
		assertEquals(7, registry.byFaction(FactionId.MONSTER).size());
		assertEquals(8, registry.byFaction(FactionId.NETHER).size());
		assertEquals(3, registry.get("enderman").cost());
		assertFalse(registry.get("villager").descriptionLines().isEmpty());
		assertEquals("주민&우민", registry.getFactionSpec(FactionId.VILLAGER).displayName());
		assertFalse(registry.get("zombie").advanceOptions().isEmpty());
	}

	@Test
	void textConfigDoesNotOverrideUnitDescriptions() {
		var registry = new UnitRegistry();
		var original = registry.get("villager").descriptionLines();
		var textConfig = new TextConfigFile();

		registry.applyTextConfig(textConfig);

		assertEquals(original, registry.get("villager").descriptionLines());
	}

	@Test
	void unitConfigOverridesRuntimeDefinitionFields() {
		var registry = new UnitRegistry();
		var config = UnitConfigEntry.from(registry.get("villager"));
		config.displayName = "테스트 주민";
		config.cost = 7;
		config.maxHealth = 24.0;
		config.moveSpeedScale = 1.25;
		config.abilityCooldownSeconds = 13;
		config.descriptionLines = java.util.List.of("&7설명");
		config.mainHand = UnitItemEntry.create("minecraft:stone_sword");
		config.abilityItem = UnitItemEntry.create("minecraft:apple");
		config.attributes = new UnitExtraAttributes();
		config.attributes.jumpStrength = 0.7D;

		registry.applyUnitConfigs(Map.of("villager", config));

		var definition = registry.get("villager");
		assertEquals("테스트 주민", definition.displayName());
		assertEquals(7, definition.cost());
		assertEquals(24.0, definition.maxHealth());
		assertEquals(1.25, definition.moveSpeedScale());
		assertEquals(13, definition.abilityCooldownSeconds());
		assertEquals("&7설명", definition.descriptionLines().getFirst());
		assertEquals("minecraft:stone_sword", definition.mainHand().itemId);
		assertEquals("minecraft:apple", definition.abilityItemSpec().itemId);
		assertEquals(0.7D, definition.extraAttributes().jumpStrength);
	}
}
