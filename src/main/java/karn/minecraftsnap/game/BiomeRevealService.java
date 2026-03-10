package karn.minecraftsnap.game;

import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BiomeRevealService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Random random;

	public BiomeRevealService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this(matchManager, textTemplateResolver, new Random());
	}

	public BiomeRevealService(MatchManager matchManager, TextTemplateResolver textTemplateResolver, Random random) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.random = random;
	}

	public void prepareForMatch(MinecraftServer server, SystemConfig systemConfig, BiomeCatalog biomeCatalog, LaneBiomeService laneBiomeService) {
		var assignments = assignBiomes(biomeCatalog);
		for (var entry : assignments.entrySet()) {
			matchManager.setAssignedBiomeId(entry.getKey(), entry.getValue().id);
		}
		laneBiomeService.prepareHiddenBiomes(server, systemConfig);
		if (systemConfig.biomeReveal.lane1RevealSecond == 0) {
			var lane1 = assignments.get(LaneId.LANE_1);
			if (lane1 != null) {
				laneBiomeService.applyAssignedBiome(server, LaneId.LANE_1, systemConfig, lane1.minecraftBiomeId);
			}
		}
	}

	public List<LaneId> tick(MinecraftServer server, SystemConfig systemConfig, BiomeCatalog biomeCatalog, LaneBiomeService laneBiomeService) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return List.of();
		}

		var elapsedSeconds = matchManager.getTotalSeconds() - matchManager.getRemainingSeconds();
		var revealed = syncRevealState(elapsedSeconds, systemConfig, biomeCatalog, laneBiomeService, server);
		for (var laneId : revealed) {
			broadcastRevealMessages(server, laneId, biomeCatalog, systemConfig);
		}
		broadcastPulseMessages(server, elapsedSeconds, biomeCatalog, systemConfig);
		return revealed;
	}

	Map<LaneId, BiomeEntry> assignBiomes(BiomeCatalog catalog) {
		var result = new EnumMap<LaneId, BiomeEntry>(LaneId.class);
		var pool = new ArrayList<>(catalog.biomes);
		for (var laneId : LaneId.values()) {
			if (pool.isEmpty()) {
				break;
			}
			var selected = pool.remove(random.nextInt(pool.size()));
			result.put(laneId, selected);
		}
		return result;
	}

	public List<LaneId> syncRevealState(
		int elapsedSeconds,
		SystemConfig systemConfig,
		BiomeCatalog biomeCatalog,
		LaneBiomeService laneBiomeService,
		MinecraftServer server
	) {
		List<LaneId> revealed = new ArrayList<>();
		revealIfDue(LaneId.LANE_1, elapsedSeconds, systemConfig.biomeReveal.lane1RevealSecond, revealed, biomeCatalog, laneBiomeService, server, systemConfig);
		revealIfDue(LaneId.LANE_2, elapsedSeconds, systemConfig.biomeReveal.lane2RevealSecond, revealed, biomeCatalog, laneBiomeService, server, systemConfig);
		revealIfDue(LaneId.LANE_3, elapsedSeconds, systemConfig.biomeReveal.lane3RevealSecond, revealed, biomeCatalog, laneBiomeService, server, systemConfig);
		return revealed;
	}

	private void revealIfDue(
		LaneId laneId,
		int elapsedSeconds,
		int revealSecond,
		List<LaneId> revealed,
		BiomeCatalog biomeCatalog,
		LaneBiomeService laneBiomeService,
		MinecraftServer server,
		SystemConfig systemConfig
	) {
		var override = matchManager.getLaneRevealOverride(laneId);
		if (override != null) {
			if (override) {
				matchManager.revealLane(laneId);
			}
			return;
		}

		if (elapsedSeconds < revealSecond || matchManager.isLaneRevealed(laneId)) {
			return;
		}

		matchManager.revealLane(laneId);
		var biome = findBiomeEntry(laneId, biomeCatalog);
		if (biome != null) {
			laneBiomeService.applyAssignedBiome(server, laneId, systemConfig, biome.minecraftBiomeId);
		}
		revealed.add(laneId);
	}

	private void broadcastRevealMessages(MinecraftServer server, LaneId laneId, BiomeCatalog biomeCatalog, SystemConfig systemConfig) {
		var biome = findBiomeEntry(laneId, biomeCatalog);
		if (biome == null) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(
				systemConfig.biomeReveal.messageTemplate,
				Map.of("{lane}", laneLabel(laneId))
			), false);
			return;
		}
		for (var line : biome.revealMessages) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(
				line,
				Map.of("{lane}", laneLabel(laneId), "{biome}", biome.displayName)
			), false);
		}
		playSound(server, biome.revealSoundId);
	}

	private void broadcastPulseMessages(MinecraftServer server, int elapsedSeconds, BiomeCatalog biomeCatalog, SystemConfig systemConfig) {
		for (var laneId : LaneId.values()) {
			if (!matchManager.isLaneRevealed(laneId)) {
				continue;
			}
			var revealSecond = revealSecond(laneId, systemConfig.biomeReveal);
			var biome = findBiomeEntry(laneId, biomeCatalog);
			if (biome == null || biome.pulseIntervalSeconds <= 0 || elapsedSeconds <= revealSecond) {
				continue;
			}
			if ((elapsedSeconds - revealSecond) % biome.pulseIntervalSeconds != 0) {
				continue;
			}
			for (var line : biome.pulseMessages) {
				server.getPlayerManager().broadcast(textTemplateResolver.format(
					line,
					Map.of("{lane}", laneLabel(laneId), "{biome}", biome.displayName)
				), false);
			}
			playSound(server, biome.pulseSoundId);
		}
	}

	private void playSound(MinecraftServer server, String soundId) {
		var identifier = Identifier.tryParse(soundId);
		if (identifier == null) {
			return;
		}
		var soundEvent = SoundEvent.of(identifier);
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.playSoundToPlayer(soundEvent, SoundCategory.MASTER, 1.0f, 1.0f);
		}
	}

	private BiomeEntry findBiomeEntry(LaneId laneId, BiomeCatalog biomeCatalog) {
		var assigned = matchManager.getAssignedBiomeId(laneId);
		if (assigned == null || biomeCatalog == null) {
			return null;
		}
		return biomeCatalog.biomes.stream()
			.filter(entry -> assigned.equals(entry.id))
			.findFirst()
			.orElse(null);
	}

	private int revealSecond(LaneId laneId, SystemConfig.BiomeRevealConfig config) {
		return switch (laneId) {
			case LANE_1 -> config.lane1RevealSecond;
			case LANE_2 -> config.lane2RevealSecond;
			case LANE_3 -> config.lane3RevealSecond;
		};
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번";
			case LANE_2 -> "2번";
			case LANE_3 -> "3번";
		};
	}
}
