package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	public ServerPlayerEntity player;

	@Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$handleShortcut(PlayerActionC2SPacket packet, CallbackInfo ci) {
		if (packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND || !player.isSneaking()) {
			return;
		}

		var mod = MinecraftSnap.getInstance();
		if (mod != null && mod.handleShortcut(player)) {
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
}
