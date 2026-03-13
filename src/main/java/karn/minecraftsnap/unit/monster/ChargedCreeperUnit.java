package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ChargedCreeperUnit extends CreeperUnit {
	public static final UnitDefinition DEFINITION = unit(
		"charged_creeper",
		"대전된 크리퍼",
		FactionId.MONSTER,
		false,
		0,
		0,
		24.0,
		1.05,
		item("minecraft:tnt"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"자폭",
		15,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:creeper"),
		List.of("&7천둥 조건 전직 결과"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
