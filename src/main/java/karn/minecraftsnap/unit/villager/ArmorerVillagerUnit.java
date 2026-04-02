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
		"갑옷 대장장이 주민",
		FactionId.VILLAGER,
		true,
		2,
		20.0,
		0.8,
		item("minecraft:wooden_sword"),
		none(),
		item("minecraft:iron_helmet"),
		item("minecraft:iron_chestplate"),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:villager", "{VillagerData:{type:\"minecraft:plains\",profession:\"minecraft:armorer\",level:1}}"),
		List.of("&f무기 &7- 나무 검", "&f방어구 &7- 철 헬멧, 철 흉갑"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
