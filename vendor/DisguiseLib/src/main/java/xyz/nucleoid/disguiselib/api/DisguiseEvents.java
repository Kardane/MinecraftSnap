package xyz.nucleoid.disguiselib.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

/**
 * Events for disguise lifecycle.
 */
public final class DisguiseEvents {

	/**
	 * Called before an entity is disguised.
	 */
	public static final Event<BeforeDisguise> BEFORE_DISGUISE = EventFactory.createArrayBacked(
			BeforeDisguise.class,
			listeners -> (entity, disguiseType) -> {
				for (BeforeDisguise listener : listeners) {
					if (!listener.beforeDisguise(entity, disguiseType)) {
						return false;
					}
				}
				return true;
			});

	/**
	 * Called after an entity has been disguised.
	 */
	public static final Event<AfterDisguise> AFTER_DISGUISE = EventFactory.createArrayBacked(
			AfterDisguise.class,
			listeners -> (entity, disguiseType) -> {
				for (AfterDisguise listener : listeners) {
					listener.afterDisguise(entity, disguiseType);
				}
			});

	/**
	 * Called before a disguise is removed.
	 */
	public static final Event<BeforeRemove> BEFORE_REMOVE = EventFactory.createArrayBacked(
			BeforeRemove.class,
			listeners -> (entity) -> {
				for (BeforeRemove listener : listeners) {
					if (!listener.beforeRemove(entity)) {
						return false;
					}
				}
				return true;
			});

	/**
	 * Called after a disguise has been removed.
	 */
	public static final Event<AfterRemove> AFTER_REMOVE = EventFactory.createArrayBacked(
			AfterRemove.class,
			listeners -> (entity) -> {
				for (AfterRemove listener : listeners) {
					listener.afterRemove(entity);
				}
			});

	@FunctionalInterface
	public interface BeforeDisguise {
		/**
		 * Called before an entity is disguised.
		 *
		 * @param entity       the entity being disguised
		 * @param disguiseType the type to disguise as
		 * @return true to allow the disguise, false to cancel
		 */
		boolean beforeDisguise(Entity entity, EntityType<?> disguiseType);
	}

	@FunctionalInterface
	public interface AfterDisguise {
		/**
		 * Called after an entity has been disguised.
		 *
		 * @param entity       the entity that was disguised
		 * @param disguiseType the type it was disguised as
		 */
		void afterDisguise(Entity entity, EntityType<?> disguiseType);
	}

	@FunctionalInterface
	public interface BeforeRemove {
		/**
		 * Called before a disguise is removed.
		 *
		 * @param entity the entity whose disguise is being removed
		 * @return true to allow removal, false to cancel
		 */
		boolean beforeRemove(Entity entity);
	}

	@FunctionalInterface
	public interface AfterRemove {
		/**
		 * Called after a disguise has been removed.
		 *
		 * @param entity the entity whose disguise was removed
		 */
		void afterRemove(Entity entity);
	}

	private DisguiseEvents() {
	}
}
