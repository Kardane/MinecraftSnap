package karn.minecraftsnap.mixin;

import karn.minecraftsnap.unit.nether.GhastUnit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
	@Inject(method = "deflect", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$preventTaggedGhastFireballDeflection(
		ProjectileDeflection deflection,
		Entity deflector,
		Entity owner,
		boolean fromAttack,
		CallbackInfoReturnable<Boolean> cir
	) {
		if ((Object) this instanceof FireballEntity fireball && !GhastUnit.shouldAllowFireballDeflection(fireball.getCommandTags())) {
			cir.setReturnValue(false);
		}
	}
}
