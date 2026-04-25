package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	@Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$handleShortcut(PlayerActionC2SPacket packet, CallbackInfo ci) {
		if (minecraftsnap$shouldBlockDropAction(packet.getAction())) {
			var mod = MinecraftSnap.getInstance();
			if (mod != null && mod.shouldBlockItemDrop(player)) {
				minecraftsnap$syncInventory();
				ci.cancel();
				return;
			}
		}
		if (packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND || !player.isSneaking()) {
			return;
		}

		var mod = MinecraftSnap.getInstance();
		if (mod != null && mod.handleShortcut(player)) {
			ci.cancel();
		}
	}

	@Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$blockThrowSlotClick(ClickSlotC2SPacket packet, CallbackInfo ci) {
		if (packet.actionType() != SlotActionType.THROW) {
			return;
		}
		var mod = MinecraftSnap.getInstance();
		if (mod != null && mod.shouldBlockItemDrop(player)) {
			minecraftsnap$syncInventory();
			ci.cancel();
		}
	}

	@Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$lockPrimedCreeperMovement(PlayerMoveC2SPacket packet, CallbackInfo ci) {
		var mod = MinecraftSnap.getInstance();
		if (mod != null && mod.shouldCancelPlayerMove(player, packet)) {
			ci.cancel();
		}
	}

	@Unique
	private boolean minecraftsnap$shouldBlockDropAction(PlayerActionC2SPacket.Action action) {
		return action == PlayerActionC2SPacket.Action.DROP_ITEM
			|| action == PlayerActionC2SPacket.Action.DROP_ALL_ITEMS;
	}

	@Unique
	private void minecraftsnap$syncInventory() {
		if (player == null || player.networkHandler == null) {
			return;
		}
		var inventory = player.getInventory();
		var selectedSlot = inventory.getSelectedSlot();
		player.networkHandler.sendPacket(new SetPlayerInventoryS2CPacket(selectedSlot, inventory.getStack(selectedSlot).copy()));
		player.playerScreenHandler.sendContentUpdates();
		if (player.currentScreenHandler != null && player.currentScreenHandler != player.playerScreenHandler) {
			player.currentScreenHandler.sendContentUpdates();
		}
	}
}
