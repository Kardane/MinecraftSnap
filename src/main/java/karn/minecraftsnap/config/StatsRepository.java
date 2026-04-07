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
import java.util.List;
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
				boolean migrated = false;
				for (var stats : this.statsFile.players.values()) {
					if (stats == null) {
						continue;
					}
					if ("unit".equalsIgnoreCase(stats.preference)) {
						stats.preference = "none";
						migrated = true;
					}
				}
				this.dirty = migrated;
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
		var stats = statsFile.players.get(key);
		if (stats == null) {
			stats = new PlayerStats();
			statsFile.players.put(key, stats);
			dirty = true;
		}
		if (!name.equals(stats.lastKnownName)) {
			stats.lastKnownName = name;
			dirty = true;
		}
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

	public String getLastKnownName(UUID playerId, String fallback) {
		var stats = statsFile.players.get(playerId.toString());
		if (stats == null || stats.lastKnownName == null || stats.lastKnownName.isBlank()) {
			return fallback;
		}
		return stats.lastKnownName;
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

	public void addKill(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.kills += amount;
		dirty = true;
	}

	public void addDeath(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.deaths += amount;
		dirty = true;
	}

	public void addWin(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.wins += amount;
		dirty = true;
	}

	public void addLoss(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.losses += amount;
		dirty = true;
	}

	public void addCaptainGame(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.captainGames += amount;
		dirty = true;
	}

	public void addEmeralds(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.emeralds += amount;
		dirty = true;
	}

	public void addGoldIngots(UUID playerId, String name, int amount) {
		var stats = getOrCreate(playerId, name);
		stats.goldIngots += amount;
		dirty = true;
	}

	public void addAdvanceCount(UUID playerId, String name, int amount) {
		if (amount <= 0) {
			return;
		}
		var stats = getOrCreate(playerId, name);
		stats.advanceCount += amount;
		dirty = true;
	}

	public void addAssist(UUID playerId, String name, int amount) {
		if (amount <= 0) {
			return;
		}
		var stats = getOrCreate(playerId, name);
		stats.assists += amount;
		dirty = true;
	}

	public void addPlayTimeSeconds(UUID playerId, String name, int amount) {
		if (amount <= 0) {
			return;
		}
		var stats = getOrCreate(playerId, name);
		stats.playTimeSeconds += amount;
		dirty = true;
	}

	public void addDamageDealt(UUID playerId, String name, double amount) {
		if (amount <= 0.0D) {
			return;
		}
		var stats = getOrCreate(playerId, name);
		stats.totalDamageDealt += amount;
		dirty = true;
	}

	public void addHealingDone(UUID playerId, String name, double amount) {
		if (amount <= 0.0D) {
			return;
		}
		var stats = getOrCreate(playerId, name);
		stats.totalHealingDone += amount;
		dirty = true;
	}

	public List<PlayerStatsEntry> allEntries() {
		return statsFile.players.entrySet().stream()
			.map(entry -> new PlayerStatsEntry(UUID.fromString(entry.getKey()), entry.getValue()))
			.toList();
	}

	public record PlayerStatsEntry(UUID playerId, PlayerStats stats) {
	}
}
