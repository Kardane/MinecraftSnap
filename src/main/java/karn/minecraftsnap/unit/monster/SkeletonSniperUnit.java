package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SkeletonSniperUnit extends SkeletonUnit {
	public static final UnitDefinition DEFINITION = unit(
		"skeleton_sniper",
		"강화 스켈레톤",
		FactionId.MONSTER,
		false,
		0,
		0,
		18.0,
		1.05,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:bone", "뼈 폭발", 12),
		"뼈 폭발",
		12,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:stray"),
		List.of("&7설원 계열 전직 결과"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
