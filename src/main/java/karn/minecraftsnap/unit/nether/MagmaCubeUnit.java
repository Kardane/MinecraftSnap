package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class MagmaCubeUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyJumpStrength(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		applyJumpStrength(context);
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
	}

	@Override
	public void onDeath(UnitContext context, DamageSource source) {
		var world = context.world();
		var player = context.player();
		for (int index = 0; index < spawnedMagmaCubeCount(); index++) {
			var magmaCube = EntityType.MAGMA_CUBE.create(world, SpawnReason.MOB_SUMMONED);
			if (magmaCube == null) {
				continue;
			}
			magmaCube.setSize(spawnedMagmaCubeSize(), true);
			magmaCube.refreshPositionAndAngles(player.getX() + (index - 1.5D), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			magmaCube.setCustomName(player.getName().copy());
			SummonedMobSupport.applyFriendlyTeam(context, magmaCube);
			world.spawnEntity(magmaCube);
		}
	}

	double weaponAttackDamage() {
		return 3.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	int spawnedMagmaCubeCount() {
		return 4;
	}

	int spawnedMagmaCubeSize() {
		return 2;
	}

	double jumpStrengthValue() {
		return 0.8D;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(context.unitDefinition().extraAttributes().jumpStrengthOrDefault(jumpStrengthValue()));
		}
	}
}
