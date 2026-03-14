package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "damage", at = @At("RETURN"))
	private void minecraftsnap$resetProjectileInvulnerability(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		var mod = MinecraftSnap.getInstance();
		if (mod != null) {
			mod.handleLivingDamageApplied((LivingEntity) (Object) this, source, cir.getReturnValueZ());
		}
	}
}
