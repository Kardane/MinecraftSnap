package xyz.nucleoid.disguiselib.impl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.disguiselib.api.DisguiseUtils;
import xyz.nucleoid.disguiselib.api.EntityDisguise;
import xyz.nucleoid.disguiselib.impl.DisguiseTracker;
import xyz.nucleoid.disguiselib.impl.mixin.accessor.*;
import xyz.nucleoid.disguiselib.impl.packets.ExtendedHandler;
import xyz.nucleoid.disguiselib.impl.packets.FakePackets;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static xyz.nucleoid.disguiselib.impl.DisguiseLib.DISGUISE_TEAM;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin_Disguiser extends ServerCommonNetworkHandler
		implements ExtendedHandler {
	@Shadow
	public ServerPlayerEntity player;

	@Unique
	private boolean disguiselib$sentTeamPacket;

	public ServerPlayNetworkHandlerMixin_Disguiser(MinecraftServer server, ClientConnection connection,
			ConnectedClientData clientData) {
		super(server, connection, clientData);
	}

	public void disguiselib$transformPacket(Packet<? super ClientPlayPacketListener> packet, Runnable remove,
			Consumer<Packet<ClientPlayPacketListener>> add) {
		long startTime = System.nanoTime();

		try {
			World world = this.player.getEntityWorld();
			if (packet instanceof EntitySpawnS2CPacket) {
				int entityId = ((EntitySpawnS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 스폰 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				var entity = world.getEntityById(entityId);

				// entity가 null이면 UUID로 PlayerManager에서 플레이어 조회 (Carpet 봇 재스폰 시)
				if (entity == null && world instanceof ServerWorld serverWorld) {
					UUID uuid = ((EntitySpawnS2CPacketAccessor) packet).getUuid();
					if (uuid != null) {
						entity = serverWorld.getServer().getPlayerManager().getPlayer(uuid);
					}
				}

				if (entity != null) {
					disguiselib$sendFakePacket(entity, remove, add);
				}
			} else if (packet instanceof EntitiesDestroyS2CPacket
					&& !((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().isEmpty()
					&& ((EntitiesDestroyS2CPacketAccessor) packet).getEntityIds().getInt(0) == this.player.getId()) {
				remove.run();
				return;
			} else if (packet instanceof EntityTrackerUpdateS2CPacket) {
				int entityId = ((EntityTrackerUpdateS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 DataTracker 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				// TrueSight가 있으면 변환하지 않음
				if (((EntityDisguise) this.player).hasTrueSight()) {
					return;
				}

				Entity original = world.getEntityById(entityId);
				if (original == null) {
					return;
				}

				EntityDisguise disguise = (EntityDisguise) original;
				if (!disguise.isDisguised()) {
					return;
				}

				// 변장된 엔티티의 DataTracker 패킷은 완전히 차단
				// 원본 플레이어의 DataTracker 필드 타입이 변장 엔티티와 다르므로
				// 클라이언트에 전송하면 타입 불일치로 크래시 발생
				// 하지만 Shared Flag(0번 인덱스 - 달리기, 불, 웅크리기 등)는 동기화 필요
				List<DataTracker.SerializedEntry<?>> trackedValues = ((EntityTrackerUpdateS2CPacketAccessor) packet)
						.getTrackedValues();
				boolean hasSharedFlags = false;

				// DEBUG: Print tracked values (Removed)
				if (trackedValues != null) {
					for (DataTracker.SerializedEntry<?> entry : trackedValues) {
						if (entry.id() == 0) {
							hasSharedFlags = true;
							break;
						}
					}
				}

				if (hasSharedFlags) {
					((DisguiseUtils) disguise).updateTrackedData();
					Entity disguiseEntity = disguise.getDisguiseEntity();
					if (disguiseEntity != null) {
						var dataTracker = disguiseEntity.getDataTracker();
						if (dataTracker != null) {
							// updateTrackedData에서 setSprinting 등을 호출하므로 값은 변경되었으나,
							// dirty check에서 걸러졌을 수 있음. getChangedEntries() 결과를 가져옴.
							var allEntries = dataTracker.getChangedEntries();
							if (allEntries == null) {
								allEntries = new ArrayList<>();
							} else {
								allEntries = new ArrayList<>(allEntries); // Ensure mutable
							}

							// Check if flags (Index 0) are present
							boolean flagsPresent = false;
							for (var entry : allEntries) {
								if (entry.id() == 0) {
									flagsPresent = true;
									break;
								}
							}

							// If not present, manually add current value
							if (!flagsPresent) {
								try {
									var flagsData = EntityAccessor.getFLAGS();
									byte currentFlags = dataTracker.get(flagsData);
									// DataTracker.SerializedEntry record: (int id, TrackedDataHandler<T> handler, T
									// value)
									// TrackedData has dataType() which returns the handler
									allEntries.add(new DataTracker.SerializedEntry<>(flagsData.id(),
											flagsData.dataType(), currentFlags));
								} catch (Exception e) {
									// Ignore
								}
							}

							if (!allEntries.isEmpty()) {
								add.accept(new EntityTrackerUpdateS2CPacket(entityId, allEntries));
							}
						}
					}
				} else {
					// No shared flags found. Dropping packet.
				}

				remove.run();
				return;
			} else if (packet instanceof EntityAttributesS2CPacket && !((EntityDisguise) this.player).hasTrueSight()) {
				int entityId = ((EntityAttributesS2CPacketAccessor) packet).getEntityId();

				// 자기 자신의 속성 패킷은 변환하지 않음
				if (entityId == this.player.getId()) {
					return;
				}

				Entity original = world.getEntityById(entityId);
				if (original != null) {
					EntityDisguise entityDisguise = (EntityDisguise) original;
					if (entityDisguise.isDisguised() && !((DisguiseUtils) original).disguiseAlive()) {
						remove.run();
						return;
					}
				}
			} else if (packet instanceof ItemPickupAnimationS2CPacket pickupPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, pickupPacket.getCollectorEntityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, equipmentPacket.getEntityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof EntityStatusEffectS2CPacket effectPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, effectPacket.getEntityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof RemoveEntityStatusEffectS2CPacket effectPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, effectPacket.entityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof DamageTiltS2CPacket damageTiltPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, damageTiltPacket.id())) {
					remove.run();
					return;
				}
			} else if (packet instanceof EntityAnimationS2CPacket animationPacket
					&& !((EntityDisguise) this.player).hasTrueSight()) {
				if (this.disguiselib$shouldDropForNonLivingDisguise(world, animationPacket.getEntityId())) {
					remove.run();
					return;
				}
			} else if (packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
				int id = velocityPacket.getEntityId();
				if (id != this.player.getId()) {
					Entity entity1 = world.getEntityById(id);
					if (entity1 != null && ((EntityDisguise) entity1).isDisguised()) {
						remove.run();
					}
				}
			}
		} finally {
			long duration = System.nanoTime() - startTime;
			DisguiseTracker.recordPacketTransform(duration);
		}
	}

	@Unique
	private boolean disguiselib$shouldDropForNonLivingDisguise(World world, int entityId) {
		if (entityId == this.player.getId()) {
			return false;
		}

		Entity entity = world.getEntityById(entityId);
		if (entity == null) {
			return false;
		}

		EntityDisguise entityDisguise = (EntityDisguise) entity;
		return entityDisguise.isDisguised() && !((DisguiseUtils) entity).disguiseAlive();
	}

	/**
	 * Sends fake packet instead of the real one.
	 *
	 * @param entity the entity that is disguised and needs to have a custom packet
	 *               sent.
	 */
	@Unique
	private void disguiselib$sendFakePacket(Entity entity, Runnable remove,
			Consumer<Packet<ClientPlayPacketListener>> add) {
		EntityDisguise disguise = (EntityDisguise) entity;

		// 자기 자신에게는 변장 패킷을 보내지 않음
		if (entity.getId() == this.player.getId()) {
			return;
		}

		// TrueSight가 있거나 변장되지 않은 경우 원본 패킷 유지
		if (((EntityDisguise) this.player).hasTrueSight() || !disguise.isDisguised()) {
			return;
		}

		Entity disguiseEntity = disguise.getDisguiseEntity();
		if (disguiseEntity == null) {
			return;
		}

		try {
			Packet<?> spawnPacket;
			var entry = new EntityTrackerEntry((ServerWorld) entity.getEntityWorld(), entity, 1, true,
					new EntityTrackerEntry.TrackerPacketSender() {
						@Override
						public void sendToListeners(Packet<? super ClientPlayPacketListener> packet) {
						}

						@Override
						public void sendToSelfAndListeners(Packet<? super ClientPlayPacketListener> packet) {
						}

						@Override
						public void sendToListenersIf(Packet<? super ClientPlayPacketListener> packet,
								Predicate<ServerPlayerEntity> predicate) {
						}
					});

			spawnPacket = FakePackets.universalSpawnPacket(entity, entry, true);
			add.accept((Packet<ClientPlayPacketListener>) spawnPacket);

			// 변장 엔티티의 DataTracker 초기 값도 함께 전송 (NBT 태그 상태 반영)
			var dataTracker = disguiseEntity.getDataTracker();
			if (dataTracker != null) {
				var allEntries = dataTracker.getChangedEntries();
				if (allEntries != null && !allEntries.isEmpty()) {
					add.accept(new EntityTrackerUpdateS2CPacket(entity.getId(), allEntries));
				}
			}

			remove.run();
		} catch (Exception e) {
			// 스폰 패킷 생성 중 예외 발생 시 원본 패킷 유지
		}
	}

	@Inject(method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V", shift = At.Shift.AFTER))
	private void disguiselib$moveDisguiseEntity(PlayerMoveC2SPacket packet, CallbackInfo ci) {
		// 자기 자신에게는 변장 엔티티 위치 업데이트를 보내지 않음
		// 다른 플레이어들에게만 전송됨
	}

	public void disguiselib$onClientBrand() {
		if (!this.disguiselib$sentTeamPacket) {
			TeamS2CPacket addTeamPacket = TeamS2CPacket.updateTeam(DISGUISE_TEAM, true);
			this.disguiselib$sentTeamPacket = true;
			this.sendPacket(addTeamPacket);

			if (((EntityDisguise) this.player).isDisguised()) {
				TeamS2CPacket joinTeamPacket = TeamS2CPacket.changePlayerTeam(DISGUISE_TEAM,
						this.player.getGameProfile().name(), TeamS2CPacket.Operation.ADD);
				this.sendPacket(joinTeamPacket);
			}
		}
	}
}
