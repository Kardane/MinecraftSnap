package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SlimeBruteUnit extends SlimeUnit {
	public static final UnitDefinition DEFINITION = unit(
		"slime_brute",
		"강화 슬라임",
		FactionId.MONSTER,
		false,
		0,
		0,
		18.0,
		1.2,
		item("minecraft:slime_ball"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:slime"),
		List.of("&7늪 지형 전직 결과"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
