package karn.minecraftsnap.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftSnapConfigManagerTest {
	@Test
	void loadCreatesAndParsesAllRuntimeConfigFiles(@TempDir Path tempDir) throws Exception {
		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));

		manager.load();

		assertTrue(Files.exists(tempDir.resolve("system.json")));
		assertTrue(Files.exists(tempDir.resolve("stats.json")));
		assertTrue(Files.exists(tempDir.resolve("biomes.json")));
		assertTrue(Files.exists(tempDir.resolve("texts.json")));
		assertTrue(Files.notExists(tempDir.resolve("faction_villager.json")));
		assertTrue(Files.notExists(tempDir.resolve("faction_monster.json")));
		assertTrue(Files.notExists(tempDir.resolve("faction_nether.json")));
		assertTrue(Files.exists(tempDir.resolve("villager_shop.json")));
		assertTrue(Files.exists(tempDir.resolve("nether_shop.json")));
		assertTrue(manager.getBiomeCatalog().biomes.size() >= 3);
		assertEquals("unique_random", manager.getSystemConfig().biomeReveal.assignmentPolicy);
		assertEquals("minecraft:overworld", manager.getSystemConfig().world);
		assertEquals("forest", manager.getBiomeCatalog().biomes.getFirst().effectType);
		assertEquals("minecraftsnap:forest_lane", manager.getBiomeCatalog().biomes.getFirst().structureId);
		assertTrue(manager.getShopConfig(karn.minecraftsnap.game.FactionId.VILLAGER).entries.size() >= 1);
		assertTrue(manager.getShopConfig(karn.minecraftsnap.game.FactionId.NETHER).entries.size() >= 1);
		var storedSystem = Files.readString(tempDir.resolve("system.json"));
		var storedTexts = Files.readString(tempDir.resolve("texts.json"));
		assertFalse(storedSystem.contains("captainSpawnGuiTitle"));
		assertFalse(storedSystem.contains("countdownTitle"));
		assertFalse(storedSystem.contains("victoryCountdownSubtitleTemplate"));
		assertTrue(storedTexts.contains("tradePurchaseSuccessMessage"));
		assertTrue(storedTexts.contains("captainMenuItemName"));
		assertTrue(storedTexts.contains("lobbyScoreboardTitle"));
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
		assertEquals(0.0, manager.getSystemConfig().inGame.captainMinY);
		assertEquals(1.0f, manager.getSystemConfig().inGame.captainFlySpeed);
		assertEquals("[{ladder}] ", manager.getSystemConfig().display.ladderPrefixFormat);
		assertEquals("&6팩션 선택 남은 시간 &f{time}", manager.getSystemConfig().lobby.factionSelectBossBarTemplate);
	}

	@Test
	void loadMigratesLegacySpawnAndCaptureConfig(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("system.json"), """
			{
			  "lobby": {
			    "world": "minecraft:overworld",
			    "spawnX": 1.0,
			    "spawnY": 65.0,
			    "spawnZ": 2.0
			  },
			  "gameStart": {
			    "captainSpawn": {
			      "world": "minecraft:overworld",
			      "x": 11.0,
			      "y": 70.0,
			      "z": 12.0,
			      "yaw": 90.0,
			      "pitch": 5.0
			    },
			    "unitSpawn": {
			      "world": "minecraft:overworld",
			      "x": -11.0,
			      "y": 63.0,
			      "z": -12.0
			    }
			  },
			  "capture": {
			    "lane1": {
			      "enabled": true,
			      "label": "1번 라인",
			      "world": "minecraft:overworld",
			      "x": 0.0,
			      "y": 64.0,
			      "z": 0.0,
			      "radius": 4.0
			    }
			  }
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		var config = manager.getSystemConfig();
		assertEquals("minecraft:overworld", config.world);
		assertEquals(11.0, config.gameStart.redCaptainSpawn.x);
		assertEquals(11.0, config.gameStart.blueCaptainSpawn.x);
		assertEquals(-11.0, config.gameStart.redLane1UnitSpawn.x);
		assertEquals(-11.0, config.gameStart.redLane2UnitSpawn.x);
		assertEquals(-11.0, config.gameStart.redLane3UnitSpawn.x);
		assertEquals(-11.0, config.gameStart.blueLane1UnitSpawn.x);
		assertEquals(-11.0, config.gameStart.blueLane2UnitSpawn.x);
		assertEquals(-11.0, config.gameStart.blueLane3UnitSpawn.x);
		assertEquals(-4.0, config.capture.lane1.minX);
		assertEquals(4.0, config.capture.lane1.maxX);

		var stored = Files.readString(tempDir.resolve("system.json"));
		assertEquals(1, stored.split("\"world\"").length - 1);
	}

}
