package karn.minecraftsnap.unit.nether;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class ZombifiedPiglinUnit extends AbstractNetherUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.applyEffects(
			new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 1),
			new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 0)
		));
	}
}
