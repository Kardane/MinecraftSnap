package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class BreezeUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final String REGRANT_WIND_CHARGE_AT_TICK_KEY = "breeze_regrant_wind_charge_at_tick";

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
	}

	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyJumpStrength(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		restockWindCharge(context);
		applyJumpStrength(context);
	}

	@Override
	public void onSkillUse(UnitContext context) {
		context.activateSkill(() -> {
			var world = context.world();
			var player = context.player();
			if (world == null || player == null) {
				return false;
			}
			var charge = EntityType.BREEZE_WIND_CHARGE.create(world, SpawnReason.MOB_SUMMONED);
			if (!(charge instanceof BreezeWindChargeEntity windCharge)) {
				return false;
			}
			var direction = player.getRotationVec(1.0f).normalize();
			windCharge.setOwner(player);
			windCharge.refreshPositionAndAngles(player.getX() + direction.x * 0.5D, player.getEyeY() - 0.1D, player.getZ() + direction.z * 0.5D, player.getYaw(), player.getPitch());
			windCharge.setVelocity(direction.multiply(projectileSpeed()));
			world.spawnEntity(windCharge);
			player.getInventory().setStack(player.getInventory().getSelectedSlot(), net.minecraft.item.ItemStack.EMPTY);
			player.getInventory().markDirty();
			player.playerScreenHandler.sendContentUpdates();
			context.setUnitRuntimeLong(REGRANT_WIND_CHARGE_AT_TICK_KEY, context.serverTicks() + throwCooldownTicks());
			world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.9f, 1.0f);
			return true;
		});
	}

	private void applyJumpStrength(UnitContext context) {
		var jumpStrength = context.player().getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
		if (jumpStrength != null) {
			jumpStrength.setBaseValue(context.unitDefinition().extraAttributes().jumpStrengthOrDefault(jumpStrengthValue()));
		}
	}

	int throwCooldownTicks() {
		return 30;
	}

	double jumpStrengthValue() {
		return 0.6D;
	}

	double projectileSpeed() {
		return 1.2D;
	}

	private void restockWindCharge(UnitContext context) {
		var player = context.player();
		if (player == null) {
			return;
		}
		var regrantAtTick = context.getUnitRuntimeLong(REGRANT_WIND_CHARGE_AT_TICK_KEY);
		if (regrantAtTick != null && context.serverTicks() < regrantAtTick) {
			return;
		}
		if (regrantAtTick != null) {
			context.removeUnitRuntimeLong(REGRANT_WIND_CHARGE_AT_TICK_KEY);
		}
		restockSingleConsumable(player, Items.WIND_CHARGE);
	}

	boolean shouldMoveInventoryChargeToMainHand(boolean mainHandEmpty, boolean inventoryHasCharge) {
		return shouldMoveInventoryRestockToMainHand(mainHandEmpty, inventoryHasCharge);
	}

	boolean shouldCreateNewCharge(boolean mainHandEmpty, boolean inventoryHasCharge) {
		return shouldCreateNewRestockItem(mainHandEmpty, inventoryHasCharge);
	}

	boolean shouldRegrantChargeAfterSkillUse() {
		return true;
	}
}
