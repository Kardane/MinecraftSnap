package xyz.nucleoid.disguiselib.impl.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseEvents;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.DisguiseTracker;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.EntityTrackerEntryAccessor;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.ServerChunkLoadingManagerAccessor;

import java.util.*;
import java.util.stream.Collectors;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(Entity.class)
public abstract class EntityMixin_Disguise implements EntityDisguise, DisguiseUtils {

	@Unique
	private final Entity disguiselib$entity = (Entity) (Object) this;
	@Shadow
	public World world;
	@Shadow
	protected UUID uuid;
	@Unique
	private Entity disguiselib$disguiseEntity;
	@Unique
	private int disguiselib$ticks;
	@Unique
	private EntityType<?> disguiselib$disguiseType;
	@Unique
	private boolean disguiselib$trueSight = false;

	@Shadow
	public abstract EntityType<?> getType();

	@Shadow
	public abstract float getHeadYaw();

	@Shadow
	public abstract Text getName();

	@Shadow
	public abstract DataTracker getDataTracker();

	@Shadow
	@Nullable
	public abstract Text getCustomName();

	@Shadow
	public abstract boolean isCustomNameVisible();

	@Shadow
	public abstract boolean isSprinting();

	@Shadow
	public abstract boolean isSneaking();

	@Shadow
	public abstract boolean isSwimming();

	@Shadow
	public abstract boolean isGlowing();

	@Shadow
	public abstract boolean isSilent();

	@Shadow
	private int id;

	@Shadow
	public abstract EntityPose getPose();

	@Shadow
	public abstract int getId();

	@Shadow
	public abstract boolean isOnFire();

	@Shadow
	public abstract Text getDisplayName();

	@Shadow
	protected abstract void addPassenger(Entity passenger);

	@Shadow
	private boolean onGround;

	/**
	 * Tells you the disguised status.
	 *
	 * @return true if entity is disguised, otherwise false.
	 */
	@Override
	public boolean isDisguised() {
		return this.disguiselib$disguiseEntity != null;
	}

	/**
	 * Sets entity's disguise from {@link EntityType}
	 *
	 * @param entityType the type to disguise this entity into
	 */
	@Override
	public void disguiseAs(EntityType<?> entityType) {
		// 플레이어 타입은 지원하지 않음
		if (entityType == EntityType.PLAYER) {
			return;
		}

		// 이벤트 호출 - 취소 가능
		if (!DisguiseEvents.BEFORE_DISGUISE.invoker().beforeDisguise(this.disguiselib$entity, entityType)) {
			return;
		}

		this.disguiselib$disguiseType = entityType;

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
			this.disguiselib$hideSelfView();
		}

		// 변장 엔티티가 없거나 다른 타입인 경우 새로 생성
		if (this.disguiselib$disguiseEntity == null || this.disguiselib$disguiseEntity.getType() != entityType) {
			this.disguiselib$disguiseEntity = entityType.create(world, SpawnReason.LOAD);
		}

		// Fix some client predictions
		if (this.disguiselib$disguiseEntity instanceof MobEntity) {
			((MobEntity) this.disguiselib$disguiseEntity).setAiDisabled(true);
		}

		// Minor datatracker thingies
		this.updateTrackedData();

		// 트래커 업데이트 - null 체크 추가
		if (this.world instanceof ServerWorld serverWorld) {
			var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
			if (chunkLoadingManager != null) {
				var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
				if (trackers != null) {
					var tracker = trackers.get(this.getId());
					if (tracker != null) {
						for (var listener : tracker.getListeners()) {
							tracker.getEntry().stopTracking(listener.getPlayer());
							tracker.getEntry().startTracking(listener.getPlayer());
						}
					}
				}
			}
		}

		// 트래커에 등록
		DisguiseTracker.onDisguise(this.disguiselib$entity);

