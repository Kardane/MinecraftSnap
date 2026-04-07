package karn.minecraftsnap.biome;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class LushCaveBiomeEffect extends NoOpBiomeEffect {
	private static final int REGEN_INTERVAL_SECONDS = 30;
	private static final int REGEN_DURATION_TICKS = 200;
	private static final int REGEN_AMPLIFIER = 1;
	private int lastAppliedSecond = -1;

	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		var secondsSinceReveal = context.secondsSinceReveal();
		if (!shouldApplyRegen(context.serverTicks(), secondsSinceReveal)) {
			return;
		}
		lastAppliedSecond = secondsSinceReveal;
		for (var player : context.aliveUnitPlayers()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, REGEN_DURATION_TICKS, REGEN_AMPLIFIER, true, true, true), player);
		}
	}

	boolean shouldApplyRegen(long serverTicks, int secondsSinceReveal) {
		return serverTicks % 20L == 0L
			&& secondsSinceReveal > 0
			&& secondsSinceReveal % REGEN_INTERVAL_SECONDS == 0
			&& secondsSinceReveal != lastAppliedSecond;
	}
}
