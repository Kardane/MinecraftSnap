package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LaneBiomeService {
	private final Map<LaneId, List<BiomeCellSnapshot>> originalBiomes = new EnumMap<>(LaneId.class);
	private final CommandExecutor commandExecutor;

	public LaneBiomeService() {
		this(new ServerCommandExecutor());
	}

	public LaneBiomeService(CommandExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;
	}

	public void prepareHiddenBiomes(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || !systemConfig.biomeReveal.applyHiddenVoidBiome) {
			return;
		}

		for (var laneId : LaneId.values()) {
			var region = regionOf(laneId, systemConfig);
			if (region == null) {
				continue;
			}
			snapshotLane(server, laneId, region);
			applyUniformBiome(server, region, systemConfig.biomeReveal.hiddenWorldKey);
		}
	}

	public void applyAssignedBiome(MinecraftServer server, LaneId laneId, SystemConfig systemConfig, String biomeId) {
		if (server == null || biomeId == null || biomeId.isBlank()) {
			return;
		}

		var region = regionOf(laneId, systemConfig);
		if (region == null) {
			return;
		}
		snapshotLane(server, laneId, region);
		applyUniformBiome(server, region, biomeId);
	}

	public void restoreAll(MinecraftServer server) {
		for (var snapshots : originalBiomes.values()) {
			applySnapshots(server, snapshots);
		}
		originalBiomes.clear();
	}

	public void rememberSnapshot(LaneId laneId, List<BiomeCellSnapshot> snapshots) {
		originalBiomes.put(laneId, new ArrayList<>(snapshots));
	}

	public List<BiomeCellSnapshot> getSnapshot(LaneId laneId) {
		return originalBiomes.getOrDefault(laneId, List.of());
	}

	static String buildFillBiomeCommand(BiomeCellSnapshot snapshot) {
		return "execute in " + snapshot.worldId()
			+ " run fillbiome "
			+ snapshot.fromX() + " " + snapshot.fromY() + " " + snapshot.fromZ() + " "
			+ snapshot.toX() + " " + snapshot.toY() + " " + snapshot.toZ() + " "
			+ snapshot.biomeId();
	}

	private void snapshotLane(MinecraftServer server, LaneId laneId, SystemConfig.LaneRegionConfig region) {
		if (originalBiomes.containsKey(laneId)) {
			return;
		}

		var world = server.getWorld(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(region.world)));
		if (world == null) {
			return;
		}
		originalBiomes.put(laneId, snapshotRegion(world, region));
	}

	private List<BiomeCellSnapshot> snapshotRegion(ServerWorld world, SystemConfig.LaneRegionConfig region) {
		var snapshots = new ArrayList<BiomeCellSnapshot>();
		for (var cell : collectCells(region)) {
			var biomeId = world.getBiome(new BlockPos(cell.fromX(), cell.fromY(), cell.fromZ())).getKey()
				.map(key -> key.getValue().toString())
				.orElse("minecraft:plains");
			snapshots.add(new BiomeCellSnapshot(region.world, cell.fromX(), cell.fromY(), cell.fromZ(), cell.toX(), cell.toY(), cell.toZ(), biomeId));
		}
		return snapshots;
	}

	private void applyUniformBiome(MinecraftServer server, SystemConfig.LaneRegionConfig region, String biomeId) {
		var snapshots = collectCells(region).stream()
			.map(cell -> new BiomeCellSnapshot(region.world, cell.fromX(), cell.fromY(), cell.fromZ(), cell.toX(), cell.toY(), cell.toZ(), biomeId))
			.toList();
		applySnapshots(server, snapshots);
	}

	private void applySnapshots(MinecraftServer server, List<BiomeCellSnapshot> snapshots) {
		for (var snapshot : snapshots) {
			commandExecutor.execute(server, buildFillBiomeCommand(snapshot));
		}
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
					cells.add(new Cell(
						x,
						y,
						z,
						Math.min(x + 3, maxX),
						Math.min(y + 3, maxY),
						Math.min(z + 3, maxZ)
					));
				}
			}
		}
		return cells;
	}

	private SystemConfig.LaneRegionConfig regionOf(LaneId laneId, SystemConfig systemConfig) {
		return switch (laneId) {
			case LANE_1 -> systemConfig.inGame.lane1Region;
			case LANE_2 -> systemConfig.inGame.lane2Region;
			case LANE_3 -> systemConfig.inGame.lane3Region;
		};
	}

	public interface CommandExecutor {
		boolean execute(MinecraftServer server, String command);
	}

	public record BiomeCellSnapshot(String worldId, int fromX, int fromY, int fromZ, int toX, int toY, int toZ, String biomeId) {
	}

	private record Cell(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
	}

	private static class ServerCommandExecutor implements CommandExecutor {
		@Override
		public boolean execute(MinecraftServer server, String command) {
			if (server == null) {
				return false;
			}
			try {
				server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), command);
				return true;
			} catch (Exception exception) {
				return false;
			}
		}
	}
}
