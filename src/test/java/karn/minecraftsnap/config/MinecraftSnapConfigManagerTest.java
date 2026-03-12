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
		assertTrue(Files.exists(tempDir.resolve("villager_shop.json")));
		assertTrue(Files.exists(tempDir.resolve("nether_shop.json")));
		assertTrue(manager.getBiomeCatalog().biomes.size() >= 3);
		assertEquals("unique_random", manager.getSystemConfig().biomeReveal.assignmentPolicy);
		assertEquals("minecraft:overworld", manager.getSystemConfig().world);
		assertEquals("forest", manager.getBiomeCatalog().biomes.getFirst().effectType);
		assertEquals("", manager.getBiomeCatalog().biomes.getFirst().structureId);
		assertNotNull(manager.getFactionConfig(FactionId.VILLAGER));
		assertEquals("주민&우민", manager.getFactionConfig(FactionId.VILLAGER).displayName);
		assertTrue(manager.getShopConfig(FactionId.VILLAGER).entries.size() >= 1);
		assertTrue(manager.getShopConfig(FactionId.NETHER).entries.size() >= 1);
		assertTrue(manager.getFactionConfig(FactionId.MONSTER).units.stream()
			.filter(unit -> "zombie".equals(unit.id))
			.findFirst()
			.orElseThrow()
			.advanceOptions.size() >= 1);
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
		assertEquals(3.0f, manager.getSystemConfig().inGame.captainFlySpeed);
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
		assertEquals(-11.0, config.gameStart.redUnitSpawn.x);
		assertEquals(-11.0, config.gameStart.blueUnitSpawn.x);
		assertEquals(-4.0, config.capture.lane1.minX);
		assertEquals(4.0, config.capture.lane1.maxX);

		var stored = Files.readString(tempDir.resolve("system.json"));
		assertEquals(1, stored.split("\"world\"").length - 1);
	}

	@Test
	void loadMigratesLegacyAdvanceConditionsIntoMonsterUnitConfig(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("system.json"), """
			{
			  "advance": {
			    "conditions": [
			      {
			        "unitId": "zombie",
			        "biomes": ["minecraft:swamp"],
			        "weathers": ["rain"],
			        "requiredExp": 9,
			        "resultUnitId": "zombie_veteran"
			      }
			    ]
			  }
			}
			""");
		Files.writeString(tempDir.resolve("faction_monster.json"), """
			{
			  "displayName": "몬스터",
			  "units": [
			    {
			      "id": "zombie",
			      "displayName": "좀비",
			      "mainHand": {"itemId": "minecraft:iron_shovel"},
			      "advanceOptions": []
			    },
			    {
			      "id": "zombie_veteran",
			      "displayName": "강화 좀비",
			      "captainSpawnable": false,
			      "mainHand": {"itemId": "minecraft:iron_shovel"}
			    }
			  ]
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		var zombie = manager.getFactionConfig(FactionId.MONSTER).units.stream()
			.filter(unit -> "zombie".equals(unit.id))
			.findFirst()
			.orElseThrow();
		assertEquals(1, zombie.advanceOptions.size());
		assertEquals("zombie_veteran", zombie.advanceOptions.getFirst().resultUnitId);
		assertEquals(9, zombie.advanceOptions.getFirst().requiredTicks);
	}
}
