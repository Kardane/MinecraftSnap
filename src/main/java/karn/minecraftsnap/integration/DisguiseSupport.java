package karn.minecraftsnap.integration;

import karn.minecraftsnap.config.EntitySpecEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final class DisguiseSupport {
	private static final String ENTITY_DISGUISE_CLASS = "xyz.nucleoid.disguiselib.api.EntityDisguise";
	private static final Logger LOGGER = LoggerFactory.getLogger("MCsnap/DisguiseSupport");

	private DisguiseSupport() {
	}

	public static void applyDisguise(ServerPlayerEntity player, EntitySpecEntry disguise) {
		if (disguise == null || disguise.isEmpty()) {
			clearDisguise(player);
			return;
		}

		try {
			var disguiseClass = Class.forName(ENTITY_DISGUISE_CLASS);
			if (!disguiseClass.isInstance(player)) {
				return;
			}
			var entityType = Registries.ENTITY_TYPE.get(Identifier.of(disguise.entityId));
			if (entityType == EntityType.PLAYER) {
				return;
			}
			var disguiseEntity = createDisguiseEntity(player, disguise);
			if (disguiseEntity != null && invokeDisguiseAsEntity(player, disguiseClass, disguiseEntity)) {
				return;
			}
			invokeDisguiseAsType(player, disguiseClass, entityType);
			if (disguiseEntity == null && !disguise.entityNbt.isBlank()) {
				LOGGER.warn("변장 NBT 적용 실패, 엔티티 타입만 사용: {}", disguise.entityId);
			}
		} catch (Exception ignored) {
		}
	}

	public static void clearDisguise(ServerPlayerEntity player) {
		try {
			var disguiseClass = Class.forName(ENTITY_DISGUISE_CLASS);
			if (!disguiseClass.isInstance(player)) {
				return;
			}
			disguiseClass.getMethod("removeDisguise").invoke(player);
		} catch (Exception ignored) {
		}
	}

	private static Entity createDisguiseEntity(ServerPlayerEntity player, EntitySpecEntry disguise) {
		if (disguise.entityNbt == null || disguise.entityNbt.isBlank()) {
			return null;
		}
		try {
			var nbt = StringNbtReader.readCompound(disguise.entityNbt);
			nbt.putString("id", disguise.entityId);
			return EntityType.loadEntityWithPassengers(nbt, player.getWorld(), SpawnReason.COMMAND, entity -> entity);
		} catch (Exception exception) {
			LOGGER.warn("변장 엔티티 NBT 파싱 실패: {}", disguise.entityId, exception);
			return null;
		}
	}

	private static boolean invokeDisguiseAsEntity(ServerPlayerEntity player, Class<?> disguiseClass, Entity entity) {
		var method = findMethod(disguiseClass, "disguiseAs", entity.getClass(), Entity.class);
		if (method == null) {
			return false;
		}
		try {
			method.invoke(player, entity);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static void invokeDisguiseAsType(ServerPlayerEntity player, Class<?> disguiseClass, EntityType<?> entityType) throws Exception {
		var method = findMethod(disguiseClass, "disguiseAs", EntityType.class);
		if (method != null) {
			method.invoke(player, entityType);
		}
	}

	private static Method findMethod(Class<?> owner, String name, Class<?>... preferredTypes) {
		for (var preferredType : preferredTypes) {
			try {
				return owner.getMethod(name, preferredType);
			} catch (NoSuchMethodException ignored) {
			}
		}
		for (var method : owner.getMethods()) {
			if (!method.getName().equals(name) || method.getParameterCount() != 1) {
				continue;
			}
			for (var preferredType : preferredTypes) {
				if (method.getParameterTypes()[0].isAssignableFrom(preferredType)) {
					return method;
				}
			}
		}
		return null;
	}
}
