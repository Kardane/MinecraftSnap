package karn.minecraftsnap.config;

import karn.minecraftsnap.game.FactionId;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FactionUnitEntryTest {
	@Test
	void normalizesStructuredItemAndEntitySpecs() {
		var entry = new FactionUnitEntry();
		entry.mainHand = new UnitItemEntry();
		entry.offHand = new UnitItemEntry();
		entry.helmet = new UnitItemEntry();
		entry.chest = new UnitItemEntry();
		entry.legs = new UnitItemEntry();
		entry.boots = new UnitItemEntry();
		entry.abilityItem = new UnitItemEntry();
		entry.disguise = new EntitySpecEntry();
		entry.advanceOptions = new java.util.ArrayList<>(java.util.List.of(new AdvanceOptionEntry()));

		entry.normalize();

		assertEquals(1, entry.mainHand.count);
		assertEquals("", entry.mainHand.componentsNbt);
		assertEquals("", entry.mainHand.stackNbt);
		assertEquals("", entry.disguise.entityId);
		assertEquals("", entry.disguise.entityNbt);
		assertEquals(1, entry.advanceOptions.getFirst().requiredTicks);
	}

	@Test
	void convertsToUnitDefinitionWithoutAbilityPassiveEnums() {
		var entry = new FactionUnitEntry();
		entry.id = "villager";
		entry.displayName = "주민";
		entry.mainHand = UnitItemEntry.create("minecraft:wooden_sword");
		entry.offHand = new UnitItemEntry();
		entry.helmet = new UnitItemEntry();
		entry.chest = new UnitItemEntry();
		entry.legs = new UnitItemEntry();
		entry.boots = new UnitItemEntry();
		entry.abilityItem = UnitItemEntry.create("minecraft:bread");
		entry.disguise = EntitySpecEntry.create("minecraft:villager");
		entry.normalize();

		var definition = entry.toUnitDefinition(FactionId.VILLAGER);

		assertEquals("villager", definition.id());
		assertNotNull(definition.mainHand());
		assertNotNull(definition.disguise());
		assertEquals("minecraft:villager", definition.disguise().entityId);
	}

	@Test
	void resolvesGuiItemFromStackNbtWhenItemIdIsBlank() {
		var entry = new UnitItemEntry();
		entry.itemId = "";
		entry.stackNbt = "{id:\"minecraft:blaze_rod\",count:1}";
		entry.normalize();
		var resolvedId = new AtomicReference<String>();

		entry.resolve(itemId -> {
			resolvedId.set(itemId);
			return null;
		});

		assertEquals("minecraft:blaze_rod", resolvedId.get());
	}
}
