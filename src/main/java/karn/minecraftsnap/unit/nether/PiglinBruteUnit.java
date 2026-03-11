package karn.minecraftsnap.unit.nether;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class PiglinBruteUnit extends AbstractNetherUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.applyEffects(
			new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 10, 1),
			new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 10, 0)
		));
	}
}
