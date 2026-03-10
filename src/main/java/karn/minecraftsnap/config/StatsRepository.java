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
import java.util.UUID;

public class StatsRepository {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private final Logger logger;
	private final Path path;
	private StatsFile statsFile = new StatsFile();
	private boolean dirty;

	public StatsRepository(Path path, Logger logger) {
		this.path = path;
		this.logger = logger;
	}

	public void load() {
		try {
			Files.createDirectories(path.getParent());

			if (Files.notExists(path)) {
				save();
				return;
			}

			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				var loaded = gson.fromJson(reader, StatsFile.class);
				this.statsFile = loaded != null ? loaded : new StatsFile();
				if (this.statsFile.players == null) {
					this.statsFile.players = new java.util.LinkedHashMap<>();
				}
				this.dirty = false;
			}
		} catch (IOException exception) {
			logger.error("stats.json 로드 실패", exception);
			this.statsFile = new StatsFile();
			this.dirty = false;
		}
	}

	public void save() {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				gson.toJson(statsFile, writer);
			}
			dirty = false;
		} catch (IOException exception) {
			logger.error("stats.json 저장 실패", exception);
		}
	}

	public void saveIfDirty() {
		if (dirty) {
			save();
		}
	}

	public PlayerStats getOrCreate(UUID playerId, String name) {
		var key = playerId.toString();
		var stats = statsFile.players.computeIfAbsent(key, ignored -> new PlayerStats());
		stats.lastKnownName = name;
		dirty = true;
		return stats;
	}

	public void setPreference(UUID playerId, String name, String preference) {
		var stats = getOrCreate(playerId, name);
		stats.preference = preference;
		dirty = true;
	}

	public int getLadder(UUID playerId, String name) {
		return getOrCreate(playerId, name).ladder;
	}

	public void addLadder(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.ladder += amount;
		dirty = true;
	}

	public void addCapture(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.captures += amount;
		dirty = true;
	}
}
