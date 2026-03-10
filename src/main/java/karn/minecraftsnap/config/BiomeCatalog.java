package karn.minecraftsnap.config;

import java.util.ArrayList;
import java.util.List;

public class BiomeCatalog {
	public List<BiomeEntry> biomes = new ArrayList<>();

	public void normalize() {
		if (biomes == null) {
			biomes = new ArrayList<>();
			return;
		}
		biomes.forEach(BiomeEntry::normalize);
	}
}
