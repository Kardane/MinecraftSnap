package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaneStructureServiceTest {
	@Test
	void originUsesCenteredFixedStructureSize() {
		var service = new LaneStructureService((server, worldId, structureId, originPos) -> true);
		var region = SystemConfig.LaneRegionConfig.create(-8.0, 63.2, -8.0, 8.0, 80.0, 8.0);

		assertEquals(new BlockPos(-54, 63, -8), service.originFor(region));
	}

	@Test
	void placesOnlyOncePerLaneWithinSingleMatch() {
		var placed = new AtomicInteger();
		var service = new LaneStructureService((server, worldId, structureId, originPos) -> {
			placed.incrementAndGet();
			return true;
		});

		assertTrue(service.placeStructure(null, "minecraft:overworld", LaneId.LANE_1, "minecraftsnap:test", BlockPos.ORIGIN));
		assertFalse(service.placeStructure(null, "minecraft:overworld", LaneId.LANE_1, "minecraftsnap:test", BlockPos.ORIGIN));
		assertTrue(service.placeStructure(null, "minecraft:overworld", LaneId.LANE_2, "minecraftsnap:test", BlockPos.ORIGIN));
		assertEquals(2, placed.get());
	}

	@Test
	void forcePlaceIgnoresPerLanePlacementLock() {
		var placed = new AtomicInteger();
		var service = new LaneStructureService((server, worldId, structureId, originPos) -> {
			placed.incrementAndGet();
			return true;
		});

		assertTrue(service.placeStructure(null, "minecraft:overworld", LaneId.LANE_1, "minecraftsnap:test", BlockPos.ORIGIN));
		assertTrue(service.forcePlaceStructure(null, "minecraft:overworld", LaneId.LANE_1, "minecraft:default", BlockPos.ORIGIN));
		assertEquals(2, placed.get());
	}
}
