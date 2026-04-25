package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class GuardianUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final String SKIP_REFLECT_UNTIL_TICK_KEY = "guardian_skip_reflect_until_tick";
	private static final String LINK_TARGET_MSB_KEY = "guardian_link_target_msb";
	private static final String LINK_TARGET_LSB_KEY = "guardian_link_target_lsb";
	private static final String LINK_END_TICK_KEY = "guardian_link_end_tick";

	@Override
	public void onDamaged(UnitContext context, DamageSource source, float amount) {
		var skipReflectUntil = context.getUnitRuntimeLong(SKIP_REFLECT_UNTIL_TICK_KEY);
		if (skipReflectUntil != null && context.serverTicks() <= skipReflectUntil) {
			context.removeUnitRuntimeLong(SKIP_REFLECT_UNTIL_TICK_KEY);
			return;
		}
		var attacker = source == null ? null : source.getAttacker();
		if (attacker instanceof LivingEntity living && context.isEnemyTarget(living)) {
			markSkipReflect(context, living);
			context.dealMobDamage(living, reflectDamageAmount());
		}
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(skillCooldownTicks(context), () -> {
			var target = closestEnemyUnit(context);
			if (target == null || context.player().squaredDistanceTo(target) > linkRange() * linkRange()) {
				return false;
			}
			storeTarget(context, target.getUuid(), context.serverTicks() + linkDurationTicks());
			context.world().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.9f, 1.0f);
			return true;
		});
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var target = linkedTarget(context);
		if (target == null) {
			clearLink(context);
			return;
		}
		if (context.player().squaredDistanceTo(target) > linkRange() * linkRange()) {
			clearLink(context);
			return;
		}
		if (context.serverTicks() % 2L == 0L) {
			spawnLinkParticles(context, target);
		}
		var endTick = context.getUnitRuntimeLong(LINK_END_TICK_KEY);
		if (endTick != null && context.serverTicks() >= endTick) {
			context.dealMobDamage(target, linkDamageAmount());
			context.world().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.0f, 1.0f);
			clearLink(context);
		}
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, false, false, false), context.player());
		if (shouldApplyWaterBuff(context.player().isTouchingWater())) {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, false, false, false), context.player());
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, false, false, false), context.player());
		}
	}

	boolean shouldApplyWaterBuff(boolean touchingWater) {
		return touchingWater;
	}

	long skillCooldownTicks(UnitContext context) {
		var definition = context == null ? null : context.unitDefinition();
		long baseCooldownTicks = definition == null ? 0L : definition.abilityCooldownSeconds() * 20L;
		if (context == null || context.player() == null || context.player().isTouchingWater()) {
			return baseCooldownTicks;
		}
		return baseCooldownTicks + outOfWaterCooldownPenaltyTicks();
	}

	long outOfWaterCooldownPenaltyTicks() {
		return 20L * 7L;
	}

	@Override
	public void onDeath(UnitContext context, DamageSource source) {
		clearLink(context);
	}

	private ServerPlayerEntity closestEnemyUnit(UnitContext context) {
		return context.world().getPlayers().stream()
			.filter(context::isEnemyUnit)
			.min(Comparator.comparingDouble(player -> context.player().squaredDistanceTo(player)))
			.orElse(null);
	}

	private void storeTarget(UnitContext context, UUID uuid, long endTick) {
		context.setUnitRuntimeLong(LINK_TARGET_MSB_KEY, uuid.getMostSignificantBits());
		context.setUnitRuntimeLong(LINK_TARGET_LSB_KEY, uuid.getLeastSignificantBits());
		context.setUnitRuntimeLong(LINK_END_TICK_KEY, endTick);
	}

	protected ServerPlayerEntity linkedTarget(UnitContext context) {
		var msb = context.getUnitRuntimeLong(LINK_TARGET_MSB_KEY);
		var lsb = context.getUnitRuntimeLong(LINK_TARGET_LSB_KEY);
		if (msb == null || lsb == null || context.player().getServer() == null) {
			return null;
		}
		var target = context.player().getServer().getPlayerManager().getPlayer(new UUID(msb, lsb));
		if (target == null || target.isSpectator() || !context.isEnemyUnit(target)) {
			return null;
		}
		return target;
	}

	private void clearLink(UnitContext context) {
		context.removeUnitRuntimeLong(LINK_TARGET_MSB_KEY);
		context.removeUnitRuntimeLong(LINK_TARGET_LSB_KEY);
		context.removeUnitRuntimeLong(LINK_END_TICK_KEY);
	}

	private void spawnLinkParticles(UnitContext context, ServerPlayerEntity target) {
		var from = context.player().getPos().add(0.0D, 1.1D, 0.0D);
		var to = target.getPos().add(0.0D, 1.1D, 0.0D);
		var delta = to.subtract(from);
		var steps = Math.max(1, (int) Math.ceil(delta.length() / 0.7D));
		var effect = new DustParticleEffect(0x55FFFF, 1.4F);
		for (int index = 0; index <= steps; index++) {
			var pos = from.add(delta.multiply(index / (double) steps));
			for (var player : context.world().getPlayers()) {
				context.world().spawnParticles(player, effect, true, false, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	float reflectDamageAmount() {
		return 2.0f;
	}

	long linkDurationTicks() {
		return 40L;
	}

	double linkRange() {
		return 16.0D;
	}

	float linkDamageAmount() {
		return 8.0f;
	}

	private void markSkipReflect(UnitContext context, LivingEntity attacker) {
		if (!(attacker instanceof ServerPlayerEntity player) || context.matchManager() == null) {
			return;
		}
		context.matchManager().getPlayerState(player.getUuid()).setUnitRuntimeLong(SKIP_REFLECT_UNTIL_TICK_KEY, context.serverTicks() + reflectGuardTicks());
	}

	long reflectGuardTicks() {
		return 1L;
	}
}
