package karn.minecraftsnap.mixin;

import karn.minecraftsnap.unit.nether.BlazeUnit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin {
	@Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$cancelTaggedBlazeFireballDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (entityHitResult == null) {
			return;
		}
		var projectile = (SmallFireballEntity) (Object) this;
		if (!projectile.getCommandTags().contains(BlazeUnit.ZERO_DAMAGE_FIREBALL_TAG)) {
			return;
		}
		Entity entity = entityHitResult.getEntity();
		if (entity != null && !entity.isFireImmune()) {
			entity.setOnFireFor(5);
		}
		ci.cancel();
	}
}
