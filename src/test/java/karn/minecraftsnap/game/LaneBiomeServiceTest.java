package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaneBiomeServiceTest {
	@Test
	void collectChunkPositionsReturnsUniqueChunkSet() {
		var region = new karn.minecraftsnap.config.SystemConfig.LaneRegionConfig();
		region.minX = 0.0;
		region.maxX = 31.0;
		region.minZ = 0.0;
		region.maxZ = 31.0;

		Set<Long> chunks = LaneBiomeService.collectChunkPositions(region);

		assertEquals(4, chunks.size());
	}

	@Test
	void targetRegionFollowsCaptureRegionLocation() {
		var config = new karn.minecraftsnap.config.SystemConfig();
		config.capture.lane1.enabled = true;
		config.capture.lane1.minX = 16.0;
		config.capture.lane1.maxX = 20.0;
		config.capture.lane1.minY = 10.0;
		config.capture.lane1.maxY = 20.0;
		config.capture.lane1.minZ = -2.0;
		config.capture.lane1.maxZ = 2.0;

		assertEquals(config.inGame.lane2Region, LaneBiomeService.targetRegionOf(LaneId.LANE_1, config));
	}

	@Test
	void restoreAllReappliesStoredSnapshots() {
		var applied = new ArrayList<List<LaneBiomeService.BiomeCellSnapshot>>();
		var service = new LaneBiomeService(new LaneBiomeService.BiomeApplier() {
			@Override
			public List<LaneBiomeService.BiomeCellSnapshot> snapshot(net.minecraft.server.MinecraftServer server, String worldId, karn.minecraftsnap.config.SystemConfig.LaneRegionConfig region) {
				return List.of();
			}

			@Override
			public void apply(net.minecraft.server.MinecraftServer server, List<LaneBiomeService.BiomeCellSnapshot> snapshots) {
				applied.add(List.copyOf(snapshots));
			}
		});
		service.rememberSnapshot(LaneId.LANE_1, List.of(
			LaneBiomeService.BiomeCellSnapshot.at("minecraft:overworld", 0, 64, 0, "minecraft:plains"),
			LaneBiomeService.BiomeCellSnapshot.at("minecraft:overworld", 4, 64, 0, "minecraft:forest")
		));

		service.restoreAll(null);

		assertEquals(1, applied.size());
		assertEquals(2, applied.get(0).size());
		assertEquals("minecraft:plains", applied.get(0).get(0).biomeId());
		assertTrue(service.getSnapshot(LaneId.LANE_1).isEmpty());
	}
}
