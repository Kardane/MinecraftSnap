package karn.minecraftsnap.biome;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class SwampBiomeEffect extends NoOpBiomeEffect {
	private int lastAppliedSecond = -1;

	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		if (context.serverTicks() % 20L != 0L) {
			return;
		}
		var secondsSinceReveal = context.secondsSinceReveal();
		if (secondsSinceReveal <= 0 || secondsSinceReveal % 30 != 0 || secondsSinceReveal == lastAppliedSecond) {
			return;
		}
		lastAppliedSecond = secondsSinceReveal;
		for (var player : context.aliveUnitPlayers()) {
			var unitId = context.matchManager().getPlayerState(player.getUuid()).getCurrentUnitId();
			if (isImmune(unitId)) {
				continue;
			}
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0, true, true, true));
		}
	}

	static boolean isImmune(String unitId) {
		return "bogged".equals(unitId) || "slime".equals(unitId) || "giant_slime".equals(unitId);
	}
}
