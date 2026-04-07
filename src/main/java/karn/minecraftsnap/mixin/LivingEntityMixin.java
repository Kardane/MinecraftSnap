package karn.minecraftsnap.mixin;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private float minecraftsnap$healthBeforeDamage;
	@Unique
	private DamageSource minecraftsnap$pendingDamageSource;

	@Inject(method = "damage", at = @At("HEAD"))
	private void minecraftsnap$captureHealthBeforeDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		minecraftsnap$healthBeforeDamage = ((LivingEntity) (Object) this).getHealth();
		minecraftsnap$pendingDamageSource = source;
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void minecraftsnap$resetProjectileInvulnerability(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		var mod = MinecraftSnap.getInstance();
		if (mod != null) {
			var entity = (LivingEntity) (Object) this;
			mod.handleLivingDamageApplied(entity, source, minecraftsnap$healthBeforeDamage, entity.getHealth(), cir.getReturnValueZ());
		}
		minecraftsnap$pendingDamageSource = null;
	}

	@ModifyArg(
		method = "damage",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/LivingEntity;applyDamage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)V"
		),
		index = 2
	)
	private float minecraftsnap$reduceLongRangeProjectileDamage(float amount) {
		return karn.minecraftsnap.game.InGameRuleService.adjustLongRangeProjectileDamage((LivingEntity) (Object) this, minecraftsnap$pendingDamageSource, amount);
	}

	@Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void minecraftsnap$blockCaptainDrop(net.minecraft.item.ItemStack stack, boolean dropAtSelf, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
		var mod = MinecraftSnap.getInstance();
		if (mod != null && (Object) this instanceof ServerPlayerEntity serverPlayer && mod.shouldBlockItemDrop(serverPlayer)) {
			cir.setReturnValue(null);
		}
	}
}
