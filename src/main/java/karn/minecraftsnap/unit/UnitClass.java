package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.LaneId;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public interface UnitClass {
	default void onTick(UnitContext context) {
	}

	default void onAttack(UnitContext context, LivingEntity victim, float amount) {
	}

	default void onDamaged(UnitContext context, DamageSource source, float amount) {
	}

	default void onDeath(UnitContext context, DamageSource source) {
	}

	default void onKill(UnitContext context, ServerPlayerEntity victim) {
	}

	default void onSkillUse(UnitContext context) {
	}

	default void onLaneEnter(UnitContext context, LaneId previousLaneId, LaneId currentLaneId) {
	}

	default void onShiftF(UnitContext context) {
	}

	default void onCaptureTick(UnitContext context) {
	}

	default void onCaptureScore(UnitContext context) {
	}

	default void onProjectileHit(UnitContext context, ProjectileEntity projectile, Entity target) {
	}

	default void onProjectileImpact(UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
	}

	default void applyAttributes(UnitContext context) {
		context.baseApplyAttributes();
	}

	default void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
	}

	default boolean shouldCancelMove(UnitContext context, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		return false;
	}
}
