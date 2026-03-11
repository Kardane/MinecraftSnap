package karn.minecraftsnap.game;

import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaneStructureServiceTest {
	@Test
	void originUsesLaneMinimumPositionAndOffsets() {
		var service = new LaneStructureService((server, worldId, structureId, originPos) -> true);
		var region = SystemConfig.LaneRegionConfig.create(12.8, 63.2, -5.4, 20.0, 80.0, 8.0);
		var biome = new BiomeEntry();
		biome.structureOffsetX = 3;
		biome.structureOffsetY = 4;
		biome.structureOffsetZ = 5;

		assertEquals(new BlockPos(15, 67, -1), service.originFor(region, biome));
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
}
