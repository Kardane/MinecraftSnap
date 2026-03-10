package karn.minecraftsnap.game;

import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BiomeRevealServiceTest {
	@Test
	void assignBiomesUsesUniqueEntries() {
		var manager = new MatchManager();
		var service = new BiomeRevealService(manager, new TextTemplateResolver(), new Random(7));
		var catalog = catalog();

		var assigned = service.assignBiomes(catalog);

		assertEquals(3, assigned.size());
		assertNotEquals(assigned.get(LaneId.LANE_1).id, assigned.get(LaneId.LANE_2).id);
		assertNotEquals(assigned.get(LaneId.LANE_2).id, assigned.get(LaneId.LANE_3).id);
	}

	@Test
	void scheduledRevealOpensLanesAtConfiguredSeconds() {
		var manager = new MatchManager();
		var service = new BiomeRevealService(manager, new TextTemplateResolver(), new Random(1));
		var config = new SystemConfig();
		var laneBiomeService = new LaneBiomeService((server, command) -> true);
		manager.setAssignedBiomeId(LaneId.LANE_1, "forest");
		manager.setAssignedBiomeId(LaneId.LANE_2, "desert");
		manager.setAssignedBiomeId(LaneId.LANE_3, "swamp");

		service.syncRevealState(0, config, catalog(), laneBiomeService, null);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_1));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_2));
		assertEquals(false, manager.isLaneRevealed(LaneId.LANE_3));

		service.syncRevealState(180, config, catalog(), laneBiomeService, null);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_2));

		service.syncRevealState(360, config, catalog(), laneBiomeService, null);
		assertEquals(true, manager.isLaneRevealed(LaneId.LANE_3));
	}

	private BiomeCatalog catalog() {
		var catalog = new BiomeCatalog();
		catalog.biomes = List.of(entry("forest"), entry("desert"), entry("swamp"));
		catalog.normalize();
		return catalog;
	}

	private BiomeEntry entry(String id) {
		var entry = new BiomeEntry();
		entry.id = id;
		entry.displayName = id;
		entry.minecraftBiomeId = "minecraft:" + id;
		entry.normalize();
		return entry;
	}
}
