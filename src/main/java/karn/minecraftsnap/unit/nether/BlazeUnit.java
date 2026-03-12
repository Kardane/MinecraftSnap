package karn.minecraftsnap.unit.nether;

import net.minecraft.entity.projectile.SmallFireballEntity;

public class BlazeUnit extends AbstractNetherUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			var world = context.world();
			var player = context.player();
			var forward = player.getRotationVec(1.0f);
			var side = player.getRotationVector(0.0f, player.getYaw() + 90.0f).normalize().multiply(0.15D);
			for (int index = -1; index <= 1; index++) {
				var velocity = forward.add(side.multiply(index * 1.5D)).normalize();
				var fireball = new SmallFireballEntity(world, player, velocity);
				fireball.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYaw(), player.getPitch());
				world.spawnEntity(fireball);
			}
			return true;
		});
	}
}
