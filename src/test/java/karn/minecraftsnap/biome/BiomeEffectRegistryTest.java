package karn.minecraftsnap.biome;

import karn.minecraftsnap.config.BiomeEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiomeEffectRegistryTest {
	@Test
	void createsRegisteredBiomeEffectFromEffectType() {
		var registry = new BiomeEffectRegistry();
		var entry = new BiomeEntry();
		entry.effectType = "plain";

		assertEquals(PlainBiomeEffect.class, registry.create(entry).getClass());
	}

	@Test
	void createsExtendedBiomeEffects() {
		var registry = new BiomeEffectRegistry();
		var taiga = new BiomeEntry();
		taiga.effectType = "taiga";
		var end = new BiomeEntry();
		end.effectType = "end";
		var deepDark = new BiomeEntry();
		deepDark.effectType = "deep_dark";
		var nether = new BiomeEntry();
		nether.effectType = "nether";

		assertEquals(TaigaBiomeEffect.class, registry.create(taiga).getClass());
		assertEquals(EndBiomeEffect.class, registry.create(end).getClass());
		assertEquals(DeepDarkBiomeEffect.class, registry.create(deepDark).getClass());
		assertEquals(NetherBiomeEffect.class, registry.create(nether).getClass());
	}

	@Test
	void fallsBackToNoOpWhenEffectTypeIsMissing() {
		var registry = new BiomeEffectRegistry();
		var entry = new BiomeEntry();
		entry.effectType = "missing";

		assertEquals(NoOpBiomeEffect.class, registry.create(entry).getClass());
	}
}
