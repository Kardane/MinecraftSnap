package karn.minecraftsnap.mixin;

import karn.minecraftsnap.ui.PlayerDisplayNameHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	@Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$overrideDisplayName(CallbackInfoReturnable<Text> cir) {
		if ((Object) this instanceof PlayerDisplayNameHolder holder) {
			var displayName = holder.minecraftsnap$getStyledDisplayName();
			if (displayName != null) {
				cir.setReturnValue(displayName.copy());
			}
		}
	}
}
