package karn.minecraftsnap.biome;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Random;

public class SwampBiomeEffect extends NoOpBiomeEffect {
	private static final int EFFECT_DURATION_TICKS = 200;
	private static final int EFFECT_AMPLIFIER = 0;
	private final Random random;
	private int lastAppliedSecond = -1;

	public SwampBiomeEffect() {
		this(new Random());
	}

	SwampBiomeEffect(Random random) {
		this.random = random;
	}

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
		context.playSound("minecraft:entity.frog.ambient", 0.8f, 0.9f);
		for (var player : context.aliveCaptureUnitPlayers()) {
			var unitId = context.matchManager().getPlayerState(player.getUuid()).getCurrentUnitId();
			if (isImmune(unitId)) {
				continue;
			}
			player.addStatusEffect(randomEffect(random), player);
		}
	}

	static StatusEffectInstance randomEffect(Random random) {
		var effects = supportedEffects();
		var effect = effects.get(random.nextInt(effects.size()));
		return new StatusEffectInstance(effect, EFFECT_DURATION_TICKS, EFFECT_AMPLIFIER, true, true, true);
	}

	static List<RegistryEntry<net.minecraft.entity.effect.StatusEffect>> supportedEffects() {
		return List.of(
			StatusEffects.POISON,
			StatusEffects.SLOWNESS,
			StatusEffects.WEAKNESS,
			StatusEffects.BLINDNESS
		);
	}

	static boolean isImmune(String unitId) {
		return "bogged".equals(unitId) || "slime".equals(unitId) || "giant_slime".equals(unitId);
	}
}
