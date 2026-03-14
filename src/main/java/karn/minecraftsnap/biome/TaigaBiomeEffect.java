package karn.minecraftsnap.biome;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class TaigaBiomeEffect extends NoOpBiomeEffect {
	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		for (var player : context.aliveUnitPlayers()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, true));
		}
	}
}
