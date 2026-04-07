package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SilverfishUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void onAttack(UnitContext context, net.minecraft.entity.LivingEntity victim, float amount) {
		if (!shouldSummonOnAttack(context.isEnemyTarget(victim))) {
			return;
		}
		spawnSilverfish(context, victim);
	}

	boolean shouldSummonOnAttack(boolean enemyTarget) {
		return enemyTarget;
	}

	private void spawnSilverfish(UnitContext context, net.minecraft.entity.LivingEntity target) {
		if (context.player() == null || context.world() == null) {
			return;
		}
		for (int index = 0; index < spawnedSilverfishCount(); index++) {
			var silverfish = EntityType.SILVERFISH.create(context.world(), SpawnReason.MOB_SUMMONED);
			if (silverfish == null) {
				continue;
			}
			var offset = randomSummonOffset(context.player().getRandom().nextDouble(), context.player().getRandom().nextDouble());
			if (!SummonedMobSupport.placeMobSafely(
				context.world(),
				silverfish,
				context.player().getX() + offset.x,
				context.player().getY(),
				context.player().getZ() + offset.z,
				context.player().getYaw(),
				context.player().getPitch()
			)) {
				continue;
			}
			silverfish.setHealth(summonedSilverfishHealth());
			silverfish.setCustomName(context.player().getName().copy());
			SummonedMobSupport.applyFriendlyTeam(context, silverfish);
			silverfish.setTarget(target);
			context.world().spawnEntity(silverfish);
		}
	}

	int spawnedSilverfishCount() {
		return 1;
	}

	float summonedSilverfishHealth() {
		return 0.1F;
	}

	double summonRange() {
		return 5.0D;
	}

	Vec3d randomSummonOffset(Random random) {
		return randomSummonOffset(random.nextDouble(), random.nextDouble());
	}

	private Vec3d randomSummonOffset(double angleRoll, double distanceRoll) {
		var angle = angleRoll * Math.PI * 2.0D;
		var distance = distanceRoll * summonRange();
		return new Vec3d(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
	}
}
