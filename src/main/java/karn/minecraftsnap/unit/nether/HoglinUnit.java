package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;

public class HoglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	private static final String ZOGLIN_STATE_KEY = "hoglin_zoglin_state";
	private static final EntitySpecEntry ZOGLIN_DISGUISE = disguise("minecraft:zoglin");

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public void onTick(UnitContext context) {
		if (context.player() == null || context.world() == null || !isZoglinState(context.state())) {
			return;
		}
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, zoglinSpeedAmplifier(), true, false, true), context.player());
		if (shouldApplyZoglinSelfDamageAtTick(context.serverTicks(), context.state().getUnitRuntimeLong(ZOGLIN_STATE_KEY))) {
			context.player().damage(context.world(), context.player().getDamageSources().generic(), zoglinSelfDamageAmount());
		}
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (victim == null || !context.isEnemyTarget(victim) || amount <= 0.0F) {
			return;
		}
		var velocity = victim.getVelocity();
		victim.setVelocity(velocity.x, Math.max(velocity.y, launchVelocityY()), velocity.z);
		victim.setOnGround(false);
		victim.velocityModified = true;
		context.player().getWorld().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_HOGLIN_ATTACK, SoundCategory.PLAYERS, 0.9f, 1.1f);
	}

	double weaponAttackDamage() {
		return 4.0D;
	}

	double weaponAttackSpeed() {
		return 1.0D;
	}

	double launchVelocityY() {
		return 0.6D;
	}

	float lastStandHealAmount() {
		return 20.0F;
	}

	long zoglinSelfDamageIntervalTicks() {
		return 20L;
	}

	float zoglinSelfDamageAmount() {
		return 1.0F;
	}

	int zoglinSpeedAmplifier() {
		return 3;
	}

	boolean shouldTriggerLastStand(boolean alreadyZoglinState) {
		return !alreadyZoglinState;
	}

	boolean shouldApplyZoglinSelfDamageAtTick(long serverTicks, Long zoglinStartedTick) {
		if (serverTicks <= 0L || zoglinStartedTick == null) {
			return false;
		}
		var elapsedTicks = serverTicks - zoglinStartedTick;
		return elapsedTicks > 0L && elapsedTicks % zoglinSelfDamageIntervalTicks() == 0L;
	}

	public static float healthAfterLastStand(float currentHealth, float maxHealth) {
		return Math.min(maxHealth, Math.max(0.0F, currentHealth) + new HoglinUnit().lastStandHealAmount());
	}

	public static boolean isZoglinState(PlayerMatchState state) {
		return state != null && state.getUnitRuntimeLong(ZOGLIN_STATE_KEY) != null;
	}

	public static void enterZoglinState(net.minecraft.server.network.ServerPlayerEntity player, PlayerMatchState state, long serverTicks) {
		if (state != null) {
			state.setUnitRuntimeLong(ZOGLIN_STATE_KEY, serverTicks);
		}
		if (player != null) {
			DisguiseSupport.applyDisguise(player, ZOGLIN_DISGUISE);
		}
	}
}
