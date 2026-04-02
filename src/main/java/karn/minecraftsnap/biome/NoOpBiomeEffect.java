package karn.minecraftsnap.biome;

public class NoOpBiomeEffect implements BiomeEffect {
	@Override
	public void onTick(BiomeRuntimeContext context) {
		// Pulse notifications were removed from biome runtime behavior.
	}
}
