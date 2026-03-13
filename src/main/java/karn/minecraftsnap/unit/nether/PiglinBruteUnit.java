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

public class PiglinBruteUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"piglin_brute",
		"피글린 브루트",
		FactionId.NETHER,
		true,
		6,
		25,
		40.0,
		1.0,
		item("minecraft:golden_axe"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"광란",
		30,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:piglin_brute"),
		List.of("&7자가 강화 폭발력"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 10, 1));
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 10, 0));
			return true;
		});
	}
}
