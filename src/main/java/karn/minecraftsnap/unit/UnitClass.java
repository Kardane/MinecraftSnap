package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.LaneId;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public interface UnitClass {
	default void onTick(UnitContext context) {
	}

	default void onAttack(UnitContext context, ServerPlayerEntity victim, float amount) {
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

	default void applyAttributes(UnitContext context) {
		context.baseApplyAttributes();
	}

	default void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
	}
}
