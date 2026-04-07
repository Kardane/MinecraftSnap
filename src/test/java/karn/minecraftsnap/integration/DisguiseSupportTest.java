package karn.minecraftsnap.integration;

import karn.minecraftsnap.config.EntitySpecEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisguiseSupportTest {
	@Test
	void plainDisguisesPreferEntityTypeApplication() {
		assertFalse(DisguiseSupport.shouldCreateDisguiseEntity(EntitySpecEntry.create("minecraft:piglin_brute"), false));
	}

	@Test
	void nbtAndCustomizedDisguisesStillRequireEntityInstances() {
		var nbtDisguise = EntitySpecEntry.create("minecraft:creeper");
		nbtDisguise.entityNbt = "{powered:1b}";

		assertTrue(DisguiseSupport.shouldCreateDisguiseEntity(nbtDisguise, false));
		assertTrue(DisguiseSupport.shouldCreateDisguiseEntity(EntitySpecEntry.create("minecraft:iron_golem"), true));
	}
}
