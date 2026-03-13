package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.server.network.ServerPlayerEntity;

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
		6,
		20.0,
		0.8,
		item("minecraft:iron_shovel"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:zombie"),
		List.of("&7적 처치 시 사령관 소환 쿨 2초 감소"),
		List.of(advanceOption(
			"zombie_veteran",
			"강화 좀비",
			List.of("&7늪과 비를 버티면 강화"),
			List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
			List.of("rain", "thunder"),
			15
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onKill(karn.minecraftsnap.unit.UnitContext context, ServerPlayerEntity victim) {
		context.reduceCaptainSpawnCooldown(2);
	}
}
