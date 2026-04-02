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
		assertTrue(Files.exists(tempDir.resolve("villager_shop.json")));
		assertTrue(Files.exists(tempDir.resolve("nether_shop.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("gui.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("bossbar.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("sidebar.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("actionbar.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("title.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("subtitle.json")));
		assertTrue(Files.exists(tempDir.resolve("text").resolve("chat.json")));
		assertTrue(Files.exists(tempDir.resolve("unit").resolve("villager").resolve("villager.json")));
		assertTrue(Files.exists(tempDir.resolve("unit").resolve("monster").resolve("zombie.json")));
		assertEquals(14, manager.getBiomeCatalog().biomes.size());
		assertEquals("unique_random", manager.getSystemConfig().biomeReveal.assignmentPolicy);
		assertEquals("minecraft:overworld", manager.getSystemConfig().world);
		assertEquals(30, manager.getSystemConfig().ladderReward.captainBase);
		assertEquals(1.0f, manager.getSystemConfig().ladderReward.captainScoreGapMultiplier);
		assertEquals(5.0f, manager.getSystemConfig().ladderReward.unitBase);
		assertEquals("plain", manager.getBiomeCatalog().biomes.getFirst().effectType);
		assertEquals("minecraft:plain", manager.getBiomeCatalog().biomes.getFirst().structureId);
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "void".equals(entry.id) && "noop".equals(entry.effectType)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "basalt_deltas".equals(entry.id) && "noop".equals(entry.effectType)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "lush_cave".equals(entry.id) && "lush_cave".equals(entry.effectType)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "mushroom_island".equals(entry.id) && "mushroom_island".equals(entry.effectType)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "cold_ocean".equals(entry.id) && "cold_ocean".equals(entry.effectType)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "reverse_icicle".equals(entry.id) && "reverse_icicle".equals(entry.effectType)));
		assertTrue(manager.getShopConfig(karn.minecraftsnap.game.FactionId.VILLAGER).entries.stream().allMatch(entry -> "enchant".equals(entry.type)));
		assertTrue(manager.getShopConfig(karn.minecraftsnap.game.FactionId.NETHER).entries.stream().allMatch(entry -> "item".equals(entry.type)));

		var storedSystem = Files.readString(tempDir.resolve("system.json"));
		var storedGui = Files.readString(tempDir.resolve("text").resolve("gui.json"));
		var storedBossbar = Files.readString(tempDir.resolve("text").resolve("bossbar.json"));
		var storedSidebar = Files.readString(tempDir.resolve("text").resolve("sidebar.json"));
		var storedActionbar = Files.readString(tempDir.resolve("text").resolve("actionbar.json"));
		var storedSubtitle = Files.readString(tempDir.resolve("text").resolve("subtitle.json"));
		var storedChat = Files.readString(tempDir.resolve("text").resolve("chat.json"));
		var storedVillagerUnit = Files.readString(tempDir.resolve("unit").resolve("villager").resolve("villager.json"));
		assertFalse(storedSystem.contains("captainSpawnGuiTitle"));
		assertFalse(storedSystem.contains("countdownTitle"));
		assertFalse(storedSystem.contains("victoryCountdownSubtitleTemplate"));
		assertTrue(storedGui.contains("captainMenuItemName"));
		assertFalse(storedGui.contains("bossBarTemplate"));
		assertFalse(storedGui.contains("lobbyScoreboardTitle"));
		assertTrue(storedBossbar.contains("bossBarTemplate"));
		assertTrue(storedBossbar.contains("captureProgressBossBarTemplate"));
		assertTrue(storedSidebar.contains("lobbyScoreboardTitle"));
		assertTrue(storedSidebar.contains("runningSidebarLaneTemplate"));
		assertTrue(storedActionbar.contains("captainHudTemplate"));
		assertTrue(storedSubtitle.contains("victoryCountdownSubtitleTemplate"));
		assertTrue(storedChat.contains("tradePurchaseSuccessMessage"));
		assertTrue(storedChat.contains("commandClearTeamsMessage"));
		assertFalse(storedChat.contains("unitDescriptions"));
		assertTrue(storedVillagerUnit.contains("\"id\": \"villager\""));
		assertTrue(storedVillagerUnit.contains("\"displayName\": \"멍청이 주민\""));
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
		assertEquals(12.0f, manager.getSystemConfig().ladderReward.unitCaptureScoreDivisor);
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
			      "label": "lane1",
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

	@Test
	void loadAppendsMissingDefaultBiomesToLegacyCatalog(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("biomes.json"), """
			{
			  "biomes": [
			    { "id": "plain", "displayName": "a", "minecraftBiomeId": "minecraft:plains" },
			    { "id": "desert", "displayName": "b", "minecraftBiomeId": "minecraft:desert" },
			    { "id": "swamp", "displayName": "c", "minecraftBiomeId": "minecraft:swamp" },
			    { "id": "badlands", "displayName": "d", "minecraftBiomeId": "minecraft:badlands" }
			  ]
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals(14, manager.getBiomeCatalog().biomes.size());
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "end".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "taiga".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "basalt_deltas".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "lush_cave".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "mushroom_island".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "cold_ocean".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "reverse_icicle".equals(entry.id)));
		assertTrue(manager.getBiomeCatalog().biomes.stream().anyMatch(entry -> "void".equals(entry.id)));
		assertEquals("plain", manager.getBiomeCatalog().biomes.getFirst().effectType);
	}

	@Test
	void loadMigratesLegacyVillagerShopToEnchantShop(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("villager_shop.json"), """
			{
			  "entries": [
			    {
			      "id": "bread",
			      "price": 3,
			      "item": {
			        "itemId": "minecraft:bread",
			        "count": 1
			      }
			    }
			  ]
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		var villagerShop = manager.getShopConfig(karn.minecraftsnap.game.FactionId.VILLAGER);
		assertEquals(2, villagerShop.entries.size());
		assertTrue(villagerShop.entries.stream().allMatch(entry -> "enchant".equals(entry.type)));
		assertTrue(villagerShop.entries.stream().anyMatch(entry -> "sharpness_upgrade".equals(entry.id)));
		assertTrue(Files.readString(tempDir.resolve("villager_shop.json")).contains("\"type\": \"enchant\""));
	}

	@Test
	void textsJsonOverridesVictoryCountdownSubtitleTemplate(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("text"));
		Files.writeString(tempDir.resolve("text").resolve("subtitle.json"), """
			{
			  "victoryCountdownSubtitleTemplate": "&6{team} 승리까지 {seconds}초"
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals("&6{team} 승리까지 {seconds}초", manager.getSystemConfig().gameEnd.victoryCountdownSubtitleTemplate);
	}

	@Test
	void loadMigratesLegacyBossBarTemplateToLatestFormat(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("text"));
		Files.writeString(tempDir.resolve("text").resolve("gui.json"), """
			{
			  "bossBarTemplate": "&c레드 {red_score} &8| &f남은 시간 {time} &8| &9블루 {blue_score}"
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals(
			"&c레드 &f{red_score} &8| &f⌛{time} &8| &e다음 라인 공개: &f{next_reveal_time} &8| &9블루 &f{blue_score}",
			manager.getTextConfig().bossBarTemplate
		);
		assertEquals(manager.getTextConfig().bossBarTemplate, manager.getSystemConfig().bossBar.template);
		assertTrue(Files.readString(tempDir.resolve("text").resolve("bossbar.json")).contains("\"bossBarTemplate\""));
		assertFalse(Files.readString(tempDir.resolve("text").resolve("gui.json")).contains("\"bossBarTemplate\""));
	}

	@Test
	void textsJsonOverridesCaptainHudTemplate(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("text"));
		Files.writeString(tempDir.resolve("text").resolve("actionbar.json"), """
			{
			  "captainHudTemplate": "&f{current_mana}/{max_mana} | {lane} | {player} | {skill_cooldown}"
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals("&f{current_mana}/{max_mana} | {lane} | {player} | {skill_cooldown}", manager.getTextConfig().captainHudTemplate);
		assertEquals(manager.getTextConfig().captainHudTemplate, manager.getSystemConfig().display.captainHudTemplate);
	}

	@Test
	void loadMigratesLegacyCaptainHudTemplateToLatestFormat(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("text"));
		Files.writeString(tempDir.resolve("text").resolve("actionbar.json"), """
			{
			  "captainHudTemplate": "&d다음 소환 &f{player} &8| &e라인 &f{lane}"
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals(
			"&b{current_mana}&7/&f{max_mana} &8({mana_cooldown}초) &8| &e{lane} &8| &d{player} &8| &c{skill_cooldown}초",
			manager.getTextConfig().captainHudTemplate
		);
		assertEquals(manager.getTextConfig().captainHudTemplate, manager.getSystemConfig().display.captainHudTemplate);
	}

	@Test
	void untouchedCaptainSpawnDefaultsMigrateToLaneOneDefaults(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("system.json"), """
			{
			  "gameStart": {
			    "redCaptainSpawn": { "x": -10.0, "y": 64.0, "z": 10.0 },
			    "blueCaptainSpawn": { "x": 10.0, "y": 64.0, "z": 10.0 }
			  }
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals(-10.0, manager.getSystemConfig().gameStart.redCaptainSpawn.x);
		assertEquals(-10.0, manager.getSystemConfig().gameStart.redCaptainSpawn.z);
		assertEquals(10.0, manager.getSystemConfig().gameStart.blueCaptainSpawn.x);
		assertEquals(-10.0, manager.getSystemConfig().gameStart.blueCaptainSpawn.z);
	}

	@Test
	void customCaptainSpawnIsPreserved(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("system.json"), """
			{
			  "gameStart": {
			    "redCaptainSpawn": { "x": -30.0, "y": 80.0, "z": 5.0 },
			    "blueCaptainSpawn": { "x": 30.0, "y": 80.0, "z": 5.0 }
			  }
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals(-30.0, manager.getSystemConfig().gameStart.redCaptainSpawn.x);
		assertEquals(5.0, manager.getSystemConfig().gameStart.redCaptainSpawn.z);
		assertEquals(30.0, manager.getSystemConfig().gameStart.blueCaptainSpawn.x);
		assertEquals(5.0, manager.getSystemConfig().gameStart.blueCaptainSpawn.z);
	}

	@Test
	void loadReadsLegacyTextsJsonAndWritesSplitTextFiles(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir);
		Files.writeString(tempDir.resolve("texts.json"), """
			{
			  "mainLobbyTitle": "&0로비",
			  "bossBarTemplate": "&fBOSS",
			  "lobbyScoreboardTitle": "&eSIDE",
			  "captainHudTemplate": "&fHUD",
			  "victoryCountdownSubtitleTemplate": "&eSUB",
			  "commandClearTeamsMessage": "&a채팅"
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals("&0로비", manager.getTextConfig().mainLobbyTitle);
		assertEquals("&fBOSS", manager.getTextConfig().bossBarTemplate);
		assertEquals("&eSIDE", manager.getTextConfig().lobbyScoreboardTitle);
		assertEquals("&fHUD", manager.getTextConfig().captainHudTemplate);
		assertEquals("&eSUB", manager.getTextConfig().victoryCountdownSubtitleTemplate);
		assertEquals("&a채팅", manager.getTextConfig().commandClearTeamsMessage);
		assertTrue(Files.readString(tempDir.resolve("text").resolve("gui.json")).contains("\"mainLobbyTitle\": \"&0로비\""));
		assertTrue(Files.readString(tempDir.resolve("text").resolve("bossbar.json")).contains("\"bossBarTemplate\": \"&fBOSS\""));
		assertTrue(Files.readString(tempDir.resolve("text").resolve("sidebar.json")).contains("\"lobbyScoreboardTitle\": \"&eSIDE\""));
		assertTrue(Files.readString(tempDir.resolve("text").resolve("actionbar.json")).contains("\"captainHudTemplate\": \"&fHUD\""));
		assertTrue(Files.readString(tempDir.resolve("text").resolve("subtitle.json")).contains("\"victoryCountdownSubtitleTemplate\": \"&eSUB\""));
		assertTrue(Files.readString(tempDir.resolve("text").resolve("chat.json")).contains("\"commandClearTeamsMessage\": \"&a채팅\""));
	}
	@Test
	void loadMigratesLegacyFlatUnitConfigToFactionDirectory(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("unit"));
		Files.writeString(tempDir.resolve("unit").resolve("villager.json"), """
			{
			  "displayName": "移댁뒪? ?쇰줈?ㅺ렇",
			  "cost": 9
			}
			""");

		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();

		assertEquals("移댁뒪? ?쇰줈?ㅺ렇", manager.getUnitConfigs().get("villager").displayName);
		assertEquals(9, manager.getUnitConfigs().get("villager").cost);
		assertTrue(Files.exists(tempDir.resolve("unit").resolve("villager").resolve("villager.json")));
		assertTrue(Files.readString(tempDir.resolve("unit").resolve("villager").resolve("villager.json")).contains("\"cost\": 9"));
	}
}
