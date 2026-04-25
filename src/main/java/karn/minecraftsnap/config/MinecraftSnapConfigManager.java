package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.UnitClassRegistry;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
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
	private ServerStatsRepository serverStatsRepository;

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
			serverStatsRepository = new ServerStatsRepository(configDirectory.resolve("server_stats.json"), logger);
			serverStatsRepository.load();
		} catch (IOException exception) {
			logger.error("MCsnap 컨픽 디렉터리 초기화 실패", exception);
			systemConfig = new SystemConfig();
			textConfig = new TextConfigFile();
			textConfig.normalize();
			textConfig.applyTo(systemConfig);
			biomeCatalog = mergeDefaultBiomes(createDefaultBiomeCatalog());
			shopConfigs.clear();
			shopConfigs.put(FactionId.VILLAGER, createSeparatedDefaultShopConfig(FactionId.VILLAGER));
			shopConfigs.put(FactionId.NETHER, createSeparatedDefaultShopConfig(FactionId.NETHER));
			unitConfigs.clear();
			loadBundledUnitConfigsFallback();
			statsRepository = new StatsRepository(configDirectory.resolve("stats.json"), logger);
			statsRepository.load();
			serverStatsRepository = new ServerStatsRepository(configDirectory.resolve("server_stats.json"), logger);
			serverStatsRepository.load();
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

	public ServerStatsRepository getServerStatsRepository() {
		return serverStatsRepository;
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

	public Map<String, UnitDefinition> getUnitDefinitions() {
		var definitions = new LinkedHashMap<String, UnitDefinition>();
		for (var entry : unitConfigs.entrySet()) {
			definitions.put(entry.getKey(), entry.getValue().toDefinition());
		}
		return definitions;
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
			|| "biomerevealcaptainwarningtitle".equals(normalized)
			|| "gameendtitletemplate".equals(normalized)
			|| "gameenddrawtitletemplate".equals(normalized)
			|| "gameendladderdeltatitletemplate".equals(normalized)) {
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
		for (var unitId : new UnitClassRegistry().configuredUnitIds()) {
			var defaults = bundledUnitConfig(unitId);
			var path = unitConfigPath(unitDir, defaults);
			var mergedJson = gson.toJsonTree(defaults).getAsJsonObject();
			if (Files.notExists(path)) {
				var legacyPath = legacyUnitConfigPath(unitDir, unitId);
				if (Files.exists(legacyPath)) {
					mergeJsonObjects(mergedJson, readJsonObject(legacyPath));
				}
			} else {
				mergeJsonObjects(mergedJson, readJsonObject(path));
			}
			var loaded = gson.fromJson(mergedJson, UnitConfigEntry.class);
			if (loaded == null) {
				loaded = defaults;
			}
			loaded.normalize();
			sanitizeUnitConfigEnums(loaded, defaults);
			writeJson(path, loaded);
			unitConfigs.put(unitId, loaded);
		}
	}

	private UnitConfigEntry bundledUnitConfig(String unitId) throws IOException {
		var resourcePath = "/default-config/unit/" + unitId + ".json";
		try (var stream = MinecraftSnapConfigManager.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IOException("기본 유닛 컨픽 리소스 누락: " + resourcePath);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				var loaded = gson.fromJson(reader, UnitConfigEntry.class);
				if (loaded == null) {
					throw new IOException("기본 유닛 컨픽 파싱 실패: " + resourcePath);
				}
				loaded.normalize();
				return loaded;
			}
		}
	}

	private void loadBundledUnitConfigsFallback() {
		for (var unitId : new UnitClassRegistry().configuredUnitIds()) {
			try {
				unitConfigs.put(unitId, bundledUnitConfig(unitId));
			} catch (IOException exception) {
				logger.error("기본 유닛 컨픽 로드 실패: {}", unitId, exception);
			}
		}
	}

	private void sanitizeUnitConfigEnums(UnitConfigEntry loaded, UnitConfigEntry defaults) {
		if (loaded == null || defaults == null) {
			return;
		}
		if (!isValidFactionId(loaded.factionId)) {
			loaded.factionId = defaults.factionId;
		}
		if (!isValidAmmoType(loaded.ammoType)) {
			loaded.ammoType = defaults.ammoType;
		}
	}

	private boolean isValidFactionId(String factionId) {
		try {
			FactionId.valueOf(factionId);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private boolean isValidAmmoType(String ammoType) {
		try {
			UnitDefinition.AmmoType.valueOf(ammoType);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private Path unitConfigPath(Path unitDir, UnitConfigEntry config) throws IOException {
		var factionDir = unitDir.resolve(config.factionId.toLowerCase(java.util.Locale.ROOT));
		Files.createDirectories(factionDir);
		return factionDir.resolve(config.id + ".json");
	}

	private Path legacyUnitConfigPath(Path unitDir, String unitId) {
		return unitDir.resolve(unitId + ".json");
	}

	private ShopConfigFile loadShopConfig(Path path, FactionId factionId) throws IOException {
		var defaults = createSeparatedDefaultShopConfig(factionId);
		var loaded = loadOrCreate(path, ShopConfigFile.class, defaults);
		if (loaded == null) {
			loaded = defaults;
		}
		loaded.normalize();
		loaded = sanitizeShopConfig(loaded, defaults, factionId);
		loaded.normalize();
		writeJson(path, loaded);
		return loaded;
	}

	private ShopConfigFile sanitizeShopConfig(ShopConfigFile config, ShopConfigFile defaults, FactionId factionId) {
		if (config == null) {
			return defaults;
		}
		var expectedType = expectedShopType(factionId);
		if (expectedType == null) {
			return defaults;
		}
		var sanitized = new ShopConfigFile();
		sanitized.entries = config.entries == null
			? new java.util.ArrayList<>()
			: config.entries.stream()
				.filter(entry -> entry != null && expectedType.equals(entry.type))
				.toList();
		sanitized.normalize();
		return sanitized.entries.isEmpty() ? defaults : sanitized;
	}

	private String expectedShopType(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> "enchant";
			case NETHER -> "item";
			default -> null;
		};
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
		entries.add(typed(createBiome("cold_ocean", "차가운 바다", "minecraft:cold_ocean", "", List.of("&b====================", "&b 바이옴 공개 - 차가운 바다", "&b  물에 닿은 모든 생명체는 1초마다 빙결 수치가 30 증가합니다.", "&b====================")), "special"));
		entries.add(typed(createNoOpBiome("deep_ocean", "깊은 바다", "minecraft:deep_ocean", "", List.of("&3====================", "&3 바이옴 공개 - 깊은 바다", "&3  별도 효과가 없습니다.", "&3====================")), "special"));
		entries.add(createBiome("reverse_icicle", "역고드름", "minecraft:frozen_peaks", "", List.of("&7====================", "&7 바이옴 공개 - 역고드름", "&7  점령 점수가 2초마다 들어옵니다.", "&7====================")));
		entries.add(createNoOpBiome("jungle", "정글", "minecraft:jungle", "minecraft:jungle_sapling", List.of("&2====================", "&2 바이옴 공개 - 정글", "&2  별도 효과가 없습니다.", "&2====================")));
		entries.add(createBiome("dripstone_caves", "점적석 동굴", "minecraft:dripstone_caves", "minecraft:pointed_dripstone", List.of("&6====================", "&6 바이옴 공개 - 점적석 동굴", "&6  종유석이 천장에서 떨어집니다.", "&6====================")));
		entries.add(createBiome("soul_sand_valley", "영혼 골짜기", "minecraft:soul_sand_valley", "", List.of("&7====================", "&7 바이옴 공개 - 영혼 골짜기", "&7  유닛 사망 위치에 시듦 구름이 남습니다.", "&7====================")));
		entries.add(createNoOpBiome("savanna_hills", "사바나 언덕", "minecraft:savanna_plateau", "minecraft:acacia_sapling", List.of("&6====================", "&6 바이옴 공개 - 사바나 언덕", "&6  별도 효과가 없습니다.", "&6====================")));
		entries.add(createBiome("bamboo_jungle", "대나무 정글", "minecraft:bamboo_jungle", "", List.of("&2====================", "&2 바이옴 공개 - 대나무 정글", "&2  해당 라인의 모든 유닛 공격 피해가 99 증가합니다.", "&2====================")));
		entries.add(createNoOpBiome("mangrove_swamp", "맹그로브 늪", "minecraft:mangrove_swamp", "minecraft:mangrove_propagule", List.of("&2====================", "&2 바이옴 공개 - 맹그로브 늪", "&2  별도 효과가 없습니다.", "&2====================")));
		entries.add(typed(createNoOpBiome("buried_treasure_beach", "보물이 묻힌 해변", "minecraft:beach", "minecraft:chest", List.of("&e====================", "&e 구조물 공개 - 보물이 묻힌 해변", "&e  별도 효과가 없습니다.", "&e====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:buried_treasure_beach";
		entries.add(typed(createBiome("end_city", "엔드 시티", "minecraft:end_highlands", "", List.of("&d====================", "&d 구조물 공개 - 엔드 시티", "&d  남은 게임 시간이 1분 증가합니다.", "&d====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:end_city";
		entries.add(typed(createBiome("bastion_remnant", "보루 잔해", "minecraft:nether_wastes", "minecraft:gold_block", List.of("&4====================", "&4 구조물 공개 - 보루 잔해", "&4  전투가 더욱 거칠어집니다.", "&4====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:bastion_remnant";
		entries.add(typed(createBiome("ocean_monument", "바다 신전", "minecraft:deep_ocean", "", List.of("&3====================", "&3 구조물 공개 - 바다 신전", "&3  가디언 외 유닛은 채굴피로 II를 유지합니다.", "&3====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:ocean_monument";
		entries.add(typed(createBiome("jungle_temple", "정글 사원", "minecraft:jungle", "minecraft:tripwire_hook", List.of("&2====================", "&2 구조물 공개 - 정글 사원", "&2  사원 함정이 라인을 노립니다.", "&2====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:jungle_temple";
		entries.add(typed(createBiome("ender_ruins", "엔더 유적", "minecraft:end_midlands", "minecraft:end_stone_bricks", List.of("&5====================", "&5 구조물 공개 - 엔더 유적", "&5  유적이 점점 라인을 잠식합니다.", "&5====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:ender_ruins";
		entries.add(typed(createNoOpBiome("end_barrens", "엔드 불모지", "minecraft:end_barrens", "minecraft:end_stone", List.of("&8====================", "&8 구조물 공개 - 엔드 불모지", "&8  별도 효과가 없습니다.", "&8====================")), "structure"));
		entries.get(entries.size() - 1).structureId = "minecraftsnap:end_barrens";
		applyLaneWeights(entries.get(entries.size() - 1), 0, 0, 0);
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
		if (current.type == null || current.type.isBlank()) {
			current.type = defaults.type;
		}
		if (current.lane1Weight == null) {
			current.lane1Weight = defaults.lane1Weight;
		}
		if (current.lane2Weight == null) {
			current.lane2Weight = defaults.lane2Weight;
		}
		if (current.lane3Weight == null) {
			current.lane3Weight = defaults.lane3Weight;
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
			createNoOpBiome("forest", "숲", "minecraft:forest", "minecraft:oak_sapling", List.of("&2====================", "&2 바이옴 공개 - 숲", "&2  별도 효과가 없습니다.", "&2====================")),
			createNoOpBiome("birch_forest", "자작나무 숲", "minecraft:birch_forest", "minecraft:birch_sapling", List.of("&f====================", "&f 바이옴 공개 - 자작나무 숲", "&f  별도 효과가 없습니다.", "&f====================")),
			createBiome("flower_forest", "꽃 숲", "minecraft:flower_forest", "minecraft:oxeye_daisy", List.of("&d====================", "&d 바이옴 공개 - 꽃 숲", "&d  점령 구역 안의 유닛이 흡수 1을 유지합니다.", "&d====================")),
			typed(createBiome("desert", "사막", "minecraft:desert", "minecraft:desert", List.of("&a====================", "&a 바이옴 공개 - 사막", "&a  해당 라인의 모든 유닛이 신속 2를 얻습니다.", "&a====================")), "special"),
			typed(createBiome("swamp", "늪", "minecraft:swamp", "minecraft:swamp", List.of("&a====================", "&a 바이옴 공개 - 늪", "&a  30초마다 점령 지역의 유닛이 상태 이상 효과를 얻습니다.", "&a====================")), "special"),
			typed(createBiome("badlands", "악지", "minecraft:badlands", "minecraft:badlands", List.of("&c악지 라인 공개", "&7붉은 절벽이 전장을 감쌈")), "special"),
			createBiome("end", "엔드", "minecraft:the_end", "minecraft:end", List.of("&5====================", "&5 바이옴 공개 - 엔드", "&5  남은 시간이 60초 감소합니다.", "&5====================")),
			createBiome("deep_dark", "딥다크", "minecraft:deep_dark", "minecraft:deep_dark", List.of("&1====================", "&1 바이옴 공개 - 딥다크", "&1  고요한 어둠이 내려앉는다.", "&1====================")),
			createBiome("nether", "네더", "minecraft:nether_wastes", "minecraft:nether", List.of("&4====================", "&4 바이옴 공개 - 네더", "&4  해당 라인의 모든 유닛 공격 피해가 1 증가합니다.", "&4====================")),
			typed(createNoOpBiome("taiga", "타이가", "minecraft:taiga", "minecraft:taiga", List.of("&b====================", "&b 바이옴 공개 - 타이가", "&b  별도 효과가 없습니다.", "&b====================")), "special")
		);
	}

	private BiomeEntry typed(BiomeEntry entry, String type) {
		entry.type = type;
		applyDefaultLaneWeights(entry, type);
		entry.normalize();
		return entry;
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
		applyLaneWeights(entry, 10, 10, 10);
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

	private void applyDefaultLaneWeights(BiomeEntry entry, String type) {
		if (entry == null || type == null || type.isBlank()) {
			return;
		}
		if ("special".equalsIgnoreCase(type)) {
			applyLaneWeights(entry, 8, 10, 10);
			return;
		}
		if ("structure".equalsIgnoreCase(type)) {
			applyLaneWeights(entry, 0, 10, 10);
		}
	}

	private void applyLaneWeights(BiomeEntry entry, int lane1Weight, int lane2Weight, int lane3Weight) {
		if (entry == null) {
			return;
		}
		entry.lane1Weight = lane1Weight;
		entry.lane2Weight = lane2Weight;
		entry.lane3Weight = lane3Weight;
	}

	private ShopConfigFile createSeparatedDefaultShopConfig(FactionId factionId) {
		var config = new ShopConfigFile();
		config.entries = switch (factionId) {
			case VILLAGER -> List.of(
				enchantShop("sharpness_upgrade", "날카로움 강화", "minecraft:iron_sword", "minecraft:sharpness", "weapon", 30, 50, 70),
				enchantShop("protection_upgrade", "보호 강화", "minecraft:diamond_chestplate", "minecraft:protection", "armor", 30, 50, 70, 90, 120)
			);
			case NETHER -> List.of(
				shop("golden_helmet", 1, "minecraft:golden_helmet", 1),
				shop("golden_chestplate", 2, "minecraft:golden_chestplate", 1),
				shop("golden_leggings", 3, "minecraft:golden_leggings", 1),
				shop("golden_boots", 4, "minecraft:golden_boots", 1)
			);
			case MONSTER -> List.of();
		};
		config.normalize();
		return config;
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
