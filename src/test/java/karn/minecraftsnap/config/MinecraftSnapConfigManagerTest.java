package karn.minecraftsnap.config;

import karn.minecraftsnap.game.FactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftSnapConfigManagerTest {
	@Test
	void loadCreatesAndParsesAllRuntimeConfigFiles(@TempDir Path tempDir) {
		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));

		manager.load();

		assertTrue(Files.exists(tempDir.resolve("system.json")));
		assertTrue(Files.exists(tempDir.resolve("stats.json")));
		assertTrue(Files.exists(tempDir.resolve("biomes.json")));
		assertTrue(Files.exists(tempDir.resolve("faction_villager.json")));
		assertTrue(Files.exists(tempDir.resolve("faction_monster.json")));
		assertTrue(Files.exists(tempDir.resolve("faction_nether.json")));
		assertTrue(manager.getBiomeCatalog().biomes.size() >= 3);
		assertEquals("unique_random", manager.getSystemConfig().biomeReveal.assignmentPolicy);
		assertNotNull(manager.getFactionConfig(FactionId.VILLAGER));
		assertEquals("주민&우민", manager.getFactionConfig(FactionId.VILLAGER).displayName);
	}

	@Test
	void loadNormalizesSparseSystemConfig(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("system.json"), "{}");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertNotNull(manager.getSystemConfig().gameEnd);
		assertEquals(10, manager.getSystemConfig().gameEnd.finalTickRate);
		assertEquals(true, manager.getSystemConfig().biomeReveal.applyHiddenVoidBiome);
	}
}
