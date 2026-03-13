package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkBiomeDataS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LaneBiomeService {
	private final Map<LaneId, List<BiomeCellSnapshot>> originalBiomes = new EnumMap<>(LaneId.class);
	private final Map<LaneId, String> currentBiomeIds = new EnumMap<>(LaneId.class);
	private final BiomeApplier biomeApplier;

	public LaneBiomeService() {
		this(new WorldBiomeApplier());
	}

	public LaneBiomeService(BiomeApplier biomeApplier) {
		this.biomeApplier = biomeApplier;
	}

	public void prepareHiddenBiomes(MinecraftServer server, SystemConfig systemConfig) {
		if (!systemConfig.biomeReveal.applyHiddenVoidBiome) {
			return;
		}

		prepareHiddenBiomes(server, systemConfig.world, Map.of(
			LaneId.LANE_1, targetRegionOf(LaneId.LANE_1, systemConfig),
			LaneId.LANE_2, targetRegionOf(LaneId.LANE_2, systemConfig),
			LaneId.LANE_3, targetRegionOf(LaneId.LANE_3, systemConfig)
		), systemConfig.biomeReveal.hiddenWorldKey);
	}

	public void prepareHiddenBiomes(
		MinecraftServer server,
		String worldId,
		Map<LaneId, SystemConfig.LaneRegionConfig> laneRegions,
		String biomeId
	) {
		if (biomeId == null || biomeId.isBlank()) {
			return;
		}
		for (var entry : laneRegions.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			snapshotLane(server, entry.getKey(), worldId, entry.getValue());
			applyBiome(server, entry.getKey(), worldId, entry.getValue(), biomeId);
		}
	}

	public void applyAssignedBiome(MinecraftServer server, LaneId laneId, SystemConfig systemConfig, String biomeId) {
		applyAssignedBiome(server, laneId, systemConfig.world, targetRegionOf(laneId, systemConfig), biomeId);
	}

	public void applyAssignedBiome(
		MinecraftServer server,
		LaneId laneId,
		String worldId,
		SystemConfig.LaneRegionConfig region,
		String biomeId
	) {
		if (biomeId == null || biomeId.isBlank()) {
			return;
		}
		if (region == null) {
			return;
		}
		snapshotLane(server, laneId, worldId, region);
		applyBiome(server, laneId, worldId, region, biomeId);
	}

	public void restoreAll(MinecraftServer server) {
		for (var entry : originalBiomes.entrySet()) {
			biomeApplier.apply(server, entry.getValue());
		}
		originalBiomes.clear();
		currentBiomeIds.clear();
	}

	public void rememberSnapshot(LaneId laneId, List<BiomeCellSnapshot> snapshots) {
		originalBiomes.put(laneId, new ArrayList<>(snapshots));
	}

	public List<BiomeCellSnapshot> getSnapshot(LaneId laneId) {
		return originalBiomes.getOrDefault(laneId, List.of());
	}

	private void snapshotLane(MinecraftServer server, LaneId laneId, String worldId, SystemConfig.LaneRegionConfig region) {
		if (originalBiomes.containsKey(laneId)) {
			return;
		}

		var snapshots = biomeApplier.snapshot(server, worldId, region);
		if (!snapshots.isEmpty()) {
			originalBiomes.put(laneId, new ArrayList<>(snapshots));
		}
	}

	private void applyBiome(MinecraftServer server, LaneId laneId, String worldId, SystemConfig.LaneRegionConfig region, String biomeId) {
		if (biomeId.equals(currentBiomeIds.get(laneId))) {
			return;
		}

		var snapshots = collectCells(region).stream()
			.map(cell -> BiomeCellSnapshot.at(worldId, cell.blockX(), cell.blockY(), cell.blockZ(), biomeId))
			.toList();
		biomeApplier.apply(server, snapshots);
		currentBiomeIds.put(laneId, biomeId);
	}

	private List<Cell> collectCells(SystemConfig.LaneRegionConfig region) {
		var cells = new ArrayList<Cell>();
		int minX = (int) Math.floor(region.minX);
		int minY = (int) Math.floor(region.minY);
		int minZ = (int) Math.floor(region.minZ);
		int maxX = (int) Math.floor(region.maxX);
		int maxY = (int) Math.floor(region.maxY);
		int maxZ = (int) Math.floor(region.maxZ);

		for (int x = minX; x <= maxX; x += 4) {
			for (int y = minY; y <= maxY; y += 4) {
				for (int z = minZ; z <= maxZ; z += 4) {
					cells.add(new Cell(x, y, z));
				}
			}
		}
		return cells;
	}

	static Set<Long> collectChunkPositions(SystemConfig.LaneRegionConfig region) {
		var chunks = new LinkedHashSet<Long>();
		int minChunkX = ChunkSectionPos.getSectionCoord((int) Math.floor(region.minX));
		int maxChunkX = ChunkSectionPos.getSectionCoord((int) Math.floor(region.maxX));
		int minChunkZ = ChunkSectionPos.getSectionCoord((int) Math.floor(region.minZ));
		int maxChunkZ = ChunkSectionPos.getSectionCoord((int) Math.floor(region.maxZ));
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				chunks.add(chunkKey(chunkX, chunkZ));
			}
		}
		return chunks;
	}

	private static long chunkKey(int chunkX, int chunkZ) {
		return ((long) chunkX & 4294967295L) << 32 | ((long) chunkZ & 4294967295L);
	}

	static SystemConfig.LaneRegionConfig targetRegionOf(LaneId laneId, SystemConfig systemConfig) {
		return switch (laneId) {
			case LANE_1 -> systemConfig.inGame.lane1Region;
			case LANE_2 -> systemConfig.inGame.lane2Region;
			case LANE_3 -> systemConfig.inGame.lane3Region;
		};
	}

	public interface BiomeApplier {
		List<BiomeCellSnapshot> snapshot(MinecraftServer server, String worldId, SystemConfig.LaneRegionConfig region);

		void apply(MinecraftServer server, List<BiomeCellSnapshot> snapshots);
	}

	public record BiomeCellSnapshot(String worldId, int blockX, int blockY, int blockZ, String biomeId) {
		public static BiomeCellSnapshot at(String worldId, int blockX, int blockY, int blockZ, String biomeId) {
			return new BiomeCellSnapshot(worldId, blockX, blockY, blockZ, biomeId);
		}
	}

	private record Cell(int blockX, int blockY, int blockZ) {
	}

	private static class WorldBiomeApplier implements BiomeApplier {
		@Override
		public List<BiomeCellSnapshot> snapshot(MinecraftServer server, String worldId, SystemConfig.LaneRegionConfig region) {
			var world = resolveWorld(server, worldId);
			if (world == null) {
				return List.of();
			}

			var snapshots = new ArrayList<BiomeCellSnapshot>();
			int minX = (int) Math.floor(region.minX);
			int minY = (int) Math.floor(region.minY);
			int minZ = (int) Math.floor(region.minZ);
			int maxX = (int) Math.floor(region.maxX);
			int maxY = (int) Math.floor(region.maxY);
			int maxZ = (int) Math.floor(region.maxZ);
			for (int x = minX; x <= maxX; x += 4) {
				for (int y = minY; y <= maxY; y += 4) {
					for (int z = minZ; z <= maxZ; z += 4) {
						var biomeId = world.getBiome(new BlockPos(x, y, z)).getKey()
							.map(key -> key.getValue().toString())
							.orElse("minecraft:plains");
						snapshots.add(BiomeCellSnapshot.at(worldId, x, y, z, biomeId));
					}
				}
			}
			return snapshots;
		}

		@Override
		public void apply(MinecraftServer server, List<BiomeCellSnapshot> snapshots) {
			if (server == null || snapshots.isEmpty()) {
				return;
			}

			var changedChunks = new LinkedHashMap<ServerWorld, Map<Long, WorldChunk>>();
			for (var snapshot : snapshots) {
				var world = resolveWorld(server, snapshot.worldId());
				if (world == null) {
					continue;
				}
				var biomeEntry = resolveBiome(world, snapshot.biomeId());
				if (biomeEntry == null) {
					continue;
				}

				int chunkX = ChunkSectionPos.getSectionCoord(snapshot.blockX());
				int chunkZ = ChunkSectionPos.getSectionCoord(snapshot.blockZ());
				var chunk = world.getChunk(chunkX, chunkZ);
				int sectionY = ChunkSectionPos.getSectionCoord(snapshot.blockY());
				int sectionIndex = world.sectionCoordToIndex(sectionY);
				var sections = chunk.getSectionArray();
				if (sectionIndex < 0 || sectionIndex >= sections.length) {
					continue;
				}

				var section = sections[sectionIndex];
				@SuppressWarnings("unchecked")
				var biomeContainer = (PalettedContainer<RegistryEntry<Biome>>) section.getBiomeContainer();
				biomeContainer.lock();
				try {
					biomeContainer.set(
						ChunkSectionPos.getLocalCoord(snapshot.blockX()) >> 2,
						ChunkSectionPos.getLocalCoord(snapshot.blockY()) >> 2,
						ChunkSectionPos.getLocalCoord(snapshot.blockZ()) >> 2,
						biomeEntry
					);
				} finally {
					biomeContainer.unlock();
				}

				chunk.markNeedsSaving();
				changedChunks.computeIfAbsent(world, ignored -> new LinkedHashMap<>())
					.put(chunk.getPos().toLong(), chunk);
			}

			for (var entry : changedChunks.entrySet()) {
				for (var player : server.getPlayerManager().getPlayerList()) {
					if (player.getWorld() == entry.getKey()) {
						for (var chunk : entry.getValue().values()) {
							player.networkHandler.sendPacket(new ChunkDataS2CPacket(
								chunk,
								entry.getKey().getLightingProvider(),
								new java.util.BitSet(),
								new java.util.BitSet()
							));
						}
						player.networkHandler.sendPacket(ChunkBiomeDataS2CPacket.create(List.copyOf(entry.getValue().values())));
					}
				}
			}
		}

		private static ServerWorld resolveWorld(MinecraftServer server, String worldId) {
			if (server == null) {
				return null;
			}
			try {
				var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, Identifier.of(worldId));
				var world = server.getWorld(key);
				return world != null ? world : server.getOverworld();
			} catch (Exception ignored) {
				return server.getOverworld();
			}
		}

		private RegistryEntry<Biome> resolveBiome(ServerWorld world, String biomeId) {
			var identifier = Identifier.tryParse(biomeId);
			if (identifier == null) {
				return null;
			}
			return world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getEntry(identifier).orElse(null);
		}
	}
}
