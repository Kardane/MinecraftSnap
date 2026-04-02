package karn.minecraftsnap.unit;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractUnitClass implements UnitClass {
	protected boolean shouldMoveInventoryRestockToMainHand(boolean mainHandEmpty, boolean inventoryHasItem) {
		return mainHandEmpty && inventoryHasItem;
	}

	protected boolean shouldCreateNewRestockItem(boolean mainHandEmpty, boolean inventoryHasItem) {
		return mainHandEmpty && !inventoryHasItem;
	}

	protected void restockSingleConsumable(ServerPlayerEntity player, Item item) {
		if (player == null || item == null) {
			return;
		}
		var inventory = player.getInventory();
		var selectedSlot = inventory.getSelectedSlot();
		var mainHand = player.getMainHandStack();
		if (mainHand.isOf(item)) {
			if (mainHand.getCount() != 1) {
				mainHand.setCount(1);
				syncInventory(player);
			}
			return;
		}
		var inventorySlot = firstInventorySlot(player, item);
		if (shouldMoveInventoryRestockToMainHand(mainHand.isEmpty(), inventorySlot >= 0)) {
			var sourceStack = inventory.getStack(inventorySlot);
			if (!sourceStack.isEmpty()) {
				sourceStack.decrement(1);
				inventory.setStack(selectedSlot, new ItemStack(item));
				syncInventorySlot(player, inventorySlot);
				syncInventorySlot(player, selectedSlot);
				syncInventory(player);
			}
			return;
		}
		if (!shouldCreateNewRestockItem(mainHand.isEmpty(), inventorySlot >= 0)) {
			return;
		}
		inventory.setStack(selectedSlot, new ItemStack(item));
		syncInventorySlot(player, selectedSlot);
		syncInventory(player);
	}

	protected void regrantSingleConsumable(ServerPlayerEntity player, Item item, int cooldownTicks) {
		if (player == null || item == null) {
			return;
		}
		var inventory = player.getInventory();
		inventory.setStack(inventory.getSelectedSlot(), new ItemStack(item));
		if (cooldownTicks > 0) {
			player.getItemCooldownManager().set(item.getDefaultStack(), cooldownTicks);
		}
		syncInventorySlot(player, inventory.getSelectedSlot());
		syncInventory(player);
	}

	private int firstInventorySlot(ServerPlayerEntity player, Item item) {
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			var stack = inventory.getStack(slot);
			if (stack != null && !stack.isEmpty() && stack.isOf(item)) {
				return slot;
			}
		}
		return -1;
	}

	protected void syncInventory(ServerPlayerEntity player) {
		player.getInventory().markDirty();
		player.playerScreenHandler.sendContentUpdates();
		if (player.currentScreenHandler != null && player.currentScreenHandler != player.playerScreenHandler) {
			player.currentScreenHandler.sendContentUpdates();
		}
	}

	private void syncInventorySlot(ServerPlayerEntity player, int slot) {
		if (player == null || player.networkHandler == null || slot < 0) {
			return;
		}
		var inventory = player.getInventory();
		if (slot >= inventory.size()) {
			return;
		}
		player.networkHandler.sendPacket(new SetPlayerInventoryS2CPacket(slot, inventory.getStack(slot).copy()));
	}
}
