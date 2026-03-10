package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.LivingEntityAccessor;

/**
 * Mixin to play disguise entity hurt/death sound when a disguised entity is
 * damaged/killed.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_DisguiseSound {

	@Unique
	private static final Logger LOGGER = LoggerFactory.getLogger("DisguiseLib/Sound");

	@Shadow
	protected abstract float getSoundVolume();

	@Shadow
	protected abstract float getSoundPitch();

	/**
	 * Plays the disguise entity's hurt or death sound when a disguised entity takes
	 * damage.
	 * The sound is not played for the disguised entity itself.
	 */
	@Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("RETURN"))
	private void playDisguiseSound(ServerWorld world, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		// Only proceed if damage was actually applied
		if (!cir.getReturnValue()) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		EntityDisguise disguise = (EntityDisguise) self;

		// 위장 상태 확인 - getDisguiseEntity()가 null이 아닌지도 함께 체크
		Entity disguiseEntity = disguise.getDisguiseEntity();
		if (disguiseEntity == null) {
			// 위장되지 않은 경우 스킵
			return;
		}

		// isDisguised()가 false여도 disguiseEntity가 있으면 이전 위장 상태가 남아있는 것
		// 이 경우 사운드를 재생하지 않아야 함
		if (!disguise.isDisguised()) {
			// LOGGER.info("[DisguiseLib] Entity {} has disguiseEntity but
			// isDisguised=false, skipping",
			// self.getName().getString());
			return;
		}

		if (!(disguiseEntity instanceof LivingEntity disguiseLiving)) {
			return;
		}

		// 사망 여부에 따라 사운드 선택
		boolean isDead = self.getHealth() <= 0;
		SoundEvent sound;

		if (isDead) {
			// 사망 사운드 재생
			sound = ((LivingEntityAccessor) disguiseLiving).invokeGetDeathSound();
			if (sound == null) {
				// LOGGER.info("[DisguiseLib] No death sound for disguise entity type: {}",
				// disguiseEntity.getType().getTranslationKey());
				return;
			}
			// LOGGER.info("[DisguiseLib] Playing DEATH sound for {} disguised as {}: {}",
			// self.getName().getString(), disguiseEntity.getType().getTranslationKey(),
			// sound);
		} else {
			// 피격 사운드 재생
			sound = ((LivingEntityAccessor) disguiseLiving).invokeGetHurtSound(source);
			if (sound == null) {
				return;
			}
			// LOGGER.info("[DisguiseLib] Playing HURT sound for {} disguised as {}: {}",
			// self.getName().getString(), disguiseEntity.getType().getTranslationKey(),
			// sound);
		}

		// Play sound to all players except the disguised entity itself
		disguiselib$playDisguisedSound(world, self, sound);
	}

	@Unique
	private void disguiselib$playDisguisedSound(ServerWorld world, LivingEntity self, SoundEvent sound) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			// Skip the disguised entity
			if (player.getId() == self.getId()) {
				continue;
			}

			// Skip players with TrueSight
			if (((EntityDisguise) player).hasTrueSight()) {
				continue;
			}

			// Send sound packet to the player
			player.networkHandler.sendPacket(new PlaySoundS2CPacket(
					Registries.SOUND_EVENT.getEntry(sound),
					SoundCategory.PLAYERS,
					self.getX(),
					self.getY(),
					self.getZ(),
					this.getSoundVolume(),
					this.getSoundPitch(),
					world.getRandom().nextLong()));
		}
	}
}
