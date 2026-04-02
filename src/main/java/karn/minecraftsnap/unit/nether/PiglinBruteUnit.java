package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyEnchantment;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;
import static net.minecraft.enchantment.Enchantments.SHARPNESS;

public class PiglinBruteUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"piglin_brute",
		"피글린 브루트",
		FactionId.NETHER,
		true,
		3,
		30.0,
		1.0,
		item("minecraft:golden_axe"),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		karn.minecraftsnap.unit.UnitSpecSupport.none(),
		item("minecraft:golden_axe"),
		"광란",
		24,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:piglin_brute"),
			List.of("&f광란&7- 힘, 저항, 신속 효과를 얻습니다.","&f무기 &7- 금 도끼"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyEnchantment(context.world(), context.player().getMainHandStack(), SHARPNESS, weaponSharpnessLevel());
		//context.player().getInventory().insertStack(new ItemStack(Items.GOLD_INGOT, supportGoldCount()));
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			//context.player().clearStatusEffects();
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, buffDurationTicks(), 0));
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, buffDurationTicks(), 0));
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, buffDurationTicks(), 0));
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, buffDurationTicks(), 0));
			return true;
		});
	}

	int supportGoldCount() {
		return 3;
	}

	int weaponSharpnessLevel() {
		return 1;
	}

	int buffDurationTicks() {
		return 20 * 3;
	}
}
