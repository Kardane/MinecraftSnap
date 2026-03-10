package karn.minecraftsnap.integration;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class DisguiseSupport {
	private static final String ENTITY_DISGUISE_CLASS = "xyz.nucleoid.disguiselib.api.EntityDisguise";

	private DisguiseSupport() {
	}

	public static void applyDisguise(ServerPlayerEntity player, String disguiseId) {
		if (disguiseId == null || disguiseId.isBlank()) {
			clearDisguise(player);
			return;
		}

		try {
			var disguiseClass = Class.forName(ENTITY_DISGUISE_CLASS);
			if (!disguiseClass.isInstance(player)) {
				return;
			}
			var entityType = Registries.ENTITY_TYPE.get(Identifier.of(disguiseId));
			if (entityType == EntityType.PLAYER) {
				return;
			}
			disguiseClass.getMethod("disguiseAs", EntityType.class).invoke(player, entityType);
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
}
