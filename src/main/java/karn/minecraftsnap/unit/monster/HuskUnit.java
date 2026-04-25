package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class HuskUnit extends ZombieUnit {
	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim)) {
			return;
		}
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, statusDurationTicks(), 0), context.player());
		//victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, statusDurationTicks(), 0), context.player());
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		if (isHotDryBiome(context.currentBiomeId())) {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false, false));
		}
	}

	boolean isHotDryBiome(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return false;
		}
		return biomeId.contains("desert") || biomeId.contains("badlands");
	}

	int statusDurationTicks() {
		return 20 * 3;
	}
}
