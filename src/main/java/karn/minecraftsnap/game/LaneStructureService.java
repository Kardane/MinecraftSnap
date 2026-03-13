package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class LaneStructureService {
	static final int STRUCTURE_WIDTH = 109;
	static final int STRUCTURE_HEIGHT = 30;
	static final int STRUCTURE_DEPTH = 17;
	private final EnumSet<LaneId> placedLanes = EnumSet.noneOf(LaneId.class);
	private final StructurePlacer structurePlacer;

	public LaneStructureService() {
		this(new WorldStructurePlacer());
	}

	public LaneStructureService(StructurePlacer structurePlacer) {
		this.structurePlacer = structurePlacer;
	}

	public boolean placeStructure(MinecraftServer server, String worldId, LaneId laneId, String structureId, BlockPos originPos) {
		if (laneId == null || originPos == null || structureId == null || structureId.isBlank() || placedLanes.contains(laneId)) {
			return false;
		}
		var placed = structurePlacer.place(server, worldId, structureId, originPos);
		if (placed) {
			placedLanes.add(laneId);
		}
		return placed;
	}

	public boolean forcePlaceStructure(MinecraftServer server, String worldId, LaneId laneId, String structureId, BlockPos originPos) {
		if (laneId == null || originPos == null || structureId == null || structureId.isBlank()) {
			return false;
		}
		return structurePlacer.place(server, worldId, structureId, originPos);
	}

	public BlockPos originFor(SystemConfig.LaneRegionConfig region) {
		if (region == null) {
			return BlockPos.ORIGIN;
		}
		var centerX = Math.floor((region.minX + region.maxX) / 2.0D);
		var centerZ = Math.floor((region.minZ + region.maxZ) / 2.0D);
		return new BlockPos(
			(int) centerX - (STRUCTURE_WIDTH / 2),
			(int) Math.floor(region.minY),
			(int) centerZ - (STRUCTURE_DEPTH / 2)
		);
	}

	public void reset() {
		placedLanes.clear();
	}

	public interface StructurePlacer {
		boolean place(MinecraftServer server, String worldId, String structureId, BlockPos originPos);
	}

	private static class WorldStructurePlacer implements StructurePlacer {
		@Override
		public boolean place(MinecraftServer server, String worldId, String structureId, BlockPos originPos) {
			if (server == null) {
				return false;
			}
			var world = resolveWorld(server, worldId);
			if (world == null) {
				return false;
			}
			var identifier = Identifier.tryParse(structureId);
			if (identifier == null) {
				return false;
			}
			var template = server.getStructureTemplateManager().getTemplate(identifier).orElse(null);
			if (template == null) {
				return false;
			}
			return template.place(world, originPos, originPos, new StructurePlacementData(), world.getRandom(), Block.NOTIFY_LISTENERS);
		}

		private ServerWorld resolveWorld(MinecraftServer server, String worldId) {
			try {
				var worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
				var world = server.getWorld(worldKey);
				return world != null ? world : server.getOverworld();
			} catch (Exception ignored) {
				return server.getOverworld();
			}
		}
	}
}
