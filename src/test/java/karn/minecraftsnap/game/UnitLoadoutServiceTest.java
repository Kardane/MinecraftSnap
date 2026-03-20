package karn.minecraftsnap.game;

import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitLoadoutServiceTest {
	private final UnitLoadoutService service = new UnitLoadoutService();

	@Test
	void emptyAbilityItemUsesMainHandAsTrigger() {
		var definition = unit(
			"trigger_main",
			UnitItemEntry.create("minecraft:iron_axe"),
			new UnitItemEntry(),
			"도약",
			10
		).build();

		assertEquals(UnitLoadoutService.AbilityTriggerTarget.MAIN_HAND, service.determineAbilityTriggerTarget(definition));
	}

	@Test
	void identicalOffHandAbilityDoesNotCreateExtraTriggerItem() {
		var offHand = UnitItemEntry.create("minecraft:shield");
		var definition = unit(
			"trigger_offhand",
			UnitItemEntry.create("minecraft:wooden_sword"),
			offHand,
			"방패",
			5
		).withAbility(offHand).build();

		assertEquals(UnitLoadoutService.AbilityTriggerTarget.OFF_HAND, service.determineAbilityTriggerTarget(definition));
	}

	@Test
	void distinctAbilityItemCreatesSeparateTriggerItem() {
		var definition = unit(
			"trigger_extra",
			UnitItemEntry.create("minecraft:crossbow"),
			new UnitItemEntry(),
			"폭죽",
			15
		).withAbility(UnitItemEntry.create("minecraft:firework_rocket")).build();

		assertEquals(UnitLoadoutService.AbilityTriggerTarget.EXTRA_ITEM, service.determineAbilityTriggerTarget(definition));
	}

	@Test
	void fallbackMatchesMainHandAbilityTriggerWithoutTag() {
		var definition = unit(
			"trigger_main_fallback",
			UnitItemEntry.create("minecraft:iron_axe"),
			new UnitItemEntry(),
			"도약",
			10
		).build();

		assertTrue(service.matchesUnitAbilityTriggerItemId("minecraft:iron_axe", definition));
		assertFalse(service.matchesUnitAbilityTriggerItemId("minecraft:stick", definition));
	}

	@Test
	void fallbackMatchesSeparateAbilityItemWithoutTag() {
		var definition = unit(
			"trigger_extra_fallback",
			UnitItemEntry.create("minecraft:bow"),
			new UnitItemEntry(),
			"뼈 폭발",
			15
		).withAbility(UnitItemEntry.create("minecraft:bone")).build();

		assertTrue(service.matchesUnitAbilityTriggerItemId("minecraft:bone", definition));
		assertFalse(service.matchesUnitAbilityTriggerItemId("minecraft:bow", definition));
	}

	@Test
	void captainMenuFallsBackToBellWithoutTag() {
		assertTrue(service.matchesCaptainMenuTriggerItemId("minecraft:bell"));
		assertFalse(service.matchesCaptainMenuTriggerItemId("minecraft:stick"));
	}

	@Test
	void captainSkillFallsBackToNetherStarWithoutTag() {
		assertTrue(service.matchesCaptainSkillTriggerItemId("minecraft:nether_star"));
		assertFalse(service.matchesCaptainSkillTriggerItemId("minecraft:stick"));
	}

	private TestUnitDefinitionBuilder unit(String id, UnitItemEntry mainHand, UnitItemEntry offHand, String abilityName, int cooldown) {
		return new TestUnitDefinitionBuilder(id, mainHand, offHand, abilityName, cooldown);
	}

	private static final class TestUnitDefinitionBuilder {
		private final String id;
		private final UnitItemEntry mainHand;
		private final UnitItemEntry offHand;
		private final String abilityName;
		private final int cooldown;
		private UnitItemEntry abilityItem = new UnitItemEntry();

		private TestUnitDefinitionBuilder(String id, UnitItemEntry mainHand, UnitItemEntry offHand, String abilityName, int cooldown) {
			this.id = id;
			this.mainHand = mainHand;
			this.offHand = offHand;
			this.abilityName = abilityName;
			this.cooldown = cooldown;
		}

		private TestUnitDefinitionBuilder withAbility(UnitItemEntry abilityItem) {
			this.abilityItem = abilityItem;
			return this;
		}

		private UnitDefinition build() {
			return new UnitDefinition(
				id,
				id,
				FactionId.VILLAGER,
				true,
				0,
				20.0,
				1.0,
				mainHand,
				offHand,
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				abilityItem,
				abilityName,
				cooldown,
				UnitDefinition.AmmoType.NONE,
				new EntitySpecEntry(),
				List.of(),
				List.of()
			);
		}
	}
}
