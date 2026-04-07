package karn.minecraftsnap.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import karn.minecraftsnap.game.FactionId;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class ServerStatsRepository {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private final Logger logger;
	private final Path path;
	private ServerStatsFile statsFile = new ServerStatsFile();
	private boolean dirty;

	public ServerStatsRepository(Path path, Logger logger) {
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
				var loaded = gson.fromJson(reader, ServerStatsFile.class);
				this.statsFile = loaded != null ? loaded : new ServerStatsFile();
				if (this.statsFile.unitStats == null) {
					this.statsFile.unitStats = new LinkedHashMap<>();
				}
				if (this.statsFile.factionStats == null) {
					this.statsFile.factionStats = new LinkedHashMap<>();
				}
				this.dirty = false;
			}
		} catch (IOException exception) {
			logger.error("server_stats.json 로드 실패", exception);
			this.statsFile = new ServerStatsFile();
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
			logger.error("server_stats.json 저장 실패", exception);
		}
	}

	public void saveIfDirty() {
		if (dirty) {
			save();
		}
	}

	public ServerUnitStats getOrCreateUnitStats(String unitId) {
		var key = normalizeUnitId(unitId);
		var stats = statsFile.unitStats.get(key);
		if (stats == null) {
			stats = new ServerUnitStats();
			statsFile.unitStats.put(key, stats);
			dirty = true;
		}
		return stats;
	}

	public ServerFactionStats getOrCreateFactionStats(FactionId factionId) {
		var key = normalizeFactionId(factionId);
		var stats = statsFile.factionStats.get(key);
		if (stats == null) {
			stats = new ServerFactionStats();
			statsFile.factionStats.put(key, stats);
			dirty = true;
		}
		return stats;
	}

	public ServerUnitStats findUnitStats(String unitId) {
		var key = normalizeUnitId(unitId);
		if (key.isBlank()) {
			return null;
		}
		return statsFile.unitStats.get(key);
	}

	public ServerFactionStats findFactionStats(FactionId factionId) {
		if (factionId == null) {
			return null;
		}
		return statsFile.factionStats.get(normalizeFactionId(factionId));
	}

	public void recordUnitPick(String unitId) {
		if (unitId == null || unitId.isBlank()) {
			return;
		}
		getOrCreateUnitStats(unitId).picks++;
		dirty = true;
	}

	public void addUnitKill(String unitId, int amount) {
		if (unitId == null || unitId.isBlank() || amount <= 0) {
			return;
		}
		getOrCreateUnitStats(unitId).kills += amount;
		dirty = true;
	}

	public void addUnitDeath(String unitId, int amount) {
		if (unitId == null || unitId.isBlank() || amount <= 0) {
			return;
		}
		getOrCreateUnitStats(unitId).deaths += amount;
		dirty = true;
	}

	public void recordFactionGame(FactionId factionId, boolean won) {
		if (factionId == null) {
			return;
		}
		var stats = getOrCreateFactionStats(factionId);
		stats.games++;
		if (won) {
			stats.wins++;
		}
		dirty = true;
	}

	public void addCaptainSkillUse(FactionId factionId, int amount) {
		if (factionId == null || amount <= 0) {
			return;
		}
		getOrCreateFactionStats(factionId).captainSkillUses += amount;
		dirty = true;
	}

	public List<UnitStatsEntry> allUnitEntries() {
		return statsFile.unitStats.entrySet().stream()
			.map(entry -> new UnitStatsEntry(entry.getKey(), entry.getValue()))
			.toList();
	}

	public List<FactionStatsEntry> allFactionEntries() {
		return statsFile.factionStats.entrySet().stream()
			.map(entry -> new FactionStatsEntry(FactionId.valueOf(entry.getKey()), entry.getValue()))
			.toList();
	}

	private static String normalizeUnitId(String unitId) {
		return unitId == null ? "" : unitId.trim();
	}

	private static String normalizeFactionId(FactionId factionId) {
		return factionId.name();
	}

	public record UnitStatsEntry(String unitId, ServerUnitStats stats) {
	}

	public record FactionStatsEntry(FactionId factionId, ServerFactionStats stats) {
	}

	public static class ServerUnitStats {
		public int picks = 0;
		public int kills = 0;
		public int deaths = 0;
	}

	public static class ServerFactionStats {
		public int games = 0;
		public int wins = 0;
		public int captainSkillUses = 0;
	}
}

class ServerStatsFile {
	public java.util.Map<String, ServerStatsRepository.ServerUnitStats> unitStats = new LinkedHashMap<>();
	public java.util.Map<String, ServerStatsRepository.ServerFactionStats> factionStats = new LinkedHashMap<>();
}
