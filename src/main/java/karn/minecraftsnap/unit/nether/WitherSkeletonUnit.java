package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class WitherSkeletonUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	private static final String WITHER_SKELETON_SKULL_TAG = "minecraftsnap_wither_skeleton_skull";

	@Override
	public void onTick(UnitContext context) {
		context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
		context.player().removeStatusEffect(StatusEffects.WITHER);
	}

	boolean isWitherImmune() {
		return true;
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (!context.isEnemyTarget(victim)) {
			return;
		}
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, statusDurationTicks(), effectAmplifier()), context.player());
		if (isSoulSandValleyBiome(context.currentBiomeId())) {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, soulSandValleyRegenerationDurationTicks(), 0, true, false, true), context.player());
		}
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var world = context.world();
			var player = context.player();
			if (world == null || player == null) {
				return false;
			}
			var direction = player.getRotationVec(1.0f).normalize();
			var skull = EntityType.WITHER_SKULL.create(world, SpawnReason.MOB_SUMMONED);
			if (skull == null) {
				return false;
			}
			skull.setOwner(player);
			skull.addCommandTag(WITHER_SKELETON_SKULL_TAG);
			skull.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.2D, player.getZ(), player.getYaw(), player.getPitch());
			skull.setVelocity(direction.multiply(1.35D));
			world.spawnEntity(skull);
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		});
	}

	@Override
	public void onProjectileHit(UnitContext context, ProjectileEntity projectile, Entity target) {
		if (isSkillWitherSkull(projectile) && target != null) {
			explodeAt(context, projectile, target.getPos());
		}
	}

	@Override
	public void onProjectileImpact(UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
		if (isSkillWitherSkull(projectile)) {
			explodeAt(context, projectile, impactPos);
		}
	}

	int statusDurationTicks() {
		return 20 * 3;
	}

	int effectAmplifier() {
		return 1;
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	boolean isSoulSandValleyBiome(String biomeId) {
		return biomeId != null && biomeId.contains("soul_sand_valley");
	}

	int soulSandValleyRegenerationDurationTicks() {
		return 20 * 10;
	}

	private void explodeAt(UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
		if (context == null || context.world() == null || impactPos == null) {
			return;
		}
		var radius = skillRadius(context);
		var radiusSquared = radius * radius;
		var box = Box.of(impactPos, Math.max(1.0D, radius * 2.0D), Math.max(1.0D, radius * 2.0D), Math.max(1.0D, radius * 2.0D));
		var damageSource = projectile != null && projectile.getOwner() != null ? projectile.getOwner() : context.player();
		for (var target : context.world().getEntitiesByClass(LivingEntity.class, box, living ->
			living.squaredDistanceTo(impactPos) <= radiusSquared && context.isEnemyTarget(living))) {
			context.dealExplosionDamage(target, skillDamage(context), projectile, damageSource);
		}
		context.world().playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.7f, 1.4f);
	}

	private double skillRadius(UnitContext context) {
		var definition = context == null ? null : context.unitDefinition();
		return definition == null ? 0.75D : definition.extraAttributes().abilityRadiusOrDefault(0.75D);
	}

	private float skillDamage(UnitContext context) {
		var definition = context == null ? null : context.unitDefinition();
		return (float) (definition == null ? 5.0D : definition.extraAttributes().abilityDamageOrDefault(5.0D));
	}

	private boolean isSkillWitherSkull(ProjectileEntity projectile) {
		return projectile instanceof WitherSkullEntity && isTaggedSkillWitherSkull(projectile.getCommandTags());
	}

	public static boolean isTaggedSkillWitherSkull(Iterable<String> commandTags) {
		if (commandTags == null) {
			return false;
		}
		for (var tag : commandTags) {
			if (WITHER_SKELETON_SKULL_TAG.equals(tag)) {
				return true;
			}
		}
		return false;
	}
}
