package xyz.nucleoid.disguiselib.impl;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks disguised entities and provides statistics.
 */
@ApiStatus.Internal
public final class DisguiseTracker {

	private static final Set<Integer> DISGUISED_ENTITY_IDS = ConcurrentHashMap.newKeySet();
	private static final AtomicLong TOTAL_DISGUISE_COUNT = new AtomicLong(0);
	private static final AtomicLong PACKET_TRANSFORM_COUNT = new AtomicLong(0);
	private static final AtomicLong PACKET_TRANSFORM_TIME_NS = new AtomicLong(0);

	private DisguiseTracker() {
	}

	/**
	 * Registers an entity as disguised.
	 *
	 * @param entity the entity that was disguised
	 */
	public static void onDisguise(Entity entity) {
		if (DISGUISED_ENTITY_IDS.add(entity.getId())) {
			TOTAL_DISGUISE_COUNT.incrementAndGet();
		}
	}

	/**
	 * Unregisters an entity from being disguised.
	 *
	 * @param entity the entity whose disguise was removed
	 */
	public static void onRemoveDisguise(Entity entity) {
		DISGUISED_ENTITY_IDS.remove(entity.getId());
	}

	/**
	 * Returns the current number of disguised entities.
	 *
	 * @return current disguised entity count
	 */
	public static int getCurrentDisguisedCount() {
		return DISGUISED_ENTITY_IDS.size();
	}

	/**
	 * Returns the total number of disguises applied since server start.
	 *
	 * @return total disguise count
	 */
	public static long getTotalDisguiseCount() {
		return TOTAL_DISGUISE_COUNT.get();
	}

	/**
	 * Returns the set of currently disguised entity IDs.
	 *
	 * @return unmodifiable set of entity IDs
	 */
	public static Set<Integer> getDisguisedEntityIds() {
		return Collections.unmodifiableSet(DISGUISED_ENTITY_IDS);
	}

	/**
	 * Records a packet transformation for performance tracking.
	 *
	 * @param durationNs duration in nanoseconds
	 */
	public static void recordPacketTransform(long durationNs) {
		PACKET_TRANSFORM_COUNT.incrementAndGet();
		PACKET_TRANSFORM_TIME_NS.addAndGet(durationNs);
	}

	/**
	 * Returns the total number of packet transformations.
	 *
	 * @return packet transform count
	 */
	public static long getPacketTransformCount() {
		return PACKET_TRANSFORM_COUNT.get();
	}

	/**
	 * Returns the average packet transformation time in nanoseconds.
	 *
	 * @return average transformation time, or 0 if no transformations
	 */
	public static double getAveragePacketTransformTimeNs() {
		long count = PACKET_TRANSFORM_COUNT.get();
		return count > 0 ? (double) PACKET_TRANSFORM_TIME_NS.get() / count : 0;
	}

	/**
	 * Resets all statistics (for debugging/testing).
	 */
	public static void resetStats() {
		DISGUISED_ENTITY_IDS.clear();
		TOTAL_DISGUISE_COUNT.set(0);
		PACKET_TRANSFORM_COUNT.set(0);
		PACKET_TRANSFORM_TIME_NS.set(0);
	}
}
