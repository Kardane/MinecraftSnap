package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class VindicatorUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"vindicator",
		"변명자",
		FactionId.VILLAGER,
		true,
		5,
		25,
		26.0,
		1.0,
		item("minecraft:iron_axe"),
		none(),
		none(),
		none(),
		none(),
		none(),
		item("minecraft:iron_axe"),
		"돌진",
		8,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:vindicator"),
			List.of("&f무기 &7- 철 도끼","&f돌진 &7- 앞으로 살짝 돌진합니다."),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			var dash = player.getRotationVec(1.0f).multiply(dashHorizontalStrength());
			player.addVelocity(dash.x, dashVerticalBoost(), dash.z);
			player.velocityModified = true;
			player.getWorld().spawnParticles(
				ParticleTypes.SWEEP_ATTACK,
				player.getX(),
				player.getBodyY(0.5D),
				player.getZ(),
				6,
				0.15D,
				0.1D,
				0.15D,
				0.0D
			);
			player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 0.9f);
			return true;
		});
	}

	double dashHorizontalStrength() {
		return 0.5D;
	}

	double dashVerticalBoost() {
		return 0.1D;
	}
}