		// 이벤트 호출
		DisguiseEvents.AFTER_DISGUISE.invoker().afterDisguise(this.disguiselib$entity, entityType);
	}

	/**
	 * Sets entity's disguise from {@link Entity}
	 *
	 * @param entity the entity to disguise into
	 */
	@Override
	public void disguiseAs(Entity entity) {
		// 플레이어로 변장 시도 시 무시
		if (entity instanceof PlayerEntity) {
			return;
		}

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
			this.disguiselib$hideSelfView();
		}

		this.disguiselib$disguiseEntity = entity;
		this.disguiseAs(entity.getType());
	}

	/**
	 * Clears the disguise - sets the
	 * {@link EntityMixin_Disguise#disguiselib$disguiseType} back to original.
	 */
	@Override
	public void removeDisguise() {
		if (!this.isDisguised()) {
			return;
		}

		// 이벤트 호출 - 취소 가능
		if (!DisguiseEvents.BEFORE_REMOVE.invoker().beforeRemove(this.disguiselib$entity)) {
			return;
		}

		if (this.disguiselib$disguiseEntity != null && this.disguiselib$entity instanceof ServerPlayerEntity) {
			this.disguiselib$hideSelfView();
		}

		// 트래커에서 제거
		DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);

		// Setting as not-disguised
		this.disguiselib$disguiseEntity = null;
		this.disguiselib$disguiseType = null;

		// 트래커 업데이트
		if (this.world instanceof ServerWorld serverWorld) {
			var chunkLoadingManager = serverWorld.getChunkManager().chunkLoadingManager;
			if (chunkLoadingManager != null) {
				var trackers = ((ServerChunkLoadingManagerAccessor) chunkLoadingManager).getEntityTrackers();
				if (trackers != null) {
					var tracker = trackers.get(this.getId());
					if (tracker != null) {
						for (var listener : tracker.getListeners()) {
							tracker.getEntry().stopTracking(listener.getPlayer());
							tracker.getEntry().startTracking(listener.getPlayer());
						}
					}
				}
			}
		}

		// 이벤트 호출
		DisguiseEvents.AFTER_REMOVE.invoker().afterRemove(this.disguiselib$entity);
	}

	/**
	 * Gets the disguise entity type
	 *
	 * @return disguise entity type or real type if there's no disguise
	 */
	@Override
	public EntityType<?> getDisguiseType() {
		return this.disguiselib$disguiseType != null ? this.disguiselib$disguiseType : this.getType();
	}

	/**
	 * Gets the disguise entity.
	 *
	 * @return disguise entity or null if there's no disguise
	 */
	@Nullable
	@Override
	public Entity getDisguiseEntity() {
		return this.disguiselib$disguiseEntity;
	}

	/**
	 * Whether disguise type entity is an instance of {@link LivingEntity}.
	 *
	 * @return true if the disguise type is an instance of {@link LivingEntity},
	 *         otherwise false.
	 */
	@Override
	public boolean disguiseAlive() {
		return this.disguiselib$disguiseEntity instanceof LivingEntity;
	}

	/**
	 * Whether this entity can bypass the
	 * "disguises" and see entities normally
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @return if entity can be "fooled" by disguise
	 */
	@Override
	public boolean hasTrueSight() {
		return this.disguiselib$trueSight;
	}

	/**
	 * Toggles true sight - whether entity
	 * can see disguises or not.
	 * Intended more for admins (to not get trolled themselves).
	 *
	 * @param trueSight if entity should not see disguises
	 */
	@Override
	public void setTrueSight(boolean trueSight) {
		this.disguiselib$trueSight = trueSight;
	}

	/**
	 * Hides player's self-disguise-entity
	 */
	@Unique
	private void disguiselib$hideSelfView() {
		if (!(this.disguiselib$entity instanceof ServerPlayerEntity player)) {
			return;
		}
		if (this.disguiselib$disguiseEntity == null) {
			return;
		}

		player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(this.disguiselib$disguiseEntity.getId()));
		TeamS2CPacket removeTeamPacket = TeamS2CPacket.changePlayerTeam(DISGUISE_TEAM, player.getGameProfile().name(),
				TeamS2CPacket.Operation.REMOVE);
		player.networkHandler.sendPacket(removeTeamPacket);
	}

	/**
	 * Gets equipment as list of {@link Pair Pairs}.
	 * Requires entity to be an instanceof {@link LivingEntity}.
	 *
	 * @return equipment list of pairs.
	 */
	@Unique
	private List<Pair<EquipmentSlot, ItemStack>> disguiselib$getEquipment() {
		if (disguiselib$entity instanceof LivingEntity) {
			return Arrays.stream(EquipmentSlot.values())
					.map(slot -> new Pair<>(slot, ((LivingEntity) disguiselib$entity).getEquippedStack(slot)))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Updates custom name and its visibility.
	 * Also sets no-gravity to true in order
	 * to prevent the client from predicting
	 * the entity position and velocity.
	 */
	@Override
	public void updateTrackedData() {
		if (this.disguiselib$disguiseEntity == null) {
			return;
		}

		this.disguiselib$disguiseEntity.setNoGravity(true);
		this.disguiselib$disguiseEntity.setCustomName(this.getCustomName());
		this.disguiselib$disguiseEntity.setCustomNameVisible(this.isCustomNameVisible());
		this.disguiselib$disguiseEntity.setSprinting(this.isSprinting());
		this.disguiselib$disguiseEntity.setSneaking(this.isSneaking());
		this.disguiselib$disguiseEntity.setSwimming(this.isSwimming());
		this.disguiselib$disguiseEntity.setGlowing(this.isGlowing());
		this.disguiselib$disguiseEntity.setOnFire(this.isOnFire());
		this.disguiselib$disguiseEntity.setSilent(this.isSilent());
		this.disguiselib$disguiseEntity.setPose(this.getPose());

		if (this.disguiselib$disguiseEntity instanceof LivingEntity disguise
				&& ((Object) this) instanceof LivingEntity self) {
			disguise.getAttributes().setFrom(self.getAttributes());
		}
	}

	/**
	 * Sends additional move packets to the client if
	 * entity is disguised.
	 * Prevents client desync and fixes "blocky" movement.
	 */
	@Inject(method = "tick()V", at = @At("TAIL"))
	private void postTick(CallbackInfo ci) {
		if (!this.isDisguised()) {
			return;
		}

		if (this.world.getServer() != null && !(this.disguiselib$disguiseEntity instanceof LivingEntity)
				&& !(this.disguiselib$entity instanceof PlayerEntity)) {
			this.world.getServer().getPlayerManager().sendToDimension(
					new EntityPositionS2CPacket(
							this.disguiselib$entity.getId(),
							new EntityPosition(
									this.disguiselib$entity.getSyncedPos(),
									this.disguiselib$entity.getVelocity(),
									this.disguiselib$entity.getYaw(),
									this.disguiselib$entity.getPitch()),
							Set.of(), this.onGround),
					this.world.getRegistryKey());
		} else if (this.disguiselib$entity instanceof ServerPlayerEntity && ++this.disguiselib$ticks % 40 == 0) {
			// MutableText msg = Text.literal("You are disguised as ")
			// .append(Text.translatable(this.disguiselib$disguiseEntity.getType().getTranslationKey()))
			// .formatted(Formatting.GREEN);

			// ((ServerPlayerEntity) this.disguiselib$entity).sendMessage(msg, true);
			this.disguiselib$ticks = 0;
		}
	}

	/**
	 * If entity is disguised, we need to clean up on discard.
	 */
	@Inject(method = "discard()V", at = @At("TAIL"))
	private void onRemove(CallbackInfo ci) {
		if (this.isDisguised()) {
			DisguiseTracker.onRemoveDisguise(this.disguiselib$entity);
		}
	}

	/**
	 * Takes care of loading the fake entity data from tag.
	 *
	 * @param tag tag to load data from.
	 */
	@Inject(method = "readData", at = @At("TAIL"))
	private void fromTag(ReadView tag, CallbackInfo ci) {
		var disguiseTag = tag.getOptionalReadView("DisguiseLib");

		if (disguiseTag.isPresent()) {
			Identifier disguiseTypeId = Identifier.tryParse(disguiseTag.get().getString("DisguiseType", ""));
			if (disguiseTypeId == null) {
				return;
			}

			this.disguiselib$disguiseType = Registries.ENTITY_TYPE.get(disguiseTypeId);

			// 플레이어 타입은 무시
			if (this.disguiselib$disguiseType == EntityType.PLAYER) {
				this.disguiselib$disguiseType = null;
				return;
			}

			var disguiseEntityTag = disguiseTag.get().getOptionalReadView("DisguiseEntity");
			if (disguiseEntityTag.isPresent()) {
				this.disguiselib$disguiseEntity = EntityType.loadEntityWithPassengers(
						disguiseEntityTag.get(), this.world, SpawnReason.LOAD, (entityx) -> entityx);
			}
		}
	}

	/**
	 * Takes care of saving the fake entity data to tag.
	 *
	 * @param tag tag to save data to.
	 */
	@Inject(method = "writeData", at = @At("TAIL"))
	private void toTag(WriteView tag, CallbackInfo ci) {
		if (this.isDisguised() && this.disguiselib$disguiseType != null) {
			var disguiseTag = tag.get("DisguiseLib");

			disguiseTag.putString("DisguiseType",
					Registries.ENTITY_TYPE.getId(this.disguiselib$disguiseType).toString());

			if (this.disguiselib$disguiseEntity != null
					&& !this.disguiselib$entity.equals(this.disguiselib$disguiseEntity)) {
				var disguiseEntityTag = disguiseTag.get("DisguiseEntity");
				this.disguiselib$disguiseEntity.writeData(disguiseEntityTag);

				Identifier identifier = Registries.ENTITY_TYPE.getId(this.disguiselib$disguiseEntity.getType());
				disguiseEntityTag.putString("id", identifier.toString());
			}
		}
	}
}
