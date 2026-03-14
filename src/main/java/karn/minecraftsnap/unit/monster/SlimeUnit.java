package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SlimeUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"slime",
		"슬라임",
		FactionId.MONSTER,
		true,
		2,
		8,
		14.0,
		1.0,
		item("minecraft:slime_ball"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:slime", "{Size:3}"),
		List.of("&7점프력 1 고정", "&7사망 시 사이즈 2 슬라임 3마리 생성"),
		List.of(advanceOption(
			"giant_slime",
			"거대 슬라임",
			List.of("&7늪에서 15초 버티면 적응"),
			List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
			List.of(),
			300
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), definition().id(), weaponAttackDamage(), weaponAttackSpeed());
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
	}

	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		var world = context.world();
		var player = context.player();
		for (int index = 0; index < spawnedSlimeCount(); index++) {
			var slime = EntityType.SLIME.create(world, SpawnReason.MOB_SUMMONED);
			if (slime == null) {
				continue;
			}
			slime.setSize(spawnedSlimeSize(), true);
			slime.refreshPositionAndAngles(player.getX() + (index - 1), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			slime.setCustomName(player.getName().copy());
			world.spawnEntity(slime);
		}
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(jumpStrengthValue());
		}
	}

	int spawnedSlimeCount() {
		return 4;
	}

	int spawnedSlimeSize() {
		return 2;
	}

	double weaponAttackDamage() {
		return 3.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	double jumpStrengthValue() {
		return 1.0D;
	}
}
