package karn.minecraftsnap.biome;

public class NoOpBiomeEffect implements BiomeEffect {
	@Override
	public void onTick(BiomeRuntimeContext context) {
		if (context.serverTicks() % 20L != 0L) {
			return;
		}
		var interval = context.biomeEntry().pulseIntervalSeconds;
		if (interval <= 0 || context.secondsSinceReveal() <= 0 || context.secondsSinceReveal() % interval != 0) {
			return;
		}
		context.broadcast(context.biomeEntry().pulseMessages);
		context.playSound(context.biomeEntry().pulseSoundId);
	}
}
