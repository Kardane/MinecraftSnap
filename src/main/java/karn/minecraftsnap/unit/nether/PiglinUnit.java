package karn.minecraftsnap.unit.nether;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;

public class PiglinUnit extends AbstractNetherUnit {
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
