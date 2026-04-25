package karn.minecraftsnap.integration;

import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.VanillaPlayerTeamService;
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
import java.util.function.Consumer;

public final class DisguiseSupport {
	private static final String ENTITY_DISGUISE_CLASS = "xyz.nucleoid.disguiselib.api.EntityDisguise";
	private static final Logger LOGGER = LoggerFactory.getLogger("MCsnap/DisguiseSupport");

	private DisguiseSupport() {
	}

	public static void applyDisguise(ServerPlayerEntity player, EntitySpecEntry disguise) {
		applyDisguise(player, disguise, null);
	}

	public static void applyTeamDisguise(ServerPlayerEntity player, EntitySpecEntry disguise, TeamId teamId) {
		applyDisguise(player, disguise, entity -> assignEntityTeam(player, entity, teamId));
	}

	public static void applyDisguise(ServerPlayerEntity player, EntitySpecEntry disguise, Consumer<Entity> entityCustomizer) {
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
			if (!shouldCreateDisguiseEntity(disguise, entityCustomizer != null) && invokeDisguiseAsType(player, disguiseClass, entityType)) {
				return;
			}
			var disguiseEntity = createDisguiseEntity(player, disguise, entityType, entityCustomizer);
			if (disguiseEntity != null && invokeDisguiseAsEntity(player, disguiseClass, disguiseEntity)) {
				if (entityCustomizer != null) {
					entityCustomizer.accept(disguiseEntity);
				}
				return;
			}
			if (invokeDisguiseAsType(player, disguiseClass, entityType)) {
				return;
			}
			if (disguiseEntity == null && !disguise.entityNbt.isBlank()) {
				LOGGER.warn("변장 NBT 적용 실패, 엔티티 타입만 사용: {}", disguise.entityId);
			}
		} catch (Exception ignored) {
		}
	}

	private static void assignEntityTeam(ServerPlayerEntity player, Entity entity, TeamId teamId) {
		if (player == null || player.getServer() == null || entity == null || teamId == null) {
			return;
		}
		var teamService = new VanillaPlayerTeamService();
		var scoreboard = player.getServer().getScoreboard();
		teamService.assignScoreHolder(scoreboard, entity.getNameForScoreboard(), teamId);
		teamService.assignScoreHolder(scoreboard, entity.getUuidAsString(), teamId);
		teamService.assignScoreHolder(scoreboard, entity.getUuid().toString(), teamId);
		teamService.assignScoreHolder(scoreboard, player.getNameForScoreboard(), teamId);
	}

	static boolean shouldCreateDisguiseEntity(EntitySpecEntry disguise, boolean hasEntityCustomizer) {
		return hasEntityCustomizer || (disguise != null && disguise.entityNbt != null && !disguise.entityNbt.isBlank());
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

	private static Entity createDisguiseEntity(
		ServerPlayerEntity player,
		EntitySpecEntry disguise,
		EntityType<?> entityType,
		Consumer<Entity> entityCustomizer
	) {
		try {
			Entity entity;
			if (disguise.entityNbt == null || disguise.entityNbt.isBlank()) {
				entity = entityType.create(player.getWorld(), SpawnReason.COMMAND);
			} else {
				var nbt = StringNbtReader.readCompound(disguise.entityNbt);
				nbt.putString("id", disguise.entityId);
				entity = EntityType.loadEntityWithPassengers(nbt, player.getWorld(), SpawnReason.COMMAND, loaded -> loaded);
			}
			if (entity != null) {
				entity.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
			}
			if (entity != null && entityCustomizer != null) {
				entityCustomizer.accept(entity);
			}
			return entity;
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

	private static boolean invokeDisguiseAsType(ServerPlayerEntity player, Class<?> disguiseClass, EntityType<?> entityType) throws Exception {
		var method = findMethod(disguiseClass, "disguiseAs", EntityType.class);
		if (method == null) {
			return false;
		}
		method.invoke(player, entityType);
		return true;
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
