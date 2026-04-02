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
		register("lush_cave", LushCaveBiomeEffect::new);
		register("mushroom_island", MushroomIslandBiomeEffect::new);
		register("cold_ocean", ColdOceanBiomeEffect::new);
		register("reverse_icicle", ReverseIcicleBiomeEffect::new);
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
		var effectType = normalizedEffectType(biomeEntry);
		return factories.getOrDefault(effectType, NoOpBiomeEffect::new).get();
	}

	private String normalizedEffectType(BiomeEntry biomeEntry) {
		if (biomeEntry.effectType != null
			&& !biomeEntry.effectType.isBlank()
			&& !"noop".equals(biomeEntry.effectType)) {
			return biomeEntry.effectType;
		}
		if (biomeEntry.id != null && factories.containsKey(biomeEntry.id)) {
			return biomeEntry.id;
		}
		return biomeEntry.effectType;
	}
}
