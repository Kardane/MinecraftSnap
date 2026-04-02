package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MinecraftSnapConfigManager {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private final Path configDirectory;
	private final Logger logger;
	private SystemConfig systemConfig = new SystemConfig();
	private TextConfigFile textConfig = new TextConfigFile();
	private BiomeCatalog biomeCatalog = new BiomeCatalog();
	private final EnumMap<FactionId, ShopConfigFile> shopConfigs = new EnumMap<>(FactionId.class);
	private final Map<String, UnitConfigEntry> unitConfigs = new LinkedHashMap<>();
	private StatsRepository statsRepository;

	public MinecraftSnapConfigManager(Path configDirectory, Logger logger) {
		this.configDirectory = configDirectory;
		this.logger = logger;
	}

	public void load() {
		try {
			Files.createDirectories(configDirectory);
			systemConfig = loadSystemConfig(configDirectory.resolve("system.json"));
			textConfig = loadTextConfig();
			textConfig.normalize();
			textConfig.applyTo(systemConfig);
			biomeCatalog = loadOrCreate(configDirectory.resolve("biomes.json"), BiomeCatalog.class, createDefaultBiomeCatalog());
			biomeCatalog = mergeDefaultBiomes(biomeCatalog);
			biomeCatalog.normalize();
			writeJson(configDirectory.resolve("biomes.json"), biomeCatalog);
			shopConfigs.clear();
			shopConfigs.put(FactionId.VILLAGER, loadShopConfig(configDirectory.resolve("villager_shop.json"), FactionId.VILLAGER));
			shopConfigs.put(FactionId.NETHER, loadShopConfig(configDirectory.resolve("nether_shop.json"), FactionId.NETHER));
			loadUnitConfigs();

			statsRepository = new StatsRepository(configDirectory.resolve("stats.json"), logger);
			statsRepository.load();
		} catch (IOException exception) {
			logger.error("MCsnap 컨픽 디렉터리 초기화 실패", exception);
			systemConfig = new SystemConfig();
			textConfig = new TextConfigFile();
			textConfig.normalize();
			textConfig.applyTo(systemConfig);
			biomeCatalog = mergeDefaultBiomes(createDefaultBiomeCatalog());
			shopConfigs.clear();
			shopConfigs.put(FactionId.VILLAGER, createDefaultShopConfig(FactionId.VILLAGER));
			shopConfigs.put(FactionId.NETHER, createDefaultShopConfig(FactionId.NETHER));
			unitConfigs.clear();
			var defaultRegistry = new UnitRegistry();
			for (var definition : defaultRegistry.all()) {
				unitConfigs.put(definition.id(), UnitConfigEntry.from(definition));
			}
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

	public Map<String, UnitConfigEntry> getUnitConfigs() {
		return new LinkedHashMap<>(unitConfigs);
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
		migrateCaptainSpawnDefaults(loaded);
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

	private TextConfigFile loadTextConfig() throws IOException {
		var textDir = configDirectory.resolve("text");
		Files.createDirectories(textDir);
		var merged = new JsonObject();
		var loadedAny = false;
		var hasSplitFiles = false;
		for (var section : TextSection.values()) {
			var path = textDir.resolve(section.fileName);
			if (!Files.exists(path)) {
				continue;
			}
			hasSplitFiles = true;
			mergeJsonObjects(merged, readJsonObject(path));
			loadedAny = true;
		}
		var legacyTexts = configDirectory.resolve("texts.json");
		if (!hasSplitFiles && Files.exists(legacyTexts)) {
			mergeJsonObjects(merged, readJsonObject(legacyTexts));
			loadedAny = true;
		}
		var loaded = loadedAny ? gson.fromJson(merged, TextConfigFile.class) : new TextConfigFile();
		if (loaded == null) {
			loaded = new TextConfigFile();
		}
		loaded.normalize();
		writeTextConfigSections(textDir, loaded);
		return loaded;
	}

	private JsonObject readJsonObject(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private void writeTextConfigSections(Path textDir, TextConfigFile config) throws IOException {
		var raw = gson.toJsonTree(config).getAsJsonObject();
		var sections = new EnumMap<TextSection, JsonObject>(TextSection.class);
		for (var section : TextSection.values()) {
			sections.put(section, new JsonObject());
		}
		for (var entry : raw.entrySet()) {
			sections.get(classifyTextSection(entry.getKey())).add(entry.getKey(), entry.getValue());
		}
		for (var section : TextSection.values()) {
			writeJson(textDir.resolve(section.fileName), sections.get(section));
		}
	}

	private TextSection classifyTextSection(String key) {
		var normalized = key == null ? "" : key.toLowerCase(java.util.Locale.ROOT);
		if (normalized.contains("subtitle")) {
			return TextSection.SUBTITLE;
		}
		if ("gamestartcountdowntitle".equals(normalized)
			|| "gameendtitletemplate".equals(normalized)
			|| "gameenddrawtitletemplate".equals(normalized)) {
			return TextSection.TITLE;
		}
		if (normalized.contains("actionbar") || normalized.contains("hud")) {
			return TextSection.ACTIONBAR;
		}
		if (normalized.contains("bossbar")) {
			return TextSection.BOSSBAR;
		}
		if (normalized.contains("scoreboard") || normalized.contains("sidebar")) {
			return TextSection.SIDEBAR;
		}
		if (normalized.contains("chat")
			|| normalized.contains("message")
			|| normalized.contains("broadcast")
			|| normalized.contains("vote")
			|| normalized.contains("death")
			|| normalized.contains("progress")
			|| normalized.contains("phase")
			|| normalized.contains("queue")
			|| normalized.contains("ownerchanged")
			|| normalized.contains("preferenceupdated")
			|| normalized.startsWith("command")) {
			return TextSection.CHAT;
		}
		return TextSection.GUI;
	}

	private void mergeJsonObjects(JsonObject target, JsonObject source) {
		for (var entry : source.entrySet()) {
			var key = entry.getKey();
			var sourceValue = entry.getValue();
			if (target.has(key) && target.get(key).isJsonObject() && sourceValue.isJsonObject()) {
				mergeJsonObjects(target.getAsJsonObject(key), sourceValue.getAsJsonObject());
				continue;
			}
			target.add(key, sourceValue.deepCopy());
		}
	}

	private void loadUnitConfigs() throws IOException {
		unitConfigs.clear();
		var unitDir = configDirectory.resolve("unit");
		Files.createDirectories(unitDir);
		var defaultRegistry = new UnitRegistry();
		for (var definition : defaultRegistry.all()) {
			var path = unitConfigPath(unitDir, definition);
			var defaults = UnitConfigEntry.from(definition);
			UnitConfigEntry loaded;
			if (Files.notExists(path)) {
				var legacyPath = legacyUnitConfigPath(unitDir, definition);
				if (Files.exists(legacyPath)) {
					var mergedJson = gson.toJsonTree(defaults).getAsJsonObject();
					mergeJsonObjects(mergedJson, readJsonObject(legacyPath));
					loaded = gson.fromJson(mergedJson, UnitConfigEntry.class);
				} else {
					loaded = defaults;
				}
			} else {
				var mergedJson = gson.toJsonTree(defaults).getAsJsonObject();
				mergeJsonObjects(mergedJson, readJsonObject(path));
				loaded = gson.fromJson(mergedJson, UnitConfigEntry.class);
			}
			if (loaded == null) {
				loaded = defaults;
			}
			loaded.normalize();
			var merged = loaded.mergeOnto(definition);
			var stored = UnitConfigEntry.from(merged);
			writeJson(path, stored);
			unitConfigs.put(definition.id(), stored);
		}
	}

	private Path unitConfigPath(Path unitDir, karn.minecraftsnap.game.UnitDefinition definition) throws IOException {
		var factionDir = unitDir.resolve(definition.factionId().name().toLowerCase(java.util.Locale.ROOT));
		Files.createDirectories(factionDir);
		return factionDir.resolve(definition.id() + ".json");
	}

	private Path legacyUnitConfigPath(Path unitDir, karn.minecraftsnap.game.UnitDefinition definition) {
		return unitDir.resolve(definition.id() + ".json");
	}

	private ShopConfigFile loadShopConfig(Path path, FactionId factionId) throws IOException {
		var defaults = createDefaultShopConfig(factionId);
		var loaded = loadOrCreate(path, ShopConfigFile.class, defaults);
		if (loaded == null) {
			loaded = defaults;
		}
		loaded.normalize();
		if (factionId == FactionId.VILLAGER && shouldMigrateLegacyVillagerShop(loaded)) {
			loaded = defaults;
		}
		loaded.normalize();
		writeJson(path, loaded);
		return loaded;
	}

	private boolean shouldMigrateLegacyVillagerShop(ShopConfigFile config) {
		return config != null
			&& config.entries != null
			&& !config.entries.isEmpty()
			&& config.entries.stream().noneMatch(entry -> "enchant".equals(entry.type));
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

	private void migrateCaptainSpawnDefaults(SystemConfig loaded) {
		if (loaded == null || loaded.gameStart == null) {
			return;
		}
		if (isLegacyCentralCaptainSpawn(loaded.gameStart.redCaptainSpawn, -10.0)) {
			loaded.gameStart.redCaptainSpawn = SystemConfig.PositionConfig.create(-10.0, 64.0, -10.0);
		}
		if (isLegacyCentralCaptainSpawn(loaded.gameStart.blueCaptainSpawn, 10.0)) {
			loaded.gameStart.blueCaptainSpawn = SystemConfig.PositionConfig.create(10.0, 64.0, -10.0);
		}
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

	private boolean isLegacyCentralCaptainSpawn(SystemConfig.PositionConfig position, double expectedX) {
		if (position == null) {
			return false;
		}
		return position.x == expectedX
			&& position.y == 64.0
			&& position.z == 10.0
			&& position.yaw == 0.0f
			&& position.pitch == 0.0f;
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
		catalog.biomes = defaultBiomeEntriesWithVoid();
		catalog.normalize();
		return catalog;
	}

	private BiomeCatalog mergeDefaultBiomes(BiomeCatalog loaded) {
		var merged = new BiomeCatalog();
		if (loaded == null || loaded.biomes == null) {
			merged.biomes = defaultBiomeEntriesWithVoid();
			return merged;
		}
		var defaultsById = new java.util.LinkedHashMap<String, BiomeEntry>();
		for (var defaultEntry : defaultBiomeEntriesWithVoid()) {
			defaultsById.put(defaultEntry.id, defaultEntry);
		}
		var seen = new java.util.LinkedHashSet<String>();
		for (var entry : loaded.biomes) {
			var defaults = defaultsById.get(entry.id);
			merged.biomes.add(mergeBiomeEntry(entry, defaults));
			seen.add(entry.id);
		}
		for (var defaultEntry : defaultBiomeEntriesWithVoid()) {
			if (!seen.contains(defaultEntry.id)) {
				merged.biomes.add(defaultEntry);
			}
		}
		return merged;
	}

	private List<BiomeEntry> defaultBiomeEntriesWithVoid() {
		var entries = new java.util.ArrayList<>(defaultBiomeEntries());
		entries.add(createNoOpBiome("basalt_deltas", "현무암 지대", "minecraft:basalt_deltas", "minecraft:basalt", List.of("&8====================", "&8 바이옴 공개 - 현무암 지대", "&8  별도 효과가 없습니다.", "&8====================")));
		entries.add(createBiome("lush_cave", "무성한 동굴", "minecraft:lush_caves", "", List.of("&2====================", "&2 바이옴 공개 - 무성한 동굴", "&2  30초마다 해당 라인 모든 유닛에게 재생 II를 10초 부여합니다.", "&2====================")));
		entries.add(createBiome("mushroom_island", "버섯섬", "minecraft:mushroom_fields", "", List.of("&d====================", "&d 바이옴 공개 - 버섯섬", "&d  해당 라인 모든 유닛이 3초마다 체력을 1 회복합니다.", "&d====================")));
		entries.add(createBiome("cold_ocean", "차가운 바다", "minecraft:cold_ocean", "", List.of("&b====================", "&b 바이옴 공개 - 차가운 바다", "&b  물에 닿은 유닛은 1초마다 빙결 수치가 30 증가합니다.", "&b====================")));
		entries.add(createBiome("reverse_icicle", "역고드름", "minecraft:frozen_peaks", "", List.of("&7====================", "&7 바이옴 공개 - 역고드름", "&7  점령지를 점령해도 점수를 얻을 수 없습니다.", "&7====================")));
		entries.add(createNoOpBiome("void", "공허", "minecraft:the_void", "minecraft:obsidian", List.of("&8====================", "&8 바이옴 공개 - 공허", "&8  별도 효과가 없는 빈 공간입니다.", "&8====================")));
		return entries;
	}

	private BiomeEntry mergeBiomeEntry(BiomeEntry current, BiomeEntry defaults) {
		if (current == null) {
			return defaults;
		}
		if (defaults == null) {
			return current;
		}
		if (current.displayName == null || current.displayName.isBlank()) {
			current.displayName = defaults.displayName;
		}
		if (current.descriptionLines == null || current.descriptionLines.isEmpty()) {
			current.descriptionLines = defaults.descriptionLines;
		}
		if (current.minecraftBiomeId == null || current.minecraftBiomeId.isBlank() || "minecraft:plains".equals(current.minecraftBiomeId) && !"minecraft:plains".equals(defaults.minecraftBiomeId)) {
			current.minecraftBiomeId = defaults.minecraftBiomeId;
		}
		if (current.effectType == null || current.effectType.isBlank() || "noop".equals(current.effectType)) {
			current.effectType = defaults.effectType;
		}
		if (current.structureId == null || current.structureId.isBlank()) {
			current.structureId = defaults.structureId;
		}
		if (current.revealMessages == null || current.revealMessages.isEmpty()) {
			current.revealMessages = defaults.revealMessages;
		}
		if (current.revealSoundId == null || current.revealSoundId.isBlank()) {
			current.revealSoundId = defaults.revealSoundId;
		}
		return current;
	}

	private List<BiomeEntry> defaultBiomeEntries() {
		return List.of(
			createBiome("plain", "평원", "minecraft:plains", "minecraft:plain", List.of("&a====================", "&a 바이옴 공개 - 평원", "&a  기본적인 필드다.", "&a====================")),
			createBiome("desert", "사막", "minecraft:desert", "minecraft:desert", List.of("&a====================", "&a 바이옴 공개 - 사막", "&a  해당 라인의 모든 유닛이 신속 2를 얻습니다.", "&a====================")),
			createBiome("swamp", "늪", "minecraft:swamp", "minecraft:swamp", List.of("&a====================", "&a 바이옴 공개 - 늪", "&a  30초마다 점령 지역의 유닛이 상태 이상 효과를 얻습니다.", "&a====================")),
			createBiome("badlands", "악지", "minecraft:badlands", "minecraft:badlands", List.of("&c악지 라인 공개", "&7붉은 절벽이 전장을 감쌈")),
			createBiome("end", "엔드", "minecraft:the_end", "minecraft:end", List.of("&5====================", "&5 바이옴 공개 - 엔드", "&5  남은 시간이 60초 감소합니다.", "&5====================")),
			createBiome("deep_dark", "딥다크", "minecraft:deep_dark", "minecraft:deep_dark", List.of("&1====================", "&1 바이옴 공개 - 딥다크", "&1  고요한 어둠이 내려앉는다.", "&1====================")),
			createBiome("nether", "네더", "minecraft:nether_wastes", "minecraft:nether", List.of("&4====================", "&4 바이옴 공개 - 네더", "&4  뜨거운 열기가 라인을 감싼다.", "&4====================")),
			createBiome("taiga", "타이가", "minecraft:taiga", "minecraft:taiga", List.of("&b====================", "&b 바이옴 공개 - 타이가", "&b  해당 라인의 모든 유닛이 구속 1을 얻습니다.", "&b===================="))
		);
	}

	private BiomeEntry createBiome(String id, String name, String minecraftBiomeId, String structureId, List<String> revealMessages) {
		var entry = new BiomeEntry();
		entry.id = id;
		entry.displayName = name;
		entry.minecraftBiomeId = minecraftBiomeId;
		entry.effectType = id;
		entry.structureId = structureId;
		entry.descriptionLines = List.of("&7대표 바이옴: &f" + minecraftBiomeId, "&7공개 후 분위기 설명 표시");
		entry.revealMessages = revealMessages;
		entry.revealSoundId = "minecraft:block.note_block.pling";
		entry.normalize();
		return entry;
	}

	private BiomeEntry createNoOpBiome(String id, String name, String minecraftBiomeId, String displayItemId, List<String> revealMessages) {
		var entry = createBiome(id, name, minecraftBiomeId, "", revealMessages);
		entry.effectType = "noop";
		entry.displayItemId = displayItemId;
		entry.normalize();
		return entry;
	}

	private ShopConfigFile createDefaultShopConfig(FactionId factionId) {
		var config = new ShopConfigFile();
		config.entries = switch (factionId) {
			case VILLAGER -> List.of(
				enchantShop("sharpness_upgrade", "?좎뭅濡쒖? 媛뺥솕", "minecraft:iron_sword", "minecraft:sharpness", "weapon", 5, 10, 15),
				enchantShop("protection_upgrade", "蹂댄샇 媛뺥솕", "minecraft:iron_chestplate", "minecraft:protection", "armor", 5, 10, 15)
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

	private ShopEntry shop(String id, int price, String itemId, int count) {
		var entry = new ShopEntry();
		entry.id = id;
		entry.type = "item";
		entry.price = price;
		entry.item = UnitItemEntry.create(itemId);
		entry.item.count = count;
		entry.normalize();
		return entry;
	}

	private ShopEntry enchantShop(String id, String displayName, String iconItemId, String enchantmentId, String target, int... prices) {
		var entry = new ShopEntry();
		entry.id = id;
		entry.type = "enchant";
		entry.enchantmentId = enchantmentId;
		entry.target = target;
		entry.maxLevel = prices == null ? 1 : prices.length;
		entry.prices = prices == null
			? List.of(1)
			: java.util.Arrays.stream(prices).boxed().toList();
		entry.price = entry.prices.isEmpty() ? 1 : entry.prices.getFirst();
		entry.item = UnitItemEntry.create(iconItemId);
		entry.item.displayName = "&b" + displayName;
		entry.normalize();
		return entry;
	}

	private enum TextSection {
		GUI("gui.json"),
		BOSSBAR("bossbar.json"),
		SIDEBAR("sidebar.json"),
		ACTIONBAR("actionbar.json"),
		TITLE("title.json"),
		SUBTITLE("subtitle.json"),
		CHAT("chat.json");

		private final String fileName;

		TextSection(String fileName) {
			this.fileName = fileName;
		}
	}

}
