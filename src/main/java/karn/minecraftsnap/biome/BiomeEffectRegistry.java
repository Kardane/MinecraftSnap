package karn.minecraftsnap.biome;

import karn.minecraftsnap.config.BiomeEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BiomeEffectRegistry {
	private final Map<String, Supplier<BiomeEffect>> factories = new HashMap<>();

	public BiomeEffectRegistry() {
		register("noop", NoOpBiomeEffect::new);
		register("plain", PlainBiomeEffect::new);
		register("forest", ForestBiomeEffect::new);
		register("desert", DesertBiomeEffect::new);
		register("swamp", SwampBiomeEffect::new);
		register("badlands", BadlandsBiomeEffect::new);
		register("taiga", TaigaBiomeEffect::new);
		register("end", EndBiomeEffect::new);
		register("deep_dark", DeepDarkBiomeEffect::new);
		register("nether", NetherBiomeEffect::new);
	}

	public void register(String effectType, Supplier<BiomeEffect> factory) {
		if (effectType == null || effectType.isBlank() || factory == null) {
			return;
		}
		factories.put(effectType, factory);
	}

	public BiomeEffect create(BiomeEntry biomeEntry) {
		if (biomeEntry == null) {
			return new NoOpBiomeEffect();
		}
		return factories.getOrDefault(biomeEntry.effectType, NoOpBiomeEffect::new).get();
	}
}
