package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.damage.DamageSource;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombieUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"zombie",
		"좀비",
		FactionId.MONSTER,
		true,
		1,
		20.0,
		0.8,
		item("minecraft:iron_shovel"),
		none(),
		item("minecraft:leather_helmet"),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:zombie"),
			List.of("&f패시브 &7- 유닛 사망시 마나 1 회복","&f무기 &7- 철 삽"),
		List.of(
			advanceOption(
				"husk",
				"허스크",
				List.of("&7사막/악지에서 20초 버티면 적응"),
				List.of("minecraft:desert", "minecraft:badlands"),
				List.of(),
				400
			),
			advanceOption(
				"drowned",
				"드라운드",
				List.of("&7바다에서 20초 버티면 적응"),
				List.of(
					"minecraft:ocean"
				),
				List.of(),
				400
			)
		)
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		context.restoreCaptainMana(captainManaRestoreOnDeath());
	}

	int captainManaRestoreOnDeath() {
		return 1;
	}
}
