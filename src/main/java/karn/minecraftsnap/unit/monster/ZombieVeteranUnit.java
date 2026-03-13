package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombieVeteranUnit extends ZombieUnit {
	public static final UnitDefinition DEFINITION = unit(
		"zombie_veteran",
		"강화 좀비",
		FactionId.MONSTER,
		false,
		0,
		0,
		26.0,
		0.95,
		item("minecraft:iron_shovel"),
		none(),
		none(),
		item("minecraft:iron_chestplate"),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:husk"),
		List.of("&7늪/비 조건 전직 결과"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
