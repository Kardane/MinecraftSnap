package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ElderGuardianUnit extends GuardianUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var target = linkedTarget(context);
		if (target != null) {
			target.addStatusEffect(new StatusEffectInstance(
				StatusEffects.MINING_FATIGUE,
				miningFatigueDurationTicks(),
				miningFatigueAmplifier(),
				false,
				false,
				true
			), context.player());
		}
	}

	@Override
	float reflectDamageAmount() {
		return 3.0f;
	}

	@Override
	double linkRange() {
		return 24.0D;
	}

	int miningFatigueDurationTicks() {
		return 40;
	}

	int miningFatigueAmplifier() {
		return 1;
	}
}
