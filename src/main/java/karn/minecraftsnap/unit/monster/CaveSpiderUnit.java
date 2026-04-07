package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class CaveSpiderUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim) || !shouldApplyPoison(ThreadLocalRandom.current().nextDouble())) {
			return;
		}
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poisonDurationTicks(), 0), context.player());
	}

	boolean shouldApplyPoison(double roll) {
		return roll < poisonChance();
	}

	double poisonChance() {
		return 0.75D;
	}

	int poisonDurationTicks() {
		return 20 * 5;
	}
}
