package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireballEntity.class)
public abstract class FireballEntityMixin {
	@Inject(method = "onCollision", at = @At("TAIL"))
	private void minecraftsnap$handleFireballCollision(HitResult hitResult, CallbackInfo ci) {
		var mod = MinecraftSnap.getInstance();
		if (mod == null || hitResult == null) {
			return;
		}
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			mod.handleProjectileHit((FireballEntity) (Object) this, ((net.minecraft.util.hit.EntityHitResult) hitResult).getEntity());
			return;
		}
		if (hitResult.getType() == HitResult.Type.BLOCK) {
			mod.handleProjectileImpact((FireballEntity) (Object) this, hitResult.getPos());
		}
	}
}
