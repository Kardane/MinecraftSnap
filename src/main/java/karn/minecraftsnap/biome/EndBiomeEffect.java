package karn.minecraftsnap.biome;

public class EndBiomeEffect extends NoOpBiomeEffect {
	@Override
	public void onReveal(BiomeRuntimeContext context) {
		context.matchManager().reduceRemainingSeconds(60);
	}
}
