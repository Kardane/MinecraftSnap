package karn.minecraftsnap.game;

import karn.minecraftsnap.biome.BiomeEffect;
import karn.minecraftsnap.biome.BiomeEffectRegistry;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.config.BiomeCatalog;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.BossBarFormatter;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BiomeRevealService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Random random;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final LaneStructureService laneStructureService;
	private final BiomeEffectRegistry biomeEffectRegistry;

	public BiomeRevealService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this(matchManager, textTemplateResolver, new Random(), null, null, null);
	}

	public BiomeRevealService(MatchManager matchManager, TextTemplateResolver textTemplateResolver, Random random) {
		this(matchManager, textTemplateResolver, random, null, null, null);
	}

	public BiomeRevealService(
		MatchManager matchManager,
		TextTemplateResolver textTemplateResolver,
		Random random,
		LaneRuntimeRegistry laneRuntimeRegistry,
		LaneStructureService laneStructureService,
		BiomeEffectRegistry biomeEffectRegistry
	) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.random = random;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.laneStructureService = laneStructureService;
		this.biomeEffectRegistry = biomeEffectRegistry;
	}

	public void prepareForMatch(MinecraftServer server, SystemConfig systemConfig, BiomeCatalog biomeCatalog, LaneBiomeService laneBiomeService) {
		var assignments = assignBiomes(biomeCatalog);
		for (var entry : assignments.entrySet()) {
			matchManager.setAssignedBiomeId(entry.getKey(), entry.getValue().id);
		}

		laneBiomeService.prepareHiddenBiomes(server, systemConfig.world, laneRegions(systemConfig), systemConfig.biomeReveal.hiddenWorldKey);

		if (systemConfig.biomeReveal.lane1RevealSecond == 0) {
			var lane1 = assignments.get(LaneId.LANE_1);
			if (lane1 != null) {
				activateBiomeForLane(server, systemConfig, laneBiomeService, LaneId.LANE_1, lane1, 0, true, false);
			}
		}
	}

	public List<LaneId> tick(MinecraftServer server, SystemConfig systemConfig, BiomeCatalog biomeCatalog, LaneBiomeService laneBiomeService) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return List.of();
		}

		var elapsedSeconds = matchManager.getElapsedSeconds();
		List<LaneId> revealed = List.of();
		if (matchManager.getServerTicks() % 20L == 0L) {
			revealed = syncRevealState(elapsedSeconds, systemConfig, biomeCatalog, laneBiomeService, server);
		}
		tickActiveEffects(server, systemConfig.world, elapsedSeconds);
		return revealed;
	}

	public int nextRevealRemainingSeconds(SystemConfig systemConfig) {
		if (systemConfig == null || systemConfig.biomeReveal == null) {
			return -1;
		}
		var elapsedSeconds = matchManager.getElapsedSeconds();
		int nextReveal = Integer.MAX_VALUE;
		nextReveal = nextRevealSecond(LaneId.LANE_1, systemConfig.biomeReveal.lane1RevealSecond, elapsedSeconds, nextReveal);
		nextReveal = nextRevealSecond(LaneId.LANE_2, systemConfig.biomeReveal.lane2RevealSecond, elapsedSeconds, nextReveal);
		nextReveal = nextRevealSecond(LaneId.LANE_3, systemConfig.biomeReveal.lane3RevealSecond, elapsedSeconds, nextReveal);
		return nextReveal == Integer.MAX_VALUE ? -1 : nextReveal;
	}

	public String nextRevealRemainingTime(SystemConfig systemConfig) {
		var remainingSeconds = nextRevealRemainingSeconds(systemConfig);
		return remainingSeconds < 0 ? "--:--" : BossBarFormatter.formatTime(remainingSeconds);
	}

	Map<LaneId, BiomeEntry> assignBiomes(BiomeCatalog catalog) {
		var result = new EnumMap<LaneId, BiomeEntry>(LaneId.class);
		var pool = new ArrayList<>(catalog.biomes);
		for (var laneId : LaneId.values()) {
			if (pool.isEmpty()) {
				break;
			}
			var selected = selectBiomeForLane(laneId, pool);
			result.put(laneId, selected);
		}
		return result;
	}

	private BiomeEntry selectBiomeForLane(LaneId laneId, List<BiomeEntry> pool) {
		if (laneId == LaneId.LANE_1) {
			var eligible = pool.stream()
				.filter(entry -> entry != null && !"reverse_icicle".equals(entry.id))
				.toList();
			if (!eligible.isEmpty()) {
				var selected = eligible.get(random.nextInt(eligible.size()));
				pool.remove(selected);
				return selected;
			}
		}
		return pool.remove(random.nextInt(pool.size()));
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
		if (override != null && !override) {
			return;
		}
		if (elapsedSeconds < revealSecond) {
			return;
		}

		var biome = findBiomeEntry(laneId, biomeCatalog);
		if (biome == null) {
			return;
		}

		var alreadyRevealed = matchManager.isLaneRevealed(laneId);
		var runtime = runtimeFor(laneId);
		var alreadyActivated = runtime != null && runtime.hasActiveBiome() && biome.id.equals(runtime.biomeEntry().id);

		if (!alreadyRevealed) {
			matchManager.revealLane(laneId);
		}
		if (alreadyActivated) {
			applyRevealEffectIfNeeded(server, systemConfig.world, laneId, biome, elapsedSeconds);
			return;
		}

		activateBiomeForLane(server, systemConfig, laneBiomeService, laneId, biome, elapsedSeconds, true, true);
		revealed.add(laneId);
	}

	private int nextRevealSecond(LaneId laneId, int revealSecond, int elapsedSeconds, int currentBest) {
		if (matchManager.isLaneRevealed(laneId)) {
			return currentBest;
		}
		return Math.min(currentBest, Math.max(0, revealSecond - elapsedSeconds));
	}

	private void activateBiomeForLane(
		MinecraftServer server,
		SystemConfig systemConfig,
		LaneBiomeService laneBiomeService,
		LaneId laneId,
		BiomeEntry biomeEntry,
		int elapsedSeconds,
		boolean announce,
		boolean applyRevealEffect
	) {
		var laneRegion = laneRegionOf(laneId, systemConfig);
		if (laneRegion == null) {
			return;
		}

		laneBiomeService.applyAssignedBiome(server, laneId, systemConfig.world, laneRegion, biomeEntry.minecraftBiomeId);
		var biomeEffect = biomeEffectRegistry == null ? null : biomeEffectRegistry.create(biomeEntry);
		var runtime = runtimeFor(laneId);
		if (runtime != null && biomeEffect != null) {
			runtime.revealBiome(biomeEntry, biomeEffect, elapsedSeconds);
			if (laneStructureService != null) {
				laneStructureService.placeStructure(
					server,
					systemConfig.world,
					laneId,
					biomeEntry.structureId,
					laneStructureService.originFor(laneRegion)
				);
			}
		}

		var context = createContext(server, systemConfig.world, laneId, biomeEntry, elapsedSeconds);
		if (announce) {
			broadcastRevealMessages(server, laneId, biomeEntry, biomeEffect, context, systemConfig);
		}
		if (applyRevealEffect && biomeEffect != null && context != null) {
			biomeEffect.onReveal(context);
			if (runtime != null) {
				runtime.markRevealEffectApplied();
			}
		}
	}

	private void applyRevealEffectIfNeeded(MinecraftServer server, String worldId, LaneId laneId, BiomeEntry biomeEntry, int elapsedSeconds) {
		var runtime = runtimeFor(laneId);
		if (runtime == null || !runtime.hasActiveBiome() || runtime.revealEffectApplied()) {
			return;
		}
		var context = createContext(server, worldId, laneId, biomeEntry, elapsedSeconds);
		if (context == null) {
			return;
		}
		runtime.biomeEffect().onReveal(context);
		runtime.markRevealEffectApplied();
	}

	private void tickActiveEffects(MinecraftServer server, String worldId, int elapsedSeconds) {
		if (laneRuntimeRegistry == null) {
			return;
		}
		for (var runtime : laneRuntimeRegistry.all()) {
			if (!matchManager.isLaneRevealed(runtime.laneId()) || !runtime.hasActiveBiome()) {
				continue;
			}
			var context = createContext(server, worldId, runtime.laneId(), runtime.biomeEntry(), elapsedSeconds);
			if (context != null) {
				runtime.biomeEffect().onTick(context);
			}
		}
	}

	private void broadcastRevealMessages(
		MinecraftServer server,
		LaneId laneId,
		BiomeEntry biomeEntry,
		BiomeEffect biomeEffect,
		BiomeRuntimeContext context,
		SystemConfig systemConfig
	) {
		if (server == null) {
			return;
		}
		var lines = biomeEffect == null || context == null ? biomeEntry.revealMessages : biomeEffect.revealMessages(context);
		if (lines == null || lines.isEmpty()) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(
				systemConfig.biomeReveal.messageTemplate,
				Map.of("{lane}", laneLabel(laneId))
			), false);
			if (context != null) {
				context.playSound(biomeEntry.revealSoundId);
			}
			return;
		}
		var placeholders = Map.of(
			"{lane}", laneLabel(laneId),
			"{biome}", biomeEntry.displayName
		);
		for (var line : lines) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(line, placeholders), false);
		}
		if (context != null) {
			context.playSound(biomeEntry.revealSoundId);
		}
	}

	private BiomeRuntimeContext createContext(MinecraftServer server, String worldId, LaneId laneId, BiomeEntry biomeEntry, int elapsedSeconds) {
		var runtime = runtimeFor(laneId);
		if (runtime == null || !runtime.hasActiveBiome()) {
			return null;
		}
		return new BiomeRuntimeContext(
			server,
			resolveWorld(server, worldId),
			matchManager,
			runtime,
			biomeEntry,
			textTemplateResolver,
			matchManager.getServerTicks(),
			elapsedSeconds
		);
	}

	private LaneRuntime runtimeFor(LaneId laneId) {
		return laneRuntimeRegistry == null ? null : laneRuntimeRegistry.get(laneId);
	}

	private Map<LaneId, SystemConfig.LaneRegionConfig> laneRegions(SystemConfig systemConfig) {
		return Map.of(
			LaneId.LANE_1, laneRegionOf(LaneId.LANE_1, systemConfig),
			LaneId.LANE_2, laneRegionOf(LaneId.LANE_2, systemConfig),
			LaneId.LANE_3, laneRegionOf(LaneId.LANE_3, systemConfig)
		);
	}

	private SystemConfig.LaneRegionConfig laneRegionOf(LaneId laneId, SystemConfig systemConfig) {
		var runtime = runtimeFor(laneId);
		if (runtime != null && runtime.laneRegion() != null) {
			return runtime.laneRegion();
		}
		return LaneBiomeService.targetRegionOf(laneId, systemConfig);
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

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번";
			case LANE_2 -> "2번";
			case LANE_3 -> "3번";
		};
	}

	private net.minecraft.server.world.ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		if (server == null) {
			return null;
		}
		try {
			var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}
}
