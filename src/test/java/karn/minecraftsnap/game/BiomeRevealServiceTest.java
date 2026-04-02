package karn.minecraftsnap.game;

import karn.minecraftsnap.biome.BiomeEffectRegistry;
import karn.minecraftsnap.biome.NoOpBiomeEffect;
import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		var laneBiomeService = new LaneBiomeService(new LaneBiomeService.BiomeApplier() {
			@Override
			public List<LaneBiomeService.BiomeCellSnapshot> snapshot(net.minecraft.server.MinecraftServer server, String worldId, SystemConfig.LaneRegionConfig region) {
				return List.of();
			}

			@Override
			public void apply(net.minecraft.server.MinecraftServer server, List<LaneBiomeService.BiomeCellSnapshot> snapshots) {
			}
		});
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

	@Test
	void nextRevealRemainingSecondsTracksNextHiddenLane() {
		var manager = new MatchManager();
		var service = new BiomeRevealService(manager, new TextTemplateResolver(), new Random(1));
		var config = new SystemConfig();
		manager.startGameRunning();
		for (int i = 0; i < 120; i++) {
			manager.tick();
		}

		assertEquals(174, service.nextRevealRemainingSeconds(config));
		assertEquals("02:54", service.nextRevealRemainingTime(config));

		manager.revealLane(LaneId.LANE_2);

		assertEquals(354, service.nextRevealRemainingSeconds(config));
	}

	@Test
	void prepareForMatchSetsRuntimeEffectAndPlacesStructureForImmediateReveal() {
		var manager = new MatchManager();
		var registry = new LaneRuntimeRegistry();
		var config = new SystemConfig();
		registry.refresh(null, config, manager, null);
		var structurePlacements = new AtomicInteger();
		var revealCalls = new AtomicInteger();
		var effectRegistry = new BiomeEffectRegistry();
		effectRegistry.register("tracking", () -> new NoOpBiomeEffect() {
			@Override
			public void onReveal(karn.minecraftsnap.biome.BiomeRuntimeContext context) {
				revealCalls.incrementAndGet();
			}
		});
		var service = new BiomeRevealService(
			manager,
			new TextTemplateResolver(),
			new Random(3),
			registry,
			new LaneStructureService((server, worldId, structureId, originPos) -> {
				structurePlacements.incrementAndGet();
				return true;
			}),
			effectRegistry
		);
		var applied = new ArrayList<List<LaneBiomeService.BiomeCellSnapshot>>();
		var laneBiomeService = new LaneBiomeService(new LaneBiomeService.BiomeApplier() {
			@Override
			public List<LaneBiomeService.BiomeCellSnapshot> snapshot(net.minecraft.server.MinecraftServer server, String worldId, SystemConfig.LaneRegionConfig region) {
				return List.of();
			}

			@Override
			public void apply(net.minecraft.server.MinecraftServer server, List<LaneBiomeService.BiomeCellSnapshot> snapshots) {
				applied.add(List.copyOf(snapshots));
			}
		});
		var catalog = new BiomeCatalog();
		var biome = entry("forest");
		biome.effectType = "tracking";
		biome.structureId = "minecraftsnap:test_structure";
		catalog.biomes = List.of(biome);
		catalog.normalize();

		service.prepareForMatch(null, config, catalog, laneBiomeService);

		assertEquals("forest", manager.getAssignedBiomeId(LaneId.LANE_1));
		assertTrue(registry.get(LaneId.LANE_1).hasActiveBiome());
		assertEquals("forest", registry.get(LaneId.LANE_1).biomeEntry().id);
		assertEquals(0, revealCalls.get());
		assertEquals(1, structurePlacements.get());
		assertTrue(applied.stream().flatMap(List::stream).anyMatch(snapshot -> "minecraft:forest".equals(snapshot.biomeId())));

		manager.startGameRunning();
		service.syncRevealState(0, config, catalog, laneBiomeService, null);

		assertEquals(1, revealCalls.get());
	}

	@Test
	void immediateEndRevealEffectAppliesWhenGameStartsRunning() {
		var manager = new MatchManager();
		manager.applyGameDuration(300);
		manager.enterGameStart();
		var registry = new LaneRuntimeRegistry();
		var config = new SystemConfig();
		registry.refresh(null, config, manager, null);
		var service = new BiomeRevealService(
			manager,
			new TextTemplateResolver(),
			new Random(0),
			registry,
			new LaneStructureService((server, worldId, structureId, originPos) -> true),
			new BiomeEffectRegistry()
		);
		var laneBiomeService = new LaneBiomeService(new LaneBiomeService.BiomeApplier() {
			@Override
			public List<LaneBiomeService.BiomeCellSnapshot> snapshot(net.minecraft.server.MinecraftServer server, String worldId, SystemConfig.LaneRegionConfig region) {
				return List.of();
			}

			@Override
			public void apply(net.minecraft.server.MinecraftServer server, List<LaneBiomeService.BiomeCellSnapshot> snapshots) {
			}
		});
		var catalog = new BiomeCatalog();
		catalog.biomes = List.of(entry("end"));
		catalog.normalize();

		service.prepareForMatch(null, config, catalog, laneBiomeService);

		assertEquals(300, manager.getRemainingSeconds());

		manager.startGameRunning();
		service.syncRevealState(0, config, catalog, laneBiomeService, null);
		assertEquals(240, manager.getRemainingSeconds());

		service.syncRevealState(0, config, catalog, laneBiomeService, null);
		assertEquals(240, manager.getRemainingSeconds());
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
