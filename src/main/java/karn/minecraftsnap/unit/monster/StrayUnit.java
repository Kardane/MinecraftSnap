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

public class StrayUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"stray",
		"스트레이",
		FactionId.MONSTER,
		false,
		4,
		16.0,
		0.8,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:stray"),
			List.of("&f패시브 &7- 공격시 구속을 부여합니다.","&f무기 &7- 활"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim)) {
			return;
		}
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, statusDurationTicks(), effectAmplifier()), context.player());
	}

	int statusDurationTicks() {
		return 20 * 2;
	}

	int effectAmplifier() {
		return 1;
	}
}
