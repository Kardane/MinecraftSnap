package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import karn.minecraftsnap.game.FactionId;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MinecraftSnapConfigManager {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private final Path configDirectory;
	private final Logger logger;
	private SystemConfig systemConfig = new SystemConfig();
	private TextConfigFile textConfig = new TextConfigFile();
	private BiomeCatalog biomeCatalog = new BiomeCatalog();
	private final EnumMap<FactionId, FactionConfigFile> factionConfigs = new EnumMap<>(FactionId.class);
	private final EnumMap<FactionId, ShopConfigFile> shopConfigs = new EnumMap<>(FactionId.class);
	private StatsRepository statsRepository;

	public MinecraftSnapConfigManager(Path configDirectory, Logger logger) {
		this.configDirectory = configDirectory;
		this.logger = logger;
	}

	public void load() {
		try {
			Files.createDirectories(configDirectory);
			systemConfig = loadSystemConfig(configDirectory.resolve("system.json"));
			textConfig = loadOrCreate(configDirectory.resolve("texts.json"), TextConfigFile.class, new TextConfigFile());
			textConfig.normalize();
			writeJson(configDirectory.resolve("texts.json"), textConfig);
			textConfig.applyTo(systemConfig);
			biomeCatalog = loadOrCreate(configDirectory.resolve("biomes.json"), BiomeCatalog.class, createDefaultBiomeCatalog());
			biomeCatalog.normalize();
			factionConfigs.clear();
			shopConfigs.clear();
			shopConfigs.put(FactionId.VILLAGER, loadOrCreate(configDirectory.resolve("villager_shop.json"), ShopConfigFile.class, createDefaultShopConfig(FactionId.VILLAGER)));
			shopConfigs.put(FactionId.NETHER, loadOrCreate(configDirectory.resolve("nether_shop.json"), ShopConfigFile.class, createDefaultShopConfig(FactionId.NETHER)));
			shopConfigs.values().forEach(ShopConfigFile::normalize);

			statsRepository = new StatsRepository(configDirectory.resolve("stats.json"), logger);
			statsRepository.load();
		} catch (IOException exception) {
			logger.error("MCsnap 컨픽 디렉터리 초기화 실패", exception);
			systemConfig = new SystemConfig();
			textConfig = new TextConfigFile();
			textConfig.normalize();
			textConfig.applyTo(systemConfig);
			biomeCatalog = createDefaultBiomeCatalog();
			factionConfigs.clear();
			shopConfigs.clear();
			shopConfigs.put(FactionId.VILLAGER, createDefaultShopConfig(FactionId.VILLAGER));
			shopConfigs.put(FactionId.NETHER, createDefaultShopConfig(FactionId.NETHER));
			statsRepository = new StatsRepository(configDirectory.resolve("stats.json"), logger);
			statsRepository.load();
		}
	}

	public void reload() {
		load();
	}

	public SystemConfig getSystemConfig() {
		return systemConfig;
	}

	public StatsRepository getStatsRepository() {
		return statsRepository;
	}

	public TextConfigFile getTextConfig() {
		return textConfig;
	}

	public BiomeCatalog getBiomeCatalog() {
		return biomeCatalog;
	}

	public ShopConfigFile getShopConfig(FactionId factionId) {
		var config = shopConfigs.get(factionId);
		if (config != null) {
			return config;
		}
		var empty = new ShopConfigFile();
		empty.normalize();
		return empty;
	}

	private SystemConfig loadSystemConfig(Path path) throws IOException {
		if (Files.notExists(path)) {
			var defaults = new SystemConfig();
			defaults.normalize();
			writeJson(path, defaults);
			return defaults;
		}

		JsonObject rawJson;
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			rawJson = JsonParser.parseReader(reader).getAsJsonObject();
		}

		var loaded = gson.fromJson(rawJson, SystemConfig.class);
		if (loaded == null) {
			loaded = new SystemConfig();
		}
		migrateLegacySystemConfig(rawJson, loaded);
		loaded.normalize();
		validateCaptureRegions(loaded);
		writeJson(path, loaded);
		return loaded;
	}

	private <T> T loadOrCreate(Path path, Class<T> type, T defaultValue) throws IOException {
		if (Files.notExists(path)) {
			writeJson(path, defaultValue);
			return defaultValue;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			var loaded = gson.fromJson(reader, type);
			return loaded != null ? loaded : defaultValue;
		}
	}

	private FactionConfigFile loadFactionConfig(Path path, FactionId factionId) throws IOException {
		var defaultValue = createDefaultFactionConfig(factionId);
		if (Files.notExists(path)) {
			writeJson(path, defaultValue);
			return defaultValue;
		}

		JsonObject rawJson;
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			rawJson = JsonParser.parseReader(reader).getAsJsonObject();
		}

		var loaded = gson.fromJson(rawJson, FactionConfigFile.class);
		if (loaded == null) {
			loaded = new FactionConfigFile();
		}
		var migrated = migrateLegacyFactionConfig(rawJson, loaded);
		loaded.normalize();
		if (migrated) {
			writeJson(path, loaded);
		}
		return loaded;
	}

	private void writeJson(Path path, Object value) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			gson.toJson(value, writer);
		}
	}

	private void migrateLegacySystemConfig(JsonObject rawJson, SystemConfig loaded) {
		loaded.world = firstNonBlank(
			legacyString(rawJson, "world"),
			legacyString(rawJson, "lobby", "world"),
			legacyString(rawJson, "gameStart", "captainSpawn", "world"),
			legacyString(rawJson, "gameStart", "unitSpawn", "world"),
			legacyString(rawJson, "inGame", "lane1Region", "world"),
			legacyString(rawJson, "capture", "lane1", "world"),
			loaded.world
		);

		var legacyCaptainSpawn = legacyPosition(rawJson, "gameStart", "captainSpawn");
		if (legacyCaptainSpawn != null) {
			if (loaded.gameStart == null) {
				loaded.gameStart = new SystemConfig.GameStartConfig();
			}
			if (nestedObject(rawJson, "gameStart", "redCaptainSpawn") == null) {
				loaded.gameStart.redCaptainSpawn = copyPosition(legacyCaptainSpawn);
			}
			if (nestedObject(rawJson, "gameStart", "blueCaptainSpawn") == null) {
				loaded.gameStart.blueCaptainSpawn = copyPosition(legacyCaptainSpawn);
			}
		}

		migrateLegacyLaneUnitSpawns(rawJson, loaded, legacyPosition(rawJson, "gameStart", "unitSpawn"));

		if (loaded.capture == null) {
			loaded.capture = new SystemConfig.CaptureConfig();
		}
		migrateLegacyCaptureRegion(rawJson, loaded.capture, "lane1");
		migrateLegacyCaptureRegion(rawJson, loaded.capture, "lane2");
		migrateLegacyCaptureRegion(rawJson, loaded.capture, "lane3");
	}

	private void migrateLegacyLaneUnitSpawns(JsonObject rawJson, SystemConfig loaded, SystemConfig.PositionConfig legacyUnitSpawn) {
		if (loaded.gameStart == null) {
			loaded.gameStart = new SystemConfig.GameStartConfig();
		}
		var redLegacy = legacyPosition(rawJson, "gameStart", "redUnitSpawn");
		var blueLegacy = legacyPosition(rawJson, "gameStart", "blueUnitSpawn");
		if (redLegacy == null) {
			redLegacy = legacyUnitSpawn != null ? copyPosition(legacyUnitSpawn) : copyPosition(loaded.gameStart.redCaptainSpawn);
		}
		if (blueLegacy == null) {
			blueLegacy = legacyUnitSpawn != null ? copyPosition(legacyUnitSpawn) : copyPosition(loaded.gameStart.blueCaptainSpawn);
		}
		if (nestedObject(rawJson, "gameStart", "redLane1UnitSpawn") == null) {
			loaded.gameStart.redLane1UnitSpawn = copyPosition(redLegacy);
		}
		if (nestedObject(rawJson, "gameStart", "redLane2UnitSpawn") == null) {
			loaded.gameStart.redLane2UnitSpawn = copyPosition(redLegacy);
		}
		if (nestedObject(rawJson, "gameStart", "redLane3UnitSpawn") == null) {
			loaded.gameStart.redLane3UnitSpawn = copyPosition(redLegacy);
		}
		if (nestedObject(rawJson, "gameStart", "blueLane1UnitSpawn") == null) {
			loaded.gameStart.blueLane1UnitSpawn = copyPosition(blueLegacy);
		}
		if (nestedObject(rawJson, "gameStart", "blueLane2UnitSpawn") == null) {
			loaded.gameStart.blueLane2UnitSpawn = copyPosition(blueLegacy);
		}
		if (nestedObject(rawJson, "gameStart", "blueLane3UnitSpawn") == null) {
			loaded.gameStart.blueLane3UnitSpawn = copyPosition(blueLegacy);
		}
	}

	private void migrateLegacyCaptureRegion(JsonObject rawJson, SystemConfig.CaptureConfig captureConfig, String laneKey) {
		var laneJson = nestedObject(rawJson, "capture", laneKey);
		if (laneJson == null || !laneJson.has("x") || !laneJson.has("y") || !laneJson.has("z")) {
			return;
		}
		if (laneJson.has("minX") && laneJson.has("minY") && laneJson.has("minZ") && laneJson.has("maxX") && laneJson.has("maxY") && laneJson.has("maxZ")) {
			return;
		}

		double x = laneJson.get("x").getAsDouble();
		double y = laneJson.get("y").getAsDouble();
		double z = laneJson.get("z").getAsDouble();
		double radius = laneJson.has("radius") ? laneJson.get("radius").getAsDouble() : 4.0;
		var migrated = SystemConfig.CaptureRegionConfig.create(
			laneJson.has("label") ? laneJson.get("label").getAsString() : laneKey,
			x - radius,
			y - radius,
			z - radius,
			x + radius,
			y + radius,
			z + radius
		);
		migrated.enabled = laneJson.has("enabled") && laneJson.get("enabled").getAsBoolean();

		switch (laneKey) {
			case "lane1" -> captureConfig.lane1 = migrated;
			case "lane2" -> captureConfig.lane2 = migrated;
			case "lane3" -> captureConfig.lane3 = migrated;
		}
	}

	private void validateCaptureRegions(SystemConfig config) {
		validateCaptureRegion("lane1", config.capture.lane1, config.inGame.lane1Region);
		validateCaptureRegion("lane2", config.capture.lane2, config.inGame.lane2Region);
		validateCaptureRegion("lane3", config.capture.lane3, config.inGame.lane3Region);
	}

	private void validateCaptureRegion(String laneKey, SystemConfig.CaptureRegionConfig captureRegion, SystemConfig.LaneRegionConfig laneRegion) {
		if (captureRegion == null || laneRegion == null) {
			return;
		}
		if (captureRegion.minX >= laneRegion.minX
			&& captureRegion.maxX <= laneRegion.maxX
			&& captureRegion.minY >= laneRegion.minY
			&& captureRegion.maxY <= laneRegion.maxY
			&& captureRegion.minZ >= laneRegion.minZ
			&& captureRegion.maxZ <= laneRegion.maxZ) {
			return;
		}
		logger.warn("MCsnap 점령 구역 {} 설정이 라인 구역을 벗어나 비활성화됨", laneKey);
		captureRegion.enabled = false;
	}

	private boolean migrateLegacyFactionConfig(JsonObject rawJson, FactionConfigFile loaded) {
		if (rawJson == null || loaded == null || !rawJson.has("units") || !rawJson.get("units").isJsonArray()) {
			return false;
		}
		boolean migrated = false;
		var rawUnits = rawJson.getAsJsonArray("units");
		for (var unit : loaded.units) {
			var rawUnit = findUnitJson(rawUnits, unit.id);
			if (rawUnit == null) {
				continue;
			}
			migrated |= migrateLegacyUnitEntry(rawUnit, unit);
		}
		return migrated;
	}

	private JsonObject findUnitJson(JsonArray rawUnits, String unitId) {
		for (var element : rawUnits) {
			if (!element.isJsonObject()) {
				continue;
			}
			var unit = element.getAsJsonObject();
			if (unit.has("id") && unitId.equals(unit.get("id").getAsString())) {
				return unit;
			}
		}
		return null;
	}

	private boolean migrateLegacyUnitEntry(JsonObject rawUnit, FactionUnitEntry unit) {
		boolean migrated = false;
		migrated |= migrateLegacyItem(rawUnit, "mainHandItemId", unit.mainHand);
		migrated |= migrateLegacyItem(rawUnit, "offHandItemId", unit.offHand);
		migrated |= migrateLegacyItem(rawUnit, "helmetItemId", unit.helmet);
		migrated |= migrateLegacyItem(rawUnit, "chestItemId", unit.chest);
		migrated |= migrateLegacyItem(rawUnit, "legsItemId", unit.legs);
		migrated |= migrateLegacyItem(rawUnit, "bootsItemId", unit.boots);
		migrated |= migrateLegacyItem(rawUnit, "abilityItemId", unit.abilityItem);
		if (rawUnit.has("disguiseId") && !rawUnit.get("disguiseId").isJsonNull()) {
			if (unit.disguise == null) {
				unit.disguise = new EntitySpecEntry();
			}
			if (unit.disguise.entityId == null || unit.disguise.entityId.isBlank()) {
				unit.disguise.entityId = normalizeMinecraftId(rawUnit.get("disguiseId").getAsString());
				migrated = true;
			}
		}
		if (rawUnit.has("abilityName") && !rawUnit.get("abilityName").isJsonNull() && (unit.abilityName == null || unit.abilityName.isBlank())) {
			unit.abilityName = rawUnit.get("abilityName").getAsString();
			migrated = true;
		}
		if (rawUnit.has("abilityCooldownSeconds") && !rawUnit.get("abilityCooldownSeconds").isJsonNull() && unit.abilityCooldownSeconds <= 0) {
			unit.abilityCooldownSeconds = rawUnit.get("abilityCooldownSeconds").getAsInt();
			migrated = true;
		}
		if (!unit.abilityItem.isEmpty()
			&& (unit.abilityItem.displayName == null || unit.abilityItem.displayName.isBlank())
			&& unit.abilityName != null
			&& !unit.abilityName.isBlank()) {
			unit.abilityItem.displayName = "&b" + unit.abilityName;
			unit.abilityItem.loreLines = List.of("&7유닛 스킬 발동", "&8쿨다운: &f" + unit.abilityCooldownSeconds + "초");
			migrated = true;
		}
		return migrated;
	}

	private boolean migrateLegacyItem(JsonObject rawUnit, String legacyKey, UnitItemEntry itemEntry) {
		if (itemEntry == null || !rawUnit.has(legacyKey) || rawUnit.get(legacyKey).isJsonNull()) {
			return false;
		}
		var legacyId = rawUnit.get(legacyKey).getAsString();
		if (legacyId == null || legacyId.isBlank()) {
			return false;
		}
		itemEntry.itemId = normalizeMinecraftId(legacyId);
		if (itemEntry.count <= 0) {
			itemEntry.count = 1;
		}
		return true;
	}

	private String normalizeMinecraftId(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.contains(":") ? value : "minecraft:" + value;
	}

	private SystemConfig.PositionConfig legacyPosition(JsonObject rawJson, String... path) {
		var object = nestedObject(rawJson, path);
		if (object == null || !object.has("x") || !object.has("y") || !object.has("z")) {
			return null;
		}
		var position = SystemConfig.PositionConfig.create(
			object.get("x").getAsDouble(),
			object.get("y").getAsDouble(),
			object.get("z").getAsDouble()
		);
		position.yaw = object.has("yaw") ? object.get("yaw").getAsFloat() : 0.0f;
		position.pitch = object.has("pitch") ? object.get("pitch").getAsFloat() : 0.0f;
		return position;
	}

	private SystemConfig.PositionConfig copyPosition(SystemConfig.PositionConfig source) {
		var copy = SystemConfig.PositionConfig.create(source.x, source.y, source.z);
		copy.yaw = source.yaw;
		copy.pitch = source.pitch;
		return copy;
	}

	private JsonObject nestedObject(JsonObject root, String... path) {
		JsonObject current = root;
		for (var key : path) {
			if (current == null || !current.has(key) || !current.get(key).isJsonObject()) {
				return null;
			}
			current = current.getAsJsonObject(key);
		}
		return current;
	}

	private String legacyString(JsonObject root, String... path) {
		var current = root;
		for (int i = 0; i < path.length - 1; i++) {
			if (current == null || !current.has(path[i]) || !current.get(path[i]).isJsonObject()) {
				return null;
			}
			current = current.getAsJsonObject(path[i]);
		}
		var leaf = path[path.length - 1];
		if (current == null || !current.has(leaf) || current.get(leaf).isJsonNull()) {
			return null;
		}
		return current.get(leaf).getAsString();
	}

	private String firstNonBlank(String... values) {
		for (var value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return "minecraft:overworld";
	}

	private BiomeCatalog createDefaultBiomeCatalog() {
		var catalog = new BiomeCatalog();
		catalog.biomes = List.of(
			createBiome("forest", "숲", "minecraft:forest", "minecraftsnap:forest_lane", List.of("&a고요한 숲 라인 공개", "&7울창한 나무가 시야를 가름"), 90, List.of("&2숲의 바람이 라인을 감쌈")),
			createBiome("desert", "사막", "minecraft:desert", "minecraftsnap:desert_lane", List.of("&e사막 라인 공개", "&7건조한 지형이 시야를 비움"), 90, List.of("&6사막의 열기가 다시 퍼짐")),
			createBiome("swamp", "늪", "minecraft:swamp", "minecraftsnap:swamp_lane", List.of("&2늪 라인 공개", "&7축축한 기운이 라인을 덮음"), 90, List.of("&a늪의 안개가 다시 짙어짐")),
			createBiome("badlands", "악지", "minecraft:badlands", "minecraftsnap:badlands_lane", List.of("&c악지 라인 공개", "&7붉은 절벽이 전장을 감쌈"), 120, List.of("&6악지의 모래먼지가 다시 이는 중"))
		);
		catalog.normalize();
		return catalog;
	}

	private BiomeEntry createBiome(String id, String name, String minecraftBiomeId, String structureId, List<String> revealMessages, int pulseIntervalSeconds, List<String> pulseMessages) {
		var entry = new BiomeEntry();
		entry.id = id;
		entry.displayName = name;
		entry.minecraftBiomeId = minecraftBiomeId;
		entry.effectType = id;
		entry.structureId = structureId;
		entry.descriptionLines = List.of("&7대표 바이옴: &f" + minecraftBiomeId, "&7공개 후 분위기 설명 표시");
		entry.revealMessages = revealMessages;
		entry.pulseIntervalSeconds = pulseIntervalSeconds;
		entry.pulseMessages = pulseMessages;
		entry.revealSoundId = "minecraft:block.note_block.pling";
		entry.pulseSoundId = "minecraft:block.note_block.chime";
		entry.normalize();
		return entry;
	}

	private FactionConfigFile createDefaultFactionConfig(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> createVillagerFactionConfig();
			case MONSTER -> createMonsterFactionConfig();
			case NETHER -> createNetherFactionConfig();
		};
	}

	private ShopConfigFile createDefaultShopConfig(FactionId factionId) {
		var config = new ShopConfigFile();
		config.entries = switch (factionId) {
			case VILLAGER -> List.of(
				shop("bread_bundle", 1, "minecraft:bread", 4),
				shop("cooked_beef", 2, "minecraft:cooked_beef", 3),
				shop("arrow_pack", 2, "minecraft:arrow", 8)
			);
			case NETHER -> List.of(
				shop("fire_charge", 1, "minecraft:fire_charge", 2),
				shop("cooked_porkchop", 2, "minecraft:cooked_porkchop", 3),
				shop("golden_carrot", 2, "minecraft:golden_carrot", 2)
			);
			case MONSTER -> List.of();
		};
		config.normalize();
		return config;
	}

	private FactionConfigFile createVillagerFactionConfig() {
		var config = new FactionConfigFile();
		config.displayName = "주민&우민";
		config.summaryLines = List.of("&7균형형 팩션", "&7거래와 유지력이 강점");
		config.captainSkill.name = "습격 소집";
		config.captainSkill.descriptionLines = List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 JSON 기준");
		config.units = List.of(
			unit("villager", "주민", true, 1, 5, 20.0, 0.8, "minecraft:wooden_sword", "", "", "", "", "", "minecraft:bread", "밥먹기", 10, "NONE", "minecraft:villager", List.of("&7체력 3 회복", "&7기본 유지력 유닛")),
			unit("armorer_villager", "대장장이 주민", true, 2, 9, 30.0, 0.9, "minecraft:wooden_sword", "minecraft:shield", "", "minecraft:iron_chestplate", "", "", "", "", 0, "NONE", "minecraft:villager", List.of("&7방패와 흉갑 보유", "&7전선 유지용")),
			unit("vindicator", "변명자", true, 4, 18, 30.0, 1.0, "minecraft:iron_axe", "", "", "", "", "", "minecraft:iron_axe", "도약", 10, "NONE", "minecraft:vindicator", List.of("&7짧은 돌진 스킬", "&7근접 압박용")),
			unit("pillager", "약탈자", true, 3, 15, 16.0, 1.1, "minecraft:crossbow", "", "", "", "", "", "minecraft:firework_rocket", "폭죽 화살 지급", 15, "FIREWORK", "minecraft:pillager", List.of("&73발 폭죽 지급", "&7원거리 견제용"))
		);
		config.normalize();
		return config;
	}

	private FactionConfigFile createMonsterFactionConfig() {
		var config = new FactionConfigFile();
		config.displayName = "몬스터";
		config.summaryLines = List.of("&7전직과 기습 중심", "&7환경 적응이 핵심");
		config.captainSkill.name = "날씨 변화";
		config.captainSkill.descriptionLines = List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 JSON 기준");
		var zombie = unit("zombie", "좀비", true, 1, 6, 20.0, 0.8, "minecraft:iron_shovel", "", "", "", "", "", "", "", 0, "NONE", "minecraft:zombie", List.of("&7적 처치 시 사령관 소환 쿨 2초 감소"));
		zombie.advanceOptions = List.of(advanceOption("zombie_veteran", "강화 좀비", List.of("&7늪과 비를 버티면 강화"), List.of("minecraft:swamp", "minecraft:mangrove_swamp"), List.of("rain", "thunder"), 15));
		var skeleton = unit("skeleton", "스켈레톤", true, 2, 13, 14.0, 1.0, "minecraft:bow", "", "", "", "", "", "minecraft:bone", "뼈 폭발", 15, "ARROW", "minecraft:skeleton", List.of("&74칸 내 적 둔화 타격"));
		skeleton.advanceOptions = List.of(advanceOption("skeleton_sniper", "강화 스켈레톤", List.of("&7설원 계열에서 정찰병화"), List.of("minecraft:snowy_plains", "minecraft:snowy_taiga"), List.of("clear", "rain"), 15));
		var slime = unit("slime", "슬라임", true, 2, 10, 10.0, 1.1, "minecraft:slime_ball", "", "", "", "", "", "", "", 0, "NONE", "minecraft:slime", List.of("&7사망 시 작은 슬라임 3마리"));
		slime.advanceOptions = List.of(advanceOption("slime_brute", "강화 슬라임", List.of("&7늪지에서 더 거대해짐"), List.of("minecraft:swamp", "minecraft:mangrove_swamp"), List.of("clear", "rain", "thunder"), 12));
		var creeper = unit("creeper", "크리퍼", true, 5, 25, 20.0, 1.0, "minecraft:tnt", "", "", "", "", "", "minecraft:tnt", "자폭", 20, "NONE", "minecraft:creeper", List.of("&71초 뒤 자폭", "&7근접 폭발 특화"));
		creeper.advanceOptions = List.of(advanceOption("charged_creeper", "대전된 크리퍼", List.of("&7천둥 아래에서 대전됨"), List.of("minecraft:plains", "minecraft:forest", "minecraft:dark_forest"), List.of("thunder"), 10));
		config.units = List.of(
			zombie,
			skeleton,
			slime,
			creeper,
			unit("zombie_veteran", "강화 좀비", false, 0, 0, 26.0, 0.95, "minecraft:iron_shovel", "", "", "minecraft:iron_chestplate", "", "", "", "", 0, "NONE", "minecraft:husk", List.of("&7늪/비 조건 전직 결과")),
			unit("skeleton_sniper", "강화 스켈레톤", false, 0, 0, 18.0, 1.05, "minecraft:bow", "", "", "", "", "", "minecraft:bone", "뼈 폭발", 12, "ARROW", "minecraft:stray", List.of("&7설원 계열 전직 결과")),
			unit("slime_brute", "강화 슬라임", false, 0, 0, 18.0, 1.2, "minecraft:slime_ball", "", "", "", "", "", "", "", 0, "NONE", "minecraft:slime", List.of("&7늪 지형 전직 결과")),
			unit("charged_creeper", "대전된 크리퍼", false, 0, 0, 24.0, 1.05, "minecraft:tnt", "", "", "", "", "", "minecraft:tnt", "자폭", 15, "NONE", "minecraft:creeper", List.of("&7천둥 조건 전직 결과"))
		);
		config.normalize();
		return config;
	}

	private FactionConfigFile createNetherFactionConfig() {
		var config = new FactionConfigFile();
		config.displayName = "네더";
		config.summaryLines = List.of("&7교전 강화형 팩션", "&7금괴 경제와 돌파력");
		config.captainSkill.name = "포탈 생성";
		config.captainSkill.descriptionLines = List.of("&7실제 스킬 로직은 기존 구현 유지", "&7위키와 GUI 설명은 JSON 기준");
		config.units = List.of(
			unit("piglin", "피글린", true, 2, 5, 20.0, 1.0, "minecraft:golden_sword", "", "", "", "", "", "", "", 0, "NONE", "minecraft:piglin", List.of("&750% 확률로 좀비 피글린 생성")),
			unit("zombified_piglin", "좀비 피글린", true, 2, 8, 20.0, 0.8, "minecraft:golden_sword", "", "", "", "", "", "minecraft:golden_sword", "분노", 15, "NONE", "minecraft:zombified_piglin", List.of("&7주변 아군 강화")),
			unit("blaze", "블레이즈", true, 3, 16, 16.0, 1.2, "minecraft:blaze_rod", "", "", "", "", "", "minecraft:blaze_rod", "화염구", 5, "NONE", "minecraft:blaze", List.of("&7작은 화염구 3연사")),
			unit("piglin_brute", "피글린 브루트", true, 6, 25, 40.0, 1.0, "minecraft:golden_axe", "", "", "", "", "", "minecraft:golden_axe", "광란", 30, "NONE", "minecraft:piglin_brute", List.of("&7자가 강화 폭발력"))
		);
		config.normalize();
		return config;
	}

	private FactionUnitEntry unit(
		String id,
		String displayName,
		boolean captainSpawnable,
		int cost,
		int spawnCooldownSeconds,
		double maxHealth,
		double moveSpeedScale,
		String mainHandItemId,
		String offHandItemId,
		String helmetItemId,
		String chestItemId,
		String legsItemId,
		String bootsItemId,
		String abilityItemId,
		String abilityName,
		int abilityCooldownSeconds,
		String ammoType,
		String disguiseId,
		List<String> descriptionLines
	) {
		var entry = new FactionUnitEntry();
		entry.id = id;
		entry.displayName = displayName;
		entry.captainSpawnable = captainSpawnable;
		entry.cost = cost;
		entry.spawnCooldownSeconds = spawnCooldownSeconds;
		entry.maxHealth = maxHealth;
		entry.moveSpeedScale = moveSpeedScale;
		entry.mainHand = UnitItemEntry.create(mainHandItemId);
		entry.offHand = UnitItemEntry.create(offHandItemId);
		entry.helmet = UnitItemEntry.create(helmetItemId);
		entry.chest = UnitItemEntry.create(chestItemId);
		entry.legs = UnitItemEntry.create(legsItemId);
		entry.boots = UnitItemEntry.create(bootsItemId);
		entry.abilityItem = UnitItemEntry.create(abilityItemId);
		entry.abilityName = abilityName;
		entry.abilityCooldownSeconds = abilityCooldownSeconds;
		if (abilityItemId != null && !abilityItemId.isBlank() && abilityName != null && !abilityName.isBlank()) {
			entry.abilityItem.displayName = "&b" + abilityName;
			entry.abilityItem.loreLines = List.of("&7유닛 스킬 발동", "&8쿨다운: &f" + abilityCooldownSeconds + "초");
		}
		entry.ammoType = ammoType;
		entry.disguise = EntitySpecEntry.create(disguiseId);
		entry.descriptionLines = descriptionLines;
		entry.normalize();
		return entry;
	}

	private ShopEntry shop(String id, int price, String itemId, int count) {
		var entry = new ShopEntry();
		entry.id = id;
		entry.price = price;
		entry.item = UnitItemEntry.create(itemId);
		entry.item.count = count;
		entry.normalize();
		return entry;
	}

	private AdvanceOptionEntry advanceOption(
		String resultUnitId,
		String displayName,
		List<String> descriptionLines,
		List<String> biomes,
		List<String> weathers,
		int requiredTicks
	) {
		var option = new AdvanceOptionEntry();
		option.resultUnitId = resultUnitId;
		option.displayName = displayName;
		option.descriptionLines = descriptionLines;
		option.biomes = biomes;
		option.weathers = weathers;
		option.requiredTicks = requiredTicks;
		option.normalize();
		return option;
	}

	private boolean migrateLegacyAdvanceOptions() {
		var monsterConfig = factionConfigs.get(FactionId.MONSTER);
		if (monsterConfig == null || systemConfig == null || systemConfig.advance == null || systemConfig.advance.conditions == null) {
			return false;
		}
		boolean migrated = false;
		for (var unit : monsterConfig.units) {
			if (unit == null || !unit.advanceOptions.isEmpty()) {
				continue;
			}
			for (var condition : systemConfig.advance.conditions) {
				if (condition == null || !unit.id.equals(condition.unitId)) {
					continue;
				}
				unit.advanceOptions.add(advanceOption(
					condition.resultUnitId,
					resolveUnitName(condition.resultUnitId),
					List.of("&7레거시 system.json 전직 조건 마이그레이션"),
					List.copyOf(condition.biomes),
					List.copyOf(condition.weathers),
					condition.requiredExp > 0 ? condition.requiredExp : 1
				));
				migrated = true;
			}
			unit.normalize();
		}
		return migrated;
	}

	private String resolveUnitName(String unitId) {
		for (var config : factionConfigs.values()) {
			if (config == null || config.units == null) {
				continue;
			}
			for (var unit : config.units) {
				if (unit != null && unitId.equals(unit.id) && unit.displayName != null && !unit.displayName.isBlank()) {
					return unit.displayName;
				}
			}
		}
		return unitId;
	}
}
