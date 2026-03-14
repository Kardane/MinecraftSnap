package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnitClassRegistryTest {
	@Test
	void validatesAllRegisteredUnitsHaveUnitClasses() {
		var registry = new UnitRegistry(false);
		for (var unitId : new String[] {
			"villager",
			"armorer_villager",
			"vindicator",
			"pillager",
			"zombie",
			"husk",
			"drowned",
			"skeleton",
			"stray",
			"bogged",
			"wither_skeleton",
			"slime",
			"giant_slime",
			"creeper",
			"charged_creeper",
			"piglin",
			"zombified_piglin",
			"blaze",
			"magma_cube",
			"piglin_brute"
		}) {
			registry.registerUnit(new UnitDefinition(
				unitId,
				unitId,
				FactionId.VILLAGER,
				true,
				0,
				0,
				20.0,
				1.0,
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				new karn.minecraftsnap.config.UnitItemEntry(),
				"",
				0,
				UnitDefinition.AmmoType.NONE,
				new karn.minecraftsnap.config.EntitySpecEntry(),
				java.util.List.of(),
				java.util.List.of()
			));
		}
		var unitClassRegistry = new UnitClassRegistry();

		assertDoesNotThrow(() -> unitClassRegistry.validateAgainst(registry));
	}

	@Test
	void throwsWhenUnitClassIsMissing() {
		var registry = new UnitRegistry(false);
		registry.registerUnit(new UnitDefinition(
			"missing",
			"누락",
			FactionId.VILLAGER,
			true,
			1,
			1,
			20.0,
			1.0,
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			new karn.minecraftsnap.config.UnitItemEntry(),
			"",
			0,
			UnitDefinition.AmmoType.NONE,
			new karn.minecraftsnap.config.EntitySpecEntry(),
			java.util.List.of(),
			java.util.List.of()
		));
		var unitClassRegistry = new UnitClassRegistry();

		assertThrows(IllegalStateException.class, () -> unitClassRegistry.validateAgainst(registry));
	}
}
