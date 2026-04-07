package karn.minecraftsnap.biome;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

public class ColdOceanBiomeEffect extends NoOpBiomeEffect {
	private static final int FREEZE_TICKS_PER_SECOND = 60;

	@Override
	public void onTick(BiomeRuntimeContext context) {
		super.onTick(context);
		var entities = affectedEntities(context);
		for (var entity : entities) {
			if (!shouldApplyFreeze(context.serverTicks(), entity.isTouchingWater())) {
				continue;
			}
			entity.setFrozenTicks(nextFrozenTicks(entity.getFrozenTicks()));
		}
	}

	private java.util.List<LivingEntity> affectedEntities(BiomeRuntimeContext context) {
		if (context == null || context.world() == null || context.laneRuntime() == null || context.laneRuntime().laneRegion() == null) {
			return java.util.List.of();
		}
		var laneRegion = context.laneRuntime().laneRegion();
		var box = new Box(laneRegion.minX, laneRegion.minY, laneRegion.minZ, laneRegion.maxX, laneRegion.maxY, laneRegion.maxZ);
		return context.world().getEntitiesByClass(LivingEntity.class, box, this::isAffectedLivingEntity);
	}

	private boolean isAffectedLivingEntity(LivingEntity entity) {
		return entity != null
			&& entity.isAlive()
			&& (!(entity instanceof ServerPlayerEntity player) || !player.isSpectator());
	}

	boolean shouldApplyFreeze(long serverTicks, boolean touchingWater) {
		return touchingWater && serverTicks % 20L == 0L;
	}

	int nextFrozenTicks(int currentFrozenTicks) {
		return Math.max(0, currentFrozenTicks) + FREEZE_TICKS_PER_SECOND;
	}
}
