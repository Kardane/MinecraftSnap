package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.projectile.SmallFireballEntity;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class BlazeUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"blaze",
		"블레이즈",
		FactionId.NETHER,
		true,
		3,
		16,
		16.0,
		1.2,
		item("minecraft:blaze_rod"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"화염구",
		5,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:blaze"),
		List.of("&7작은 화염구 3연사"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

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
