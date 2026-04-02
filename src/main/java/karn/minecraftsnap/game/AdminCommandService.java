package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * 관리자 전용 명령 실행 로직을 담당하는 서비스.
 */
public class AdminCommandService {

	public String spawnBots(ServerCommandSource source, int count, TextConfigFile textConfig) {
		if (source == null) {
			return textConfig.commandSourceNotFoundMessage;
		}
		if (count <= 0) {
			return textConfig.adminBotsInvalidCountMessage;
		}
		var server = source.getServer();
		var commandManager = server.getCommandManager();
		int created = 0;
		int nextIndex = 1;
		while (created < count && nextIndex <= count + 256) {
			var name = "BOT" + nextIndex;
			nextIndex++;
			if (server.getPlayerManager().getPlayer(name) != null) {
				continue;
			}
			commandManager.executeWithPrefix(source, "player " + name + " spawn");
			created++;
		}
		return created == count
			? textConfig.adminBotsSuccessMessage.replace("{count}", Integer.toString(created))
			: textConfig.adminBotsPartialMessage
				.replace("{created}", Integer.toString(created))
				.replace("{requested}", Integer.toString(count));
	}

	public String forceAssignUnit(
		ServerPlayerEntity player,
		String unitId,
		MatchManager matchManager,
		UnitRegistry unitRegistry,
		UnitHookService unitHookService,
		SystemConfig systemConfig,
		TextConfigFile textConfig
	) {
		if (player == null) {
			return textConfig.commandPlayerNotFoundMessage;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			return textConfig.unitSpawnUnknownUnitMessage;
		}

		matchManager.setRole(player, state.getTeamId(), RoleType.UNIT);
		if (player.isSpectator()) {
			player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
		}
		unitHookService.assignUnit(player, definition, systemConfig);
		return textConfig.commandForceUnitSuccessMessage.replace("{unit}", definition.displayName());
	}

	public String placeNearestBiomeStructure(
		ServerPlayerEntity player,
		String structureId,
		MatchManager matchManager,
		SystemConfig systemConfig,
		karn.minecraftsnap.config.BiomeCatalog biomeCatalog,
		LaneStructureService laneStructureService,
		TextConfigFile textConfig
	) {
		if (player == null) {
			return textConfig.commandPlayerNotFoundMessage;
		}
		var laneId = UnitSpawnService.nearestLaneForCaptain(player, systemConfig);
		var resolvedStructureId = structureId;
		if (resolvedStructureId == null || resolvedStructureId.isBlank()) {
			var assignedBiomeId = matchManager.getAssignedBiomeId(laneId);
			if (assignedBiomeId == null || assignedBiomeId.isBlank()) {
				return textConfig.commandPlaceNearestNoBiomeMessage.replace("{lane}", laneLabel(laneId));
			}
			var biomeEntry = biomeCatalog.biomes.stream()
				.filter(entry -> assignedBiomeId.equals(entry.id))
				.findFirst()
				.orElse(null);
			if (biomeEntry == null || biomeEntry.structureId == null || biomeEntry.structureId.isBlank()) {
				return textConfig.commandPlaceNearestMissingStructureMessage.replace("{lane}", laneLabel(laneId));
			}
			resolvedStructureId = biomeEntry.structureId;
		}
		var server = matchManager.getServer();
		var placed = laneStructureService.forcePlaceStructure(
			server,
			systemConfig.world,
			laneId,
			resolvedStructureId,
			laneStructureService.originFor(laneRegionOf(laneId, systemConfig))
		);
		return placed
			? textConfig.commandPlaceNearestSuccessMessage
				.replace("{lane}", laneLabel(laneId))
				.replace("{structure}", resolvedStructureId)
			: textConfig.commandPlaceNearestFailureMessage.replace("{lane}", laneLabel(laneId));
	}

	public String resetAllBiomeStructures(
		MinecraftServer server,
		SystemConfig systemConfig,
		LaneStructureService laneStructureService,
		TextConfigFile textConfig
	) {
		if (server == null) {
			return textConfig.commandServerNotBoundMessage;
		}
		int success = 0;
		for (var laneId : LaneId.values()) {
			if (laneStructureService.forcePlaceStructure(
				server,
				systemConfig.world,
				laneId,
				"minecraft:default",
				laneStructureService.originFor(laneRegionOf(laneId, systemConfig))
			)) {
				success++;
			}
		}
		laneStructureService.reset();
		return success == LaneId.values().length
			? textConfig.commandResetStructuresSuccessMessage
			: textConfig.commandResetStructuresPartialMessage
				.replace("{success}", Integer.toString(success))
				.replace("{total}", Integer.toString(LaneId.values().length));
	}

	public String openAdminGui(
		ServerPlayerEntity player,
		String guiId,
		MatchManager matchManager,
		TextConfigFile textConfig,
		Consumer<ServerPlayerEntity> mainOpener,
		Consumer<ServerPlayerEntity> wikiOpener,
		Consumer<ServerPlayerEntity> unitIndexOpener,
		Consumer<ServerPlayerEntity> biomeIndexOpener,
		Consumer<ServerPlayerEntity> adminToolsOpener,
		BiConsumer<ServerPlayerEntity, Consumer<FactionId>> factionSelectionOpener,
		Consumer<ServerPlayerEntity> preparationOpener,
		Consumer<ServerPlayerEntity> captainSpawnOpener,
		Consumer<ServerPlayerEntity> tradeOpener,
		Consumer<ServerPlayerEntity> advanceOpener,
		BiConsumer<TeamId, FactionId> factionSetter
	) {
		var state = matchManager.getPlayerState(player.getUuid());
		return switch (guiId) {
			case "wiki" -> {
				mainOpener.accept(player);
				yield textConfig.commandOpenMainSuccessMessage;
			}
			case "rules" -> {
				wikiOpener.accept(player);
				yield textConfig.commandOpenWikiSuccessMessage;
			}
			case "unit_index" -> {
				unitIndexOpener.accept(player);
				yield textConfig.commandOpenUnitIndexSuccessMessage;
			}
			case "biome_index" -> {
				biomeIndexOpener.accept(player);
				yield textConfig.commandOpenBiomeIndexSuccessMessage;
			}
			case "admin_tools" -> {
				adminToolsOpener.accept(player);
				yield textConfig.commandOpenAdminToolsSuccessMessage;
			}
			case "faction" -> {
				factionSelectionOpener.accept(player, factionId -> {
					if (state.getTeamId() != null) {
						factionSetter.accept(state.getTeamId(), factionId);
					}
				});
				yield textConfig.commandOpenFactionSuccessMessage;
			}
			case "preparation" -> {
				preparationOpener.accept(player);
				yield textConfig.commandOpenPreparationSuccessMessage;
			}
			case "captain_spawn" -> {
				captainSpawnOpener.accept(player);
				yield textConfig.commandOpenCaptainSpawnSuccessMessage;
			}
			case "trade" -> {
				tradeOpener.accept(player);
				yield textConfig.commandOpenTradeSuccessMessage;
			}
			case "advance" -> {
				advanceOpener.accept(player);
				yield textConfig.commandOpenAdvanceSuccessMessage;
			}
			default -> textConfig.commandUnsupportedGuiMessage;
		};
	}

	private SystemConfig.LaneRegionConfig laneRegionOf(LaneId laneId, SystemConfig systemConfig) {
		return switch (laneId) {
			case LANE_1 -> systemConfig.inGame.lane1Region;
			case LANE_2 -> systemConfig.inGame.lane2Region;
			case LANE_3 -> systemConfig.inGame.lane3Region;
		};
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}
}
