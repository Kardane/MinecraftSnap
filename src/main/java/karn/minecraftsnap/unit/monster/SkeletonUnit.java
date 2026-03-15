package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SkeletonUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"skeleton",
		"스켈레톤",
		FactionId.MONSTER,
		true,
		3,
		12,
		16.0,
		0.9,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:bone", "뼈 폭발", 12),
		"뼈 폭발",
		12,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:skeleton"),
			List.of("&f뼈 폭발 &7- 주변 적에게 피해를 입히고 밀쳐냅니다.","&f무기 &7- 활"),
		List.of(
			advanceOption(
				"stray",
				"스트레이",
				List.of("&7타이가에서 20초 버티면 적응"),
				List.of("minecraft:taiga", "minecraft:snowy_taiga"),
				List.of(),
				400
			),
			advanceOption(
				"bogged",
				"보그드",
				List.of("&7늪에서 20초 버티면 적응"),
				List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
				List.of(),
				400
			),
			advanceOption(
				"wither_skeleton",
				"위더 스켈레톤",
				List.of("&7네더에서 30초 버티면 적응"),
				List.of(
					"minecraft:nether_wastes",
					"minecraft:soul_sand_valley",
					"minecraft:crimson_forest",
					"minecraft:warped_forest",
					"minecraft:basalt_deltas"
				),
				List.of(),
				600
			)
		)
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			for (var target : nearbyEnemyTargets(context, boneBlastRadius())) {
				context.dealMobDamage(target, boneBlastDamage());
				target.takeKnockback(1D, player.getX() - target.getX(), player.getZ() - target.getZ());
			}
			player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
			return true;
		});
	}

	private java.util.List<LivingEntity> nearbyEnemyTargets(UnitContext context, double radius) {
		var player = context.player();
		var squaredRadius = radius * radius;
		return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius), target ->
			player.squaredDistanceTo(target) <= squaredRadius && context.isEnemyTarget(target));
	}

	double boneBlastRadius() {
		return 4.0D;
	}

	float boneBlastDamage() {
		return 5.0f;
	}
}
