package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ArmorerVillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"armorer_villager",
		"대장장이 주민",
		FactionId.VILLAGER,
		true,
		2,
		9,
		30.0,
		0.9,
		item("minecraft:wooden_sword"),
		item("minecraft:shield"),
		none(),
		item("minecraft:iron_chestplate"),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:villager"),
		List.of("&7방패와 흉갑 보유", "&7전선 유지용"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
