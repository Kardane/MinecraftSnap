package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class CreeperUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final String BOMB_TICK_KEY = "creeper_bomb_tick";
	private static final String BLAST_CENTER_X_KEY = "creeper_blast_center_x";
	private static final String BLAST_CENTER_Y_KEY = "creeper_blast_center_y";
	private static final String BLAST_CENTER_Z_KEY = "creeper_blast_center_z";
	private static final String ORIGINAL_JUMP_STRENGTH_KEY = "creeper_original_jump_strength";
	private static final long SELF_DESTRUCT_DELAY_TICKS = 30L;
	private static final long BLAST_CENTER_CAPTURE_TICKS = 30L;
	private static final double DEFAULT_BLAST_RADIUS = 5D;
	private static final float DEFAULT_BLAST_DAMAGE = 50.0f;
	private static final EntitySpecEntry BASE_DISGUISE = disguise("minecraft:creeper");
	private static final EntitySpecEntry IGNITED_DISGUISE = disguise("minecraft:creeper", "{ignited:1b}");
	public static final UnitDefinition DEFINITION = unit(
		"creeper",
		"크리퍼",
		FactionId.MONSTER,
		true,
		5,
		25,
		20.0,
		1.0,
		item("minecraft:tnt"),
		none(),
		none(),
		none(),
		none(),
		none(),
		item("minecraft:tnt"),
		"자폭",
		20,
		UnitDefinition.AmmoType.NONE,
		BASE_DISGUISE,
			List.of("&f패시브 &7- 기본 공격 불가","&f자폭 &7- 자폭하여 주변 적에게 피해를 입힙니다."),
		List.of(advanceOption(
			"charged_creeper",
			"대전된 크리퍼",
			List.of("&7폭풍우 속에서 30초 버티면 충전됨"),
			List.of(),
			List.of("thunder"),
			900
		))
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		clearBombState(context);
		applyDisguise(context, false);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		var triggerTick = context.getUnitRuntimeLong(BOMB_TICK_KEY);
		if (triggerTick == null) {
			return;
		}
		lockJump(context);
		if (context.serverTicks() == triggerTick - (SELF_DESTRUCT_DELAY_TICKS - BLAST_CENTER_CAPTURE_TICKS)) {
			captureBlastCenter(context);
		}
		var velocity = context.player().getVelocity();
		if (velocity.y > 0.0D) {
			context.player().setVelocity(velocity.x, 0.0D, velocity.z);
			context.player().velocityModified = true;
		}
		if (context.serverTicks() < triggerTick) {
			return;
		}
		clearBombState(context);
		explode(context);
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			if (context.getUnitRuntimeLong(BOMB_TICK_KEY) != null) {
				return false;
			}
			context.setUnitRuntimeLong(BOMB_TICK_KEY, context.serverTicks() + SELF_DESTRUCT_DELAY_TICKS);
			storeOriginalJumpStrength(context);
			lockJump(context);
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 30, 0, false, false, true), context.player());
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, (int) SELF_DESTRUCT_DELAY_TICKS + 5, 255, false, false, false), context.player());
			context.player().getWorld().playSound(
				null,
				context.player().getBlockPos(),
				SoundEvents.ENTITY_CREEPER_PRIMED,
				SoundCategory.PLAYERS,
				1.0f,
				powered() ? 0.8f : 1.0f
			);
			applyDisguise(context, true);
			context.player().sendMessage(context.format(textConfig().creeperSelfDestructPrimedMessage), true);
			return true;
		});
	}

	@Override
	public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
		clearBombState(context);
	}

	private void explode(UnitContext context) {
		var player = context.player();
		applyDisguise(context, false);
		var center = blastCenter(context);
		spawnExplosionRing(context, center.x(), center.y(), center.z(), blastRadius());
		player.getWorld().playSound(
			null,
			player.getBlockPos(),
			SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
			SoundCategory.PLAYERS,
			1.0f,
			powered() ? 0.8f : 1.0f
		);
		for (var target : nearbyEnemyTargets(context, blastRadius())) {
			if (target.squaredDistanceTo(center.x(), center.y(), center.z()) <= blastRadius() * blastRadius()) {
				context.dealExplosionDamage(target, blastDamage());
			}
		}
		player.damage(context.world(), player.getDamageSources().explosion(player, player), 1000.0f);
	}

	private void spawnExplosionRing(UnitContext context, double centerX, double centerY, double centerZ, double radius) {
		var world = context.world();
		if (world == null) {
			return;
		}
		for (int degree = 0; degree < 360; degree += 10) {
			var radians = Math.toRadians(degree);
			var x = centerX + Math.cos(radians) * radius;
			var z = centerZ + Math.sin(radians) * radius;
			world.spawnParticles(ParticleTypes.SMOKE, x, centerY, z, 2, 0.05D, 0.02D, 0.05D, 0.0D);
			world.spawnParticles(ParticleTypes.FLAME, x, centerY, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
		}
	}

	private java.util.List<LivingEntity> nearbyEnemyTargets(UnitContext context, double radius) {
		var player = context.player();
		var squaredRadius = radius * radius;
		return player.getWorld().getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius), target ->
			player.squaredDistanceTo(target) <= squaredRadius && context.isEnemyTarget(target));
	}

	private void clearBombState(UnitContext context) {
		context.removeUnitRuntimeLong(BOMB_TICK_KEY);
		context.removeUnitRuntimeDouble(BLAST_CENTER_X_KEY);
		context.removeUnitRuntimeDouble(BLAST_CENTER_Y_KEY);
		context.removeUnitRuntimeDouble(BLAST_CENTER_Z_KEY);
		if (context.player() != null) {
			context.player().removeStatusEffect(StatusEffects.SLOWNESS);
			restoreJump(context);
		}
	}

	private void storeOriginalJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			context.setUnitRuntimeDouble(ORIGINAL_JUMP_STRENGTH_KEY, jumpStrength.getBaseValue());
		}
	}

	private void lockJump(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(0.0D);
		}
	}

	private void restoreJump(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		var original = context.getUnitRuntimeDouble(ORIGINAL_JUMP_STRENGTH_KEY);
		if (jumpStrength != null && original != null) {
			jumpStrength.setBaseValue(original);
		}
		context.removeUnitRuntimeDouble(ORIGINAL_JUMP_STRENGTH_KEY);
	}

	private void captureBlastCenter(UnitContext context) {
		var pos = context.player().getPos().add(0.0D, 0.1D, 0.0D);
		context.setUnitRuntimeDouble(BLAST_CENTER_X_KEY, pos.x);
		context.setUnitRuntimeDouble(BLAST_CENTER_Y_KEY, pos.y);
		context.setUnitRuntimeDouble(BLAST_CENTER_Z_KEY, pos.z);
	}

	private BlastCenter blastCenter(UnitContext context) {
		var x = context.getUnitRuntimeDouble(BLAST_CENTER_X_KEY);
		var y = context.getUnitRuntimeDouble(BLAST_CENTER_Y_KEY);
		var z = context.getUnitRuntimeDouble(BLAST_CENTER_Z_KEY);
		if (x != null && y != null && z != null) {
			return new BlastCenter(x, y, z);
		}
		var pos = context.player().getPos().add(0.0D, 0.1D, 0.0D);
		return new BlastCenter(pos.x, pos.y, pos.z);
	}

	void applyDisguise(UnitContext context, boolean ignited) {
		if (context == null || context.player() == null) {
			return;
		}
		DisguiseSupport.applyDisguise(context.player(), ignited ? activeDisguise() : restingDisguise());
	}

	EntitySpecEntry restingDisguise() {
		return BASE_DISGUISE;
	}

	EntitySpecEntry activeDisguise() {
		return IGNITED_DISGUISE;
	}

	double blastRadius() {
		return DEFAULT_BLAST_RADIUS;
	}

	float blastDamage() {
		return DEFAULT_BLAST_DAMAGE;
	}

	long selfDestructDelayTicks() {
		return SELF_DESTRUCT_DELAY_TICKS;
	}

	boolean powered() {
		return false;
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	private record BlastCenter(double x, double y, double z) {
	}
}
