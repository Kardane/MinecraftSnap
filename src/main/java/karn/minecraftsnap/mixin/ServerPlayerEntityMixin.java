package karn.minecraftsnap.mixin;

import karn.minecraftsnap.ui.PlayerDisplayNameHolder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerDisplayNameHolder {
	@Unique
	private Text minecraftsnap$styledDisplayName;

	@Unique
	private Text minecraftsnap$playerListDisplayName;

	@Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$overridePlayerListName(CallbackInfoReturnable<Text> cir) {
		if (minecraftsnap$playerListDisplayName != null) {
			cir.setReturnValue(minecraftsnap$playerListDisplayName.copy());
		}
	}

	@Override
	public Text minecraftsnap$getStyledDisplayName() {
		return minecraftsnap$styledDisplayName;
	}

	@Override
	public void minecraftsnap$setStyledDisplayName(Text displayName) {
		this.minecraftsnap$styledDisplayName = displayName == null ? null : displayName.copy();
	}

	@Override
	public Text minecraftsnap$getPlayerListDisplayName() {
		return minecraftsnap$playerListDisplayName;
	}

	@Override
	public void minecraftsnap$setPlayerListDisplayName(Text displayName) {
		this.minecraftsnap$playerListDisplayName = displayName == null ? null : displayName.copy();
	}
}
