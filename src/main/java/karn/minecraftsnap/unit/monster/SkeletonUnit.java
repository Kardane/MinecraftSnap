package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SkeletonUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"skeleton",
		"스켈레톤",
		FactionId.MONSTER,
		true,
		3,
		16.0,
		0.8,
		item("minecraft:bow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:skeleton"),
		List.of("&f무기 &7- 활"),
		List.of(
			advanceOption(
				"stray",
				"스트레이",
				List.of("&7타이가에서 20초 버티면 적응"),
				List.of("minecraft:taiga", "minecraft:snowy_taiga"),
				List.of(),
				400
			),
			advanceOption(
				"bogged",
				"보그드",
				List.of("&7늪에서 20초 버티면 적응"),
				List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
				List.of(),
				400
			)
		)
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}
}
