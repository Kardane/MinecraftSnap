package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class WitherSkeletonUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
		context.player().removeStatusEffect(StatusEffects.WITHER);
	}

	boolean isWitherImmune() {
		return true;
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim)) {
			return;
		}
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, statusDurationTicks(), effectAmplifier()), context.player());
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var world = context.world();
			var player = context.player();
			var direction = player.getRotationVec(1.0f).normalize();
			var skull = EntityType.WITHER_SKULL.create(world, SpawnReason.MOB_SUMMONED);
			if (skull == null) {
				return false;
			}
			skull.setOwner(player);
			skull.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.2D, player.getZ(), player.getYaw(), player.getPitch());
			skull.setVelocity(direction.multiply(1.35D));
			world.spawnEntity(skull);
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		});
	}

	int statusDurationTicks() {
		return 20 * 3;
	}

	int effectAmplifier() {
		return 1;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}
}
