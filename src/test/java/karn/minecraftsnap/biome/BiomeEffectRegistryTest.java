package karn.minecraftsnap.biome;

import karn.minecraftsnap.config.BiomeEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiomeEffectRegistryTest {
	@Test
	void createsRegisteredBiomeEffectFromEffectType() {
		var registry = new BiomeEffectRegistry();
		var entry = new BiomeEntry();
		entry.effectType = "forest";

		assertEquals(ForestBiomeEffect.class, registry.create(entry).getClass());
	}

	@Test
	void fallsBackToNoOpWhenEffectTypeIsMissing() {
		var registry = new BiomeEffectRegistry();
		var entry = new BiomeEntry();
		entry.effectType = "missing";

		assertEquals(NoOpBiomeEffect.class, registry.create(entry).getClass());
	}
}
