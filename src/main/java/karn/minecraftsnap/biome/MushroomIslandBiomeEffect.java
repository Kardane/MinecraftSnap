package karn.minecraftsnap.biome;

public class MushroomIslandBiomeEffect extends NoOpBiomeEffect {
	private static final int HEAL_INTERVAL_SECONDS = 3;
	private static final float HEAL_AMOUNT = 1.0F;
	private int lastHealedSecond = -1;

	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		var secondsSinceReveal = context.secondsSinceReveal();
		if (!shouldHeal(context.serverTicks(), secondsSinceReveal)) {
			return;
		}
		lastHealedSecond = secondsSinceReveal;
		for (var player : context.aliveUnitPlayers()) {
			player.heal(HEAL_AMOUNT);
		}
	}

	boolean shouldHeal(long serverTicks, int secondsSinceReveal) {
		return serverTicks % 20L == 0L
			&& secondsSinceReveal > 0
			&& secondsSinceReveal % HEAL_INTERVAL_SECONDS == 0
			&& secondsSinceReveal != lastHealedSecond;
	}
}
