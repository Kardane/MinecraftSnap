package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MinecraftSnapConfigManager {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private final Path configDirectory;
	private final Logger logger;
	private SystemConfig systemConfig = new SystemConfig();
	private StatsRepository statsRepository;

	public MinecraftSnapConfigManager(Path configDirectory, Logger logger) {
		this.configDirectory = configDirectory;
		this.logger = logger;
	}

	public void load() {
		try {
			Files.createDirectories(configDirectory);
			systemConfig = loadOrCreate(configDirectory.resolve("system.json"), SystemConfig.class, new SystemConfig());
			writeIfMissing(configDirectory.resolve("biomes.json"), """
				{
				  "biomes": []
				}
				""");
			writeIfMissing(configDirectory.resolve("faction_villager.json"), """
				{
				  "captain_skill": {},
				  "units": []
				}
				""");
			writeIfMissing(configDirectory.resolve("faction_monster.json"), """
				{
				  "captain_skill": {},
				  "units": []
				}
				""");
			writeIfMissing(configDirectory.resolve("faction_nether.json"), """
				{
				  "captain_skill": {},
				  "units": []
				}
				""");

			statsRepository = new StatsRepository(configDirectory.resolve("stats.json"), logger);
			statsRepository.load();
		} catch (IOException exception) {
			logger.error("MCsnap 컨픽 디렉터리 초기화 실패", exception);
			systemConfig = new SystemConfig();
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

	private <T> T loadOrCreate(Path path, Class<T> type, T defaultValue) throws IOException {
		if (Files.notExists(path)) {
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				gson.toJson(defaultValue, writer);
			}
			return defaultValue;
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			var loaded = gson.fromJson(reader, type);
			return loaded != null ? loaded : defaultValue;
		}
	}

	private void writeIfMissing(Path path, String content) throws IOException {
		if (Files.exists(path)) {
			return;
		}

		Files.writeString(path, content, StandardCharsets.UTF_8);
	}
}
