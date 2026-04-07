package karn.minecraftsnap.mixin;

import karn.minecraftsnap.game.ProjectilePickupPolicy;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class ArrowEntityMixin {
	@Shadow
	public PersistentProjectileEntity.PickupPermission pickupType;

	@Inject(method = "onBlockHit", at = @At("TAIL"))
	private void minecraftsnap$disableGroundArrowPickup(BlockHitResult hitResult, CallbackInfo ci) {
		if ((Object) this instanceof ArrowEntity && ProjectilePickupPolicy.shouldDisallowGroundArrowPickup(true)) {
			pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
		}
	}
}
