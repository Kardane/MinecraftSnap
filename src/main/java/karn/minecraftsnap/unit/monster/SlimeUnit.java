package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SlimeUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"slime",
		"슬라임",
		FactionId.MONSTER,
		true,
		2,
		10,
		10.0,
		1.1,
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
		List.of("&7사망 시 작은 슬라임 3마리"),
		List.of(advanceOption(
			"slime_brute",
			"강화 슬라임",
			List.of("&7늪지에서 더 거대해짐"),
			List.of("minecraft:swamp", "minecraft:mangrove_swamp"),
			List.of("clear", "rain", "thunder"),
			12
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		var world = context.world();
		var player = context.player();
		for (int index = 0; index < 3; index++) {
			var slime = EntityType.SLIME.create(world, SpawnReason.MOB_SUMMONED);
			if (slime == null) {
				continue;
			}
			slime.setSize(1, true);
			slime.refreshPositionAndAngles(player.getX() + (index - 1), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			world.spawnEntity(slime);
		}
	}
}
