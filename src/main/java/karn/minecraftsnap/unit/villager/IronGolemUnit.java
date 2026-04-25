package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.applyCombatProfile;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class IronGolemUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String DISGUISE_HEALTH_TENTHS_KEY = "iron_golem_disguise_health_tenths";

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		applyCombatProfile(context.player().getMainHandStack(), context.unitDefinition().id(), weaponAttackDamage(), weaponAttackSpeed());
	}

	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyMobilityAttributes(context);
	}

	@Override
	public void onTick(UnitContext context) {
		applyMobilityAttributes(context);
		//syncDisguiseHealth(context);
	}

	@Override
	public void onSkillUse(UnitContext context) {
		var definition = context.unitDefinition();
		context.activateSkill(repairCooldownTicks(definition == null ? 0 : definition.abilityCooldownSeconds()), () -> repairSelf(context));
	}

	@Override
	public void onAttack(UnitContext context, LivingEntity victim, float amount) {
		if (victim == null || !shouldLaunchTarget(context.isEnemyTarget(victim), amount)) {
			return;
		}
		applyVerticalLaunch(victim);
		context.player().getWorld().playSound(null, context.player().getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 0.9f, 1.1f);
	}

	private void applyMobilityAttributes(UnitContext context) {
		var player = context.player();
		if (player == null) {
			return;
		}
		var jumpStrength = player.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(context.unitDefinition().extraAttributes().jumpStrengthOrDefault(jumpStrengthValue()));
		}
		var stepHeight = player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
		if (stepHeight != null) {
			stepHeight.setBaseValue(context.unitDefinition().extraAttributes().stepHeightOrDefault(stepHeightValue()));
		}
	}

	double jumpStrengthValue() {
		return 0.0D;
	}

	double stepHeightValue() {
		return 1.3D;
	}

	double launchVelocityY() {
		return 1D;
	}

	boolean shouldLaunchTarget(boolean enemyTarget, float amount) {
		return enemyTarget && amount > 0.0f;
	}

	private void applyVerticalLaunch(LivingEntity victim) {
		var server = victim.getServer();
		if (server == null) {
			applyVerticalLaunchNow(victim);
			return;
		}
		server.execute(() -> applyVerticalLaunchNow(victim));
	}

	private void applyVerticalLaunchNow(LivingEntity victim) {
		if (victim == null || !victim.isAlive()) {
			return;
		}
		var velocity = victim.getVelocity();
		victim.setVelocity(velocity.x, Math.max(velocity.y, launchVelocityY()), velocity.z);
		victim.setOnGround(false);
		victim.velocityModified = true;
	}

	double weaponAttackDamage() {
		return 6.0D;
	}

	double weaponAttackSpeed() {
		return 2;
	}

	long repairCooldownTicks(int cooldownSeconds) {
		return 20L * Math.max(0, cooldownSeconds);
	}

	double repairHealAmount(double maxHealth) {
		return maxHealth * 0.33D;
	}

	double disguiseHealthForPlayerHealth(double playerHealth, double playerMaxHealth, double disguiseMaxHealth) {
		if (playerMaxHealth <= 0.0D || disguiseMaxHealth <= 0.0D) {
			return 1.0D;
		}
		var ratio = Math.max(0.0D, Math.min(1.0D, playerHealth / playerMaxHealth));
		return Math.max(1.0D, disguiseMaxHealth * ratio);
	}

	boolean shouldRefreshDisguiseHealth(Long lastHealthTenths, long currentHealthTenths) {
		return lastHealthTenths == null || lastHealthTenths.longValue() != currentHealthTenths;
	}

	private boolean repairSelf(UnitContext context) {
		var player = context.player();
		var world = context.world();
		if (player == null || world == null) {
			return false;
		}
		var healed = context.healSelf((float) repairHealAmount(player.getMaxHealth()));
		if (healed <= 0.0D) {
			return false;
		}
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.9f, 1f);
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getBodyY(0.5D), player.getZ(), 20, 0.5D, 0.6D, 0.5D, 0.0D);
		//syncDisguiseHealth(context);
		return true;
	}

	private void syncDisguiseHealth(UnitContext context) {
		var player = context.player();
		if (player == null) {
			return;
		}
		var currentHealthTenths = Math.round(player.getHealth() * 10.0F);
		if (!shouldRefreshDisguiseHealth(context.getUnitRuntimeLong(DISGUISE_HEALTH_TENTHS_KEY), currentHealthTenths)) {
			return;
		}
		context.setUnitRuntimeLong(DISGUISE_HEALTH_TENTHS_KEY, currentHealthTenths);
		DisguiseSupport.applyDisguise(context.player(), context.unitDefinition().disguise(), entity -> applyDisguiseHealth(entity, player));
	}

	private void applyDisguiseHealth(Entity entity, net.minecraft.server.network.ServerPlayerEntity player) {
		if (!(entity instanceof IronGolemEntity disguise) || player == null) {
			return;
		}
		disguise.setHealth((float) disguiseHealthForPlayerHealth(player.getHealth(), player.getMaxHealth(), disguise.getMaxHealth()));
	}
}
