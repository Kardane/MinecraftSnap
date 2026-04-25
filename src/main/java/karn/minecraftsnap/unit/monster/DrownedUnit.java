package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class DrownedUnit extends ZombieUnit {
	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
		discardOwnedTridents(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var player = context.player();
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, false, false, false), player);
		if (player.isTouchingWater()) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false), player);
		}
		recallLowTridents(context);
	}

	boolean shouldRecallTrident(double y) {
		return y <= 0.0D;
	}

	private void recallLowTridents(UnitContext context) {
		var world = context.world();
		var player = context.player();
		if (world == null || player == null) {
			return;
		}
		var box = player.getBoundingBox().expand(96.0D);
		for (var trident : world.getEntitiesByClass(TridentEntity.class, box, entity -> entity.getOwner() == player && shouldRecallTrident(entity.getY()))) {
			var target = player.getEyePos().subtract(trident.getPos()).normalize();
			trident.setVelocity(target.multiply(1.5D));
			trident.setPos(trident.getX(), Math.max(player.getY(), 1.0D), trident.getZ());
			trident.velocityModified = true;
		}
	}

	private void discardOwnedTridents(UnitContext context) {
		var world = context.world();
		var player = context.player();
		if (world == null || player == null) {
			return;
		}
		var box = player.getBoundingBox().expand(128.0D);
		for (var trident : world.getEntitiesByClass(TridentEntity.class, box, entity -> entity.getOwner() == player)) {
			trident.discard();
		}
	}
}
