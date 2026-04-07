package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class SnowGolemUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	private static final String REGRANT_SNOWBALL_AT_TICK_KEY = "snow_golem_regrant_snowball_at_tick";

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
	}

	@Override
	public void onTick(UnitContext context) {
		var player = context.player();
		if (player == null) {
			return;
		}
		restockSnowball(context);
		if (isColdBiome(context.currentBiomeId())) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, true, false, false));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false, false));
		}
		if (context.serverTicks() % environmentDamageIntervalTicks() != 0L) {
			return;
		}
		if (!shouldTakeEnvironmentDamage(context.currentBiomeId(), player.isTouchingWater())) {
			return;
		}
		player.damage(context.world(), player.getDamageSources().generic(), environmentDamageAmount());
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			var world = context.world();
			if (player == null || world == null) {
				return false;
			}
			var snowball = new SnowballEntity(world, player, new ItemStack(Items.SNOWBALL));
			snowball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, snowballSpeed(), snowballDivergence());
			world.spawnEntity(snowball);
			player.getInventory().setStack(player.getInventory().getSelectedSlot(), ItemStack.EMPTY);
			player.getInventory().markDirty();
			player.playerScreenHandler.sendContentUpdates();
			context.setUnitRuntimeLong(REGRANT_SNOWBALL_AT_TICK_KEY, context.serverTicks() + throwCooldownTicks());
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 0.8f, 1.0f);
			return true;
		});
	}

	@Override
	public void onProjectileHit(UnitContext context, ProjectileEntity projectile, Entity target) {
		if (!(projectile instanceof SnowballEntity) || !(target instanceof LivingEntity living) || !context.isEnemyTarget(living)) {
			return;
		}
		living.setFrozenTicks(nextFrozenTicks(context.currentBiomeId(), living.getFrozenTicks()));
		if (shouldDealColdBiomeBonusDamage(context.currentBiomeId())) {
			living.damage(context.world(), living.getDamageSources().thrown(projectile, context.player()), coldBiomeBonusDamageAmount());
		}
	}

	int throwCooldownTicks() {
		return 4;
	}

	long environmentDamageIntervalTicks() {
		return 20L;
	}

	float environmentDamageAmount() {
		return 2.0f;
	}

	boolean shouldTakeEnvironmentDamage(String biomeId, boolean touchingWater) {
		return touchingWater || isMeltingBiome(biomeId);
	}

	boolean isMeltingBiome(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return false;
		}
		return biomeId.contains("desert") || biomeId.contains("badlands") || biomeId.contains("nether");
	}

	int nextFrozenTicks(String biomeId, int currentFrozenTicks) {
		return Math.max(0, currentFrozenTicks) + additionalFreezeTicks(biomeId);
	}

	int additionalFreezeTicks(String biomeId) {
		return isColdBiome(biomeId) ? 80 : 40;
	}

	boolean shouldDealColdBiomeBonusDamage(String biomeId) {
		return isColdBiome(biomeId);
	}

	float coldBiomeBonusDamageAmount() {
		return 0.5F;
	}

	boolean isColdBiome(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return false;
		}
		return biomeId.contains("taiga")
			|| biomeId.contains("cold_ocean")
			|| biomeId.contains("frozen_ocean")
			|| biomeId.contains("jagged_peaks");
	}

	float snowballSpeed() {
		return 1.5f;
	}

	float snowballDivergence() {
		return 1.0f;
	}

	private void restockSnowball(UnitContext context) {
		var player = context.player();
		if (player == null) {
			return;
		}
		var regrantAtTick = context.getUnitRuntimeLong(REGRANT_SNOWBALL_AT_TICK_KEY);
		if (regrantAtTick != null && context.serverTicks() < regrantAtTick) {
			return;
		}
		if (regrantAtTick != null) {
			context.removeUnitRuntimeLong(REGRANT_SNOWBALL_AT_TICK_KEY);
		}
		restockSingleConsumable(player, Items.SNOWBALL);
	}

	boolean shouldMoveInventorySnowballToMainHand(boolean mainHandEmpty, boolean inventoryHasSnowball) {
		return shouldMoveInventoryRestockToMainHand(mainHandEmpty, inventoryHasSnowball);
	}

	boolean shouldCreateNewSnowball(boolean mainHandEmpty, boolean inventoryHasSnowball) {
		return shouldCreateNewRestockItem(mainHandEmpty, inventoryHasSnowball);
	}

	boolean shouldRegrantSnowballAfterSkillUse() {
		return true;
	}
}
