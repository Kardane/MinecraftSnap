package karn.minecraftsnap.biome;

public class EndBiomeEffect extends NoOpBiomeEffect {
	@Override
	public void onReveal(BiomeRuntimeContext context) {
		karn.minecraftsnap.MinecraftSnap.LOGGER.info("[EndBiomeEffect] 엔드 바이옴 공개됨 - 경기 시간 60초 감소 시도");
		context.matchManager().reduceRemainingSeconds(60);
	}
}
