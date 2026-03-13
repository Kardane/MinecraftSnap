package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

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
		2,
		13,
		14.0,
		1.0,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:bone", "뼈 폭발", 15),
		"뼈 폭발",
		15,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:skeleton"),
		List.of("&74칸 내 적 둔화 타격"),
		List.of(advanceOption(
			"skeleton_sniper",
			"강화 스켈레톤",
			List.of("&7설원 계열에서 정찰병화"),
			List.of("minecraft:snowy_plains", "minecraft:snowy_taiga"),
			List.of("clear", "rain"),
			15
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			for (var target : nearbyEnemyPlayers(context, 4.0D)) {
				context.dealMobDamage(target, 6.0f);
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 1), context.player());
			}
			return true;
		});
	}

	private java.util.List<ServerPlayerEntity> nearbyEnemyPlayers(UnitContext context, double radius) {
		var player = context.player();
		var squaredRadius = radius * radius;
		return player.getServer().getPlayerManager().getPlayerList().stream()
			.filter(target -> target.getWorld() == player.getWorld())
			.filter(target -> !target.getUuid().equals(player.getUuid()))
			.filter(target -> player.squaredDistanceTo(target) <= squaredRadius)
			.filter(context::isEnemyUnit)
			.toList();
	}
}
