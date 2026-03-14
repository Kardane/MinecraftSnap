package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyEnchantment;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;
import static net.minecraft.enchantment.Enchantments.LOYALTY;

public class DrownedUnit extends ZombieUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"drowned",
		"드라운드",
		FactionId.MONSTER,
		false,
		3,
		10,
		18.0,
		1.0,
		item("minecraft:trident"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:drowned"),
		List.of("&7사망 시 아군 사령관 소환 쿨 3초 감소", "&7수중 호흡 무한, 물속 이동 강화"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyEnchantment(context.world(), context.player().getMainHandStack(), LOYALTY, 3);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var player = context.player();
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, false, false, false), player);
		if (player.isTouchingWater()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false), player);
		}
	}

	@Override
	int captainCooldownReductionOnDeathSeconds() {
		return 3;
	}
}
