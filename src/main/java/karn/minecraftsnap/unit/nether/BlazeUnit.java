package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class BlazeUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	private static final String SHOTS_REMAINING_KEY = "blaze_shots_remaining";
	private static final String NEXT_SHOT_TICK_KEY = "blaze_next_shot_tick";
	public static final String ZERO_DAMAGE_FIREBALL_TAG = "minecraftsnap_blaze_zero_damage";

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
		context.removeUnitRuntimeLong(SHOTS_REMAINING_KEY);
		context.removeUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
	}

	@Override
	public void onTick(UnitContext context) {
		var player = context.player();
		var world = context.world();
		if (player == null || world == null) {
			return;
		}
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
		if (shouldTakeWaterDamage(player.isTouchingWater(), context.serverTicks())) {
			player.damage(world, player.getDamageSources().generic(), waterDamageAmount());
		}
		if (shouldTakeRainDamage(world.hasRain(player.getBlockPos()), context.serverTicks())) {
			player.damage(world, player.getDamageSources().generic(), rainDamageAmount());
		}
		var remaining = context.getUnitRuntimeLong(SHOTS_REMAINING_KEY);
		var nextShotTick = context.getUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
		if (remaining == null || remaining <= 0 || nextShotTick == null || context.serverTicks() < nextShotTick) {
			return;
		}
		fireSingleShot(context);
		if (remaining <= 1) {
			context.removeUnitRuntimeLong(SHOTS_REMAINING_KEY);
			context.removeUnitRuntimeLong(NEXT_SHOT_TICK_KEY);
			return;
		}
		context.setUnitRuntimeLong(SHOTS_REMAINING_KEY, remaining - 1);
		context.setUnitRuntimeLong(NEXT_SHOT_TICK_KEY, context.serverTicks() + shotIntervalTicks());
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(skillCooldownTicks(context), () -> {
			context.setUnitRuntimeLong(SHOTS_REMAINING_KEY, (long) shotCount());
			context.setUnitRuntimeLong(NEXT_SHOT_TICK_KEY, context.serverTicks());
			return true;
		});
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim) || !isNetherWastesBiome(context.currentBiomeId())) {
			return;
		}
		context.dealMobDamage(victim, netherWastesBonusDamageAmount());
	}

	@Override
	public void onProjectileHit(UnitContext context, ProjectileEntity projectile, Entity target) {
		if (!(projectile instanceof SmallFireballEntity) || !(target instanceof LivingEntity living) || !context.isEnemyTarget(living)) {
			return;
		}
		if (!isNetherWastesBiome(context.currentBiomeId())) {
			return;
		}
		context.dealMobDamage(living, netherWastesBonusDamageAmount());
	}

	private void fireSingleShot(UnitContext context) {
		var world = context.world();
		var player = context.player();
		var direction = player.getRotationVec(1.0f).normalize();
		var fireball = new SmallFireballEntity(world, player, direction);
		fireball.refreshPositionAndAngles(player.getX() + direction.x * 0.5D, player.getEyeY() - 0.1D, player.getZ() + direction.z * 0.5D, player.getYaw(), player.getPitch());
		fireball.setVelocity(direction.multiply(1.1D));
		fireball.addCommandTag(ZERO_DAMAGE_FIREBALL_TAG);
		world.spawnEntity(fireball);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9f, 1.0f);
	}

	double weaponAttackDamage() {
		return 3.0D;
	}

	double weaponAttackSpeed() {
		return 2.0D;
	}

	int shotCount() {
		return 3;
	}

	long skillCooldownTicks(UnitContext context) {
		var definition = context == null ? null : context.unitDefinition();
		long baseCooldownTicks = definition == null ? 0L : definition.abilityCooldownSeconds() * 20L;
		if (isNetherWastesBiome(context == null ? null : context.currentBiomeId())) {
			return Math.max(0L, baseCooldownTicks - netherWastesCooldownReductionTicks());
		}
		return baseCooldownTicks;
	}

	long shotIntervalTicks() {
		return 8L;
	}

	long netherWastesCooldownReductionTicks() {
		return 20L;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	boolean shouldTakeWaterDamage(boolean touchingWater, long serverTicks) {
		return touchingWater && serverTicks > 0L && serverTicks % 20L == 0L;
	}

	float waterDamageAmount() {
		return 2.0F;
	}

	boolean shouldTakeRainDamage(boolean beingRainedOn, long serverTicks) {
		return beingRainedOn && serverTicks > 0L && serverTicks % 20L == 0L;
	}

	float rainDamageAmount() {
		return 1.0F;
	}

	boolean isNetherWastesBiome(String biomeId) {
		return biomeId != null && biomeId.contains("nether_wastes");
	}

	float netherWastesBonusDamageAmount() {
		return 1.0F;
	}

	float fireballDirectDamageAmount() {
		return 0.0F;
	}
}
