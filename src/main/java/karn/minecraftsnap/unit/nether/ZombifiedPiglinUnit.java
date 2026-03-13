package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombifiedPiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"zombified_piglin",
		"좀비 피글린",
		FactionId.NETHER,
		true,
		2,
		8,
		20.0,
		0.8,
		item("minecraft:golden_sword"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"분노",
		15,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:zombified_piglin"),
		List.of("&7주변 아군 강화"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 1));
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 0));
			return true;
		});
	}
}
