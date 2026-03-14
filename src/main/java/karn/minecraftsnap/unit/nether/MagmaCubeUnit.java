package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyEnchantment;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;
import static net.minecraft.enchantment.Enchantments.FIRE_ASPECT;

public class MagmaCubeUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"magma_cube",
		"마그마 큐브",
		FactionId.NETHER,
		true,
		2,
		10,
		18.0,
		1.0,
		item("minecraft:magma_cream"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:magma_cube", "{Size:5}"),
		List.of("&7점프력 1 고정", "&7사망 시 사이즈 2 마그마 큐브 4마리 생성"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), definition().id(), weaponAttackDamage(), weaponAttackSpeed());
		applyEnchantment(context.world(), context.player().getMainHandStack(), FIRE_ASPECT, weaponFireAspectLevel());
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
			world.spawnEntity(magmaCube);
		}
	}

	double weaponAttackDamage() {
		return 3.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	int weaponFireAspectLevel() {
		return 1;
	}

	int spawnedMagmaCubeCount() {
		return 4;
	}

	int spawnedMagmaCubeSize() {
		return 2;
	}

	double jumpStrengthValue() {
		return 1.0D;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(jumpStrengthValue());
		}
	}
}
