package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowballEntity.class)
public abstract class SnowballEntityMixin {
	@Inject(method = "onEntityHit", at = @At("TAIL"))
	private void minecraftsnap$handleSnowballHit(EntityHitResult entityHitResult, CallbackInfo ci) {
		var mod = MinecraftSnap.getInstance();
		if (mod != null && entityHitResult != null) {
			mod.handleProjectileHit((SnowballEntity) (Object) this, entityHitResult.getEntity());
		}
	}
}
