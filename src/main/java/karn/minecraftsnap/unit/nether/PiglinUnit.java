package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"piglin",
		"피글린",
		FactionId.NETHER,
		true,
		2,
		5,
		20.0,
		1.0,
		item("minecraft:golden_sword"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:piglin"),
		List.of("&750% 확률로 좀비 피글린 생성"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		if (context.player().getRandom().nextFloat() >= 0.5f) {
			return;
		}
		var world = context.world();
		var player = context.player();
		var piglin = EntityType.ZOMBIFIED_PIGLIN.create(world, SpawnReason.MOB_SUMMONED);
		if (piglin == null) {
			return;
		}
		piglin.refreshPositionAndAngles(player.getBlockPos(), player.getYaw(), player.getPitch());
		world.spawnEntity(piglin);
	}
}
