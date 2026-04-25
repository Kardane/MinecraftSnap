package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.integration.DisguiseAnimationSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class GhastUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	private static final String PENDING_FIREBALL_TICK_KEY = "ghast_pending_fireball_tick";
	private static final String VERTICAL_LIMIT_ZONE_KEY = "ghast_vertical_limit_zone";
	private static final String HANDLED_FIREBALL_TAG = "minecraftsnap_ghast_handled";
	private static final String GHAST_FIREBALL_TAG = "minecraftsnap_ghast_fireball";
	private static final String GHAST_FIREBALL_OWNER_PREFIX = "minecraftsnap_ghast_owner:";

	@Override
	public void onTick(UnitContext context) {
		var player = context.player();
		if (player == null || context.world() == null) {
			return;
		}
		player.setNoGravity(shouldKeepNoGravity());
		player.fallDistance = 0.0f;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, fireResistanceDurationTicks(), 0, true, false, false));
		var groundDistance = groundDistance(context);
		var currentZone = verticalLimitZone(groundDistance);
		var previousZone = previousVerticalLimitZone(context);
		if (shouldApplyVerticalLimitImpulse(previousZone, currentZone)) {
			var velocity = player.getVelocity();
			player.setVelocity(velocity.x, verticalVelocityForZone(currentZone, velocity.y), velocity.z);
			player.velocityModified = true;
		}
		storeVerticalLimitZone(context, currentZone);
		if (shouldLaunchFireball(context.getUnitRuntimeLong(PENDING_FIREBALL_TICK_KEY), context.serverTicks())) {
			context.removeUnitRuntimeLong(PENDING_FIREBALL_TICK_KEY);
			launchFireball(context);
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
			if (context.getUnitRuntimeLong(PENDING_FIREBALL_TICK_KEY) != null) {
				return false;
			}
			context.setUnitRuntimeLong(PENDING_FIREBALL_TICK_KEY, nextFireballLaunchTick(context.serverTicks()));
			DisguiseAnimationSupport.startGhastCharge(player, (int) fireballDelayTicks());
			playWarningSound(player);
			return true;
		});
	}

	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
		context.removeUnitRuntimeLong(PENDING_FIREBALL_TICK_KEY);
		context.removeUnitRuntimeLong(VERTICAL_LIMIT_ZONE_KEY);
		if (context.player() != null) {
			context.player().setNoGravity(false);
		}
	}

	@Override
	public void onProjectileHit(UnitContext context, ProjectileEntity projectile, Entity target) {
		if (target != null && shouldExplodeFireball(isGhastFireball(context, projectile), markFireballHandled(projectile))) {
			explodeAt(context, projectile, target.getPos());
		}
	}

	@Override
	public void onProjectileImpact(UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
		if (shouldExplodeFireball(isGhastFireball(context, projectile), markFireballHandled(projectile))) {
			explodeAt(context, projectile, impactPos);
		}
	}

	private boolean isGhastFireball(UnitContext context, ProjectileEntity projectile) {
		return projectile instanceof FireballEntity && "ghast".equals(context.unitDefinition().id());
	}

	private void explodeAt(UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
		if (impactPos == null || context.world() == null) {
			return;
		}
		var squaredRadius = explosionRadius() * explosionRadius();
		var searchBox = Box.of(impactPos, explosionRadius() * 2.0D, explosionRadius() * 2.0D, explosionRadius() * 2.0D);
		var damageAttacker = projectile != null && projectile.getOwner() != null ? projectile.getOwner() : context.player();
		for (var target : context.world().getEntitiesByClass(LivingEntity.class, searchBox, living -> living.squaredDistanceTo(impactPos) <= squaredRadius)) {
			if (!shouldDamageTargetWithGhastFireball(context.isEnemyTarget(target), isGhastUnitTarget(context, target))) {
				continue;
			}
			context.dealExplosionDamage(target, explosionDamageAmount(), projectile, damageAttacker);
		}
		spawnExplosionRing(context, impactPos);
		context.world().playSound(null, BlockPos.ofFloored(impactPos), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	double adjustedVerticalVelocity(double groundDistance, double currentVerticalVelocity) {
		return verticalVelocityForZone(verticalLimitZone(groundDistance), currentVerticalVelocity);
	}

	int fireResistanceDurationTicks() {
		return 40;
	}

	int verticalLimitZone(double groundDistance) {
		if (groundDistance <= riseThreshold()) {
			return 1;
		}
		if (groundDistance >= fallThreshold()) {
			return -1;
		}
		return 0;
	}

	boolean shouldApplyVerticalLimitImpulse(int previousZone, int currentZone) {
		return currentZone != 0 && previousZone != currentZone;
	}

	double verticalVelocityForZone(int zone, double currentVerticalVelocity) {
		return switch (zone) {
			case 1 -> riseVelocity();
			case -1 -> fallVelocity();
			default -> currentVerticalVelocity;
		};
	}

	double explosionRadius() {
		return 6.0D;
	}

	float explosionDamageAmount() {
		return 20.0f;
	}

	private double groundDistance(UnitContext context) {
		var player = context.player();
		var world = context.world();
		if (player == null || world == null) {
			return fallThreshold();
		}
		var origin = BlockPos.ofFloored(player.getPos());
		for (int offset = 0; offset <= maxGroundCheckDistance(); offset++) {
			var pos = origin.down(offset);
			if (pos.getY() < world.getBottomY()) {
				return maxGroundCheckDistance();
			}
			if (!world.getBlockState(pos).isAir()) {
				return offset;
			}
		}
		return maxGroundCheckDistance();
	}

	private double projectileSpeed() {
		return 1.0D;
	}

	long fireballDelayTicks() {
		return 20L;
	}

	long nextFireballLaunchTick(long serverTicks) {
		return serverTicks + fireballDelayTicks();
	}

	boolean shouldLaunchFireball(Long scheduledTick, long serverTicks) {
		return scheduledTick != null && serverTicks >= scheduledTick;
	}

	boolean shouldKeepNoGravity() {
		return true;
	}

	boolean shouldExplodeFireball(boolean ghastFireball, boolean alreadyHandled) {
		return ghastFireball && !alreadyHandled;
	}

	boolean shouldDamageTargetWithGhastFireball(boolean enemyUnit, boolean ghastUnit) {
		return enemyUnit || ghastUnit;
	}

	private double riseThreshold() {
		return 5.0D;
	}

	private double fallThreshold() {
		return 16.0D;
	}

	private double riseVelocity() {
		return 0.12D;
	}

	private double fallVelocity() {
		return -0.12D;
	}

	private int maxGroundCheckDistance() {
		return 32;
	}

	private int previousVerticalLimitZone(UnitContext context) {
		var value = context.getUnitRuntimeLong(VERTICAL_LIMIT_ZONE_KEY);
		return value == null ? 0 : (int) Math.max(-1L, Math.min(1L, value));
	}

	private void storeVerticalLimitZone(UnitContext context, int zone) {
		if (zone == 0) {
			context.removeUnitRuntimeLong(VERTICAL_LIMIT_ZONE_KEY);
			return;
		}
		context.setUnitRuntimeLong(VERTICAL_LIMIT_ZONE_KEY, zone);
	}

	private void launchFireball(UnitContext context) {
		var world = context.world();
		var player = context.player();
		if (world == null || player == null) {
			return;
		}
		var direction = player.getRotationVec(1.0f).normalize();
		var fireball = EntityType.FIREBALL.create(world, SpawnReason.MOB_SUMMONED);
		if (fireball == null) {
			return;
		}
		fireball.refreshPositionAndAngles(player.getX() + direction.x * 0.8D, player.getEyeY(), player.getZ() + direction.z * 0.8D, player.getYaw(), player.getPitch());
		fireball.setOwner(player);
		tagLaunchedFireball(fireball, player);
		fireball.setVelocity(direction.multiply(projectileSpeed()));
		world.spawnEntity(fireball);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	private void playWarningSound(net.minecraft.server.network.ServerPlayerEntity player) {
		if (player == null || player.getServer() == null) {
			return;
		}
		for (var serverPlayer : player.getServer().getPlayerManager().getPlayerList()) {
			serverPlayer.playSoundToPlayer(SoundEvents.ENTITY_GHAST_WARN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
	}

	private boolean markFireballHandled(ProjectileEntity projectile) {
		if (projectile == null) {
			return true;
		}
		if (projectile.getCommandTags().contains(HANDLED_FIREBALL_TAG)) {
			return true;
		}
		projectile.addCommandTag(HANDLED_FIREBALL_TAG);
		return false;
	}

	private void spawnExplosionRing(UnitContext context, Vec3d impactPos) {
		int points = explosionRingPoints();
		for (int index = 0; index < points; index++) {
			double angle = (Math.PI * 2.0D * index) / points;
			double x = impactPos.x + Math.cos(angle) * explosionRadius();
			double z = impactPos.z + Math.sin(angle) * explosionRadius();
			for (var player : context.world().getPlayers()) {
				context.world().spawnParticles(player, ParticleTypes.FLAME, true, false, x, impactPos.y + 0.1D, z, 1, 0.0D, 0.05D, 0.0D, 0.0D);
			}
		}
	}

	int explosionRingPoints() {
		return 24;
	}

	private boolean isGhastUnitTarget(UnitContext context, LivingEntity target) {
		if (!(target instanceof net.minecraft.server.network.ServerPlayerEntity player) || context == null || context.matchManager() == null) {
			return false;
		}
		return "ghast".equals(context.matchManager().getPlayerState(player.getUuid()).getCurrentUnitId());
	}

	public static String ghastFireballTag() {
		return GHAST_FIREBALL_TAG;
	}

	public static boolean isTaggedGhastFireball(Iterable<String> commandTags) {
		if (commandTags == null) {
			return false;
		}
		for (var tag : commandTags) {
			if (GHAST_FIREBALL_TAG.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	public static boolean shouldAllowFireballDeflection(Iterable<String> commandTags, Entity deflector) {
		return !isTaggedGhastFireball(commandTags);
	}

	public static String originalOwnerTag(UUID ownerId) {
		return GHAST_FIREBALL_OWNER_PREFIX + ownerId;
	}

	public static UUID findOriginalOwnerId(Iterable<String> commandTags) {
		if (!isTaggedGhastFireball(commandTags)) {
			return null;
		}
		for (var tag : commandTags) {
			if (tag != null && tag.startsWith(GHAST_FIREBALL_OWNER_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(GHAST_FIREBALL_OWNER_PREFIX.length()));
				} catch (IllegalArgumentException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	private void tagLaunchedFireball(FireballEntity fireball, net.minecraft.server.network.ServerPlayerEntity owner) {
		if (fireball == null || owner == null) {
			return;
		}
		fireball.addCommandTag(GHAST_FIREBALL_TAG);
		fireball.addCommandTag(originalOwnerTag(owner.getUuid()));
	}
}
