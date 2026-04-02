package karn.minecraftsnap.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.CompletableFuture;

public class McSnapCommandRegistrar {
	static final java.util.List<String> ADMIN_GUI_IDS = java.util.List.of("wiki", "rules", "unit_index", "biome_index", "admin_tools", "faction", "preparation", "captain_spawn", "trade", "advance");
	private final MinecraftSnap mod;

	public McSnapCommandRegistrar(MinecraftSnap mod) {
		this.mod = mod;
	}

	public void register(net.minecraft.command.CommandRegistryAccess registryAccess, net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {
	}

	public void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("mcsnap")
			.executes(ctx -> send(ctx.getSource(), textConfig().commandRootReadyMessage))
			.then(CommandManager.literal("reload")
				.executes(ctx -> {
					mod.reload();
					return send(ctx.getSource(), textConfig().commandReloadMessage);
				}))
			.then(CommandManager.literal("wiki")
				.requires(ServerCommandSource::isExecutedByPlayer)
				.executes(ctx -> {
					mod.openWiki(ctx.getSource().getPlayer());
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(CommandManager.literal("stat")
				.executes(ctx -> showStat(ctx.getSource(), ctx.getSource().getPlayer()))
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(ctx -> showStat(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
			.then(CommandManager.literal("prefer")
				.then(CommandManager.literal("captain").executes(ctx -> setPreference(ctx.getSource(), "captain")))
				.then(CommandManager.literal("role")
					.then(CommandManager.argument("unit", StringArgumentType.word())
						.suggests(this::suggestSpawnableUnitIds)
						.executes(ctx -> setPreference(ctx.getSource(), "unit:" + StringArgumentType.getString(ctx, "unit")))))
				.then(CommandManager.literal("none").executes(ctx -> setPreference(ctx.getSource(), "none"))))
			.then(CommandManager.literal("captain_red")
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(ctx -> assignCaptain(ctx.getSource(), TeamId.RED, EntityArgumentType.getPlayer(ctx, "player")))))
			.then(CommandManager.literal("captain_blue")
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(ctx -> assignCaptain(ctx.getSource(), TeamId.BLUE, EntityArgumentType.getPlayer(ctx, "player")))))
			.then(CommandManager.literal("admin")
				.requires(source -> source.hasPermissionLevel(4))
				.then(CommandManager.literal("phase")
					.then(CommandManager.literal("lobby").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.LOBBY)))
					.then(CommandManager.literal("team_select").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.TEAM_SELECT)))
					.then(CommandManager.literal("faction_select").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.FACTION_SELECT)))
					.then(CommandManager.literal("game_start").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_START)))
					.then(CommandManager.literal("game_running").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_RUNNING)))
					.then(CommandManager.literal("game_end").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_END))))
				.then(CommandManager.literal("teamsel").executes(ctx -> startTeamSelection(ctx.getSource())))
				.then(CommandManager.literal("clearteams").executes(ctx -> clearTeams(ctx.getSource())))
				.then(CommandManager.literal("bots")
					.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
						.executes(ctx -> spawnBots(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))))
				.then(CommandManager.literal("time")
					.then(CommandManager.argument("ticks", IntegerArgumentType.integer())
						.executes(ctx -> adjustTime(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks")))))
				.then(CommandManager.literal("autostart").executes(ctx -> toggleAutoStart(ctx.getSource())))
				.then(CommandManager.literal("gamestart").executes(ctx -> forceStartGame(ctx.getSource())))
				.then(CommandManager.literal("gamestop").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_END)))
				.then(CommandManager.literal("biomeshow")
					.then(CommandManager.literal("lane1").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_1, true)))
					.then(CommandManager.literal("lane2").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_2, true)))
					.then(CommandManager.literal("lane3").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_3, true))))
				.then(CommandManager.literal("biomehide")
					.then(CommandManager.literal("lane1").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_1, false)))
					.then(CommandManager.literal("lane2").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_2, false)))
					.then(CommandManager.literal("lane3").executes(ctx -> setLaneState(ctx.getSource(), LaneId.LANE_3, false))))
				.then(CommandManager.literal("biomestructure")
					.then(CommandManager.literal("place_nearest")
						.executes(ctx -> placeNearestBiomeStructure(ctx.getSource(), null))
						.then(CommandManager.argument("structure_id", StringArgumentType.greedyString())
							.executes(ctx -> placeNearestBiomeStructure(ctx.getSource(), StringArgumentType.getString(ctx, "structure_id")))))
					.then(CommandManager.literal("reset_all").executes(ctx -> resetAllBiomeStructures(ctx.getSource()))))
				.then(registerOpenGuiTree())
				.then(CommandManager.literal("manacharge")
					.executes(ctx -> chargeMana(ctx.getSource(), ctx.getSource().getPlayer()))
					.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(ctx -> chargeMana(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
				.then(CommandManager.literal("advance")
					.executes(ctx -> advance(ctx.getSource(), ctx.getSource().getPlayer()))
					.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(ctx -> advance(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
				.then(CommandManager.literal("unit")
					.then(CommandManager.argument("player", EntityArgumentType.player())
						.then(CommandManager.argument("unit", StringArgumentType.word())
							.suggests(this::suggestAllUnitIds)
							.executes(ctx -> forceUnit(
								ctx.getSource(),
								EntityArgumentType.getPlayer(ctx, "player"),
								StringArgumentType.getString(ctx, "unit")
							)))))
				.then(CommandManager.literal("captainskill")
					.then(CommandManager.literal("villager").executes(ctx -> triggerCaptainSkill(ctx.getSource(), FactionId.VILLAGER)))
					.then(CommandManager.literal("monster").executes(ctx -> triggerCaptainSkill(ctx.getSource(), FactionId.MONSTER)))
					.then(CommandManager.literal("nether").executes(ctx -> triggerCaptainSkill(ctx.getSource(), FactionId.NETHER)))))
		);
		registerSurrenderAlias(dispatcher, "항복");
		registerSurrenderAlias(dispatcher, "gg");
		registerSurrenderAlias(dispatcher, "ㅈㅈ");
		registerSurrenderAlias(dispatcher, "ww");
		dispatcher.register(CommandManager.literal("중참")
			.requires(ServerCommandSource::isExecutedByPlayer)
			.executes(ctx -> send(ctx.getSource(), mod.joinOngoingMatch(ctx.getSource().getPlayer()))));
	}

	private int showStat(ServerCommandSource source, ServerPlayerEntity player) {
		var stats = mod.getStatsRepository().getOrCreate(player.getUuid(), player.getName().getString());
		send(source, textConfig().commandStatPlayerTemplate.replace("{player}", stats.lastKnownName));
		send(source, textConfig().commandStatLadderTemplate
			.replace("{ladder}", Integer.toString(stats.ladder))
			.replace("{preference}", stats.preference));
		send(source, textConfig().commandStatCombatTemplate
			.replace("{kills}", Integer.toString(stats.kills))
			.replace("{deaths}", Integer.toString(stats.deaths))
			.replace("{captures}", Integer.toString(stats.captures)));
		send(source, textConfig().commandStatCurrencyTemplate
			.replace("{emeralds}", Integer.toString(stats.emeralds))
			.replace("{gold}", Integer.toString(stats.goldIngots)));
		return Command.SINGLE_SUCCESS;
	}

	private int setPreference(ServerCommandSource source, String preference) {
		var player = source.getPlayer();
		mod.getStatsRepository().setPreference(player.getUuid(), player.getName().getString(), preference);
		return send(source, textConfig().commandPreferenceUpdatedMessage.replace("{preference}", preference));
	}

	private CompletableFuture<Suggestions> suggestSpawnableUnitIds(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		if (mod == null) {
			return builder.buildFuture();
		}
		for (var unit : mod.getUnitRegistry().all()) {
			if (unit.captainSpawnable()) {
				builder.suggest(unit.id());
			}
		}
		return builder.buildFuture();
	}

	private CompletableFuture<Suggestions> suggestAllUnitIds(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
		if (mod == null) {
			return builder.buildFuture();
		}
		for (var unit : mod.getUnitRegistry().all()) {
			builder.suggest(unit.id());
		}
		return builder.buildFuture();
	}

	private int assignCaptain(ServerCommandSource source, TeamId teamId, ServerPlayerEntity target) {
		mod.getMatchManager().setCaptain(teamId, target);
		return send(source, textConfig().commandAssignCaptainMessage
			.replace("{player}", target.getName().getString())
			.replace("{team}", teamId.getDisplayName()));
	}

	private int startTeamSelection(ServerCommandSource source) {
		mod.assignTeamsOnly();
		return send(source, textConfig().commandTeamSelectionMessage);
	}

	private int clearTeams(ServerCommandSource source) {
		mod.clearAllTeams();
		return send(source, textConfig().commandClearTeamsMessage);
	}

	private int spawnBots(ServerCommandSource source, int count) {
		return send(source, mod.spawnBots(source, count));
	}

	private int adjustTime(ServerCommandSource source, int ticks) {
		return send(source, mod.adjustMatchTime(ticks));
	}

	private int forceStartGame(ServerCommandSource source) {
		mod.forceStartGame();
		return send(source, textConfig().commandForceStartMessage);
	}

	private int toggleAutoStart(ServerCommandSource source) {
		var enabled = mod.toggleAutoStart();
		var message = enabled
			? mod.getSystemConfig().announcements.autoStartEnabledMessage
			: mod.getSystemConfig().announcements.autoStartDisabledMessage;
		return send(source, message);
	}

	private int setPhase(ServerCommandSource source, MatchPhase phase) {
		mod.forcePhase(phase);
		if (phase == MatchPhase.LOBBY) {
			mod.getCapturePointService().resetAll();
		}
		return send(source, textConfig().commandPhaseChangedMessage.replace("{phase}", phase.getDisplayName()));
	}

	private int setLaneState(ServerCommandSource source, LaneId laneId, boolean active) {
		mod.setLaneRevealState(laneId, active);
		return send(source, textConfig().commandLaneStateMessage
			.replace("{lane}", laneLabel(laneId))
			.replace("{state}", active ? "공개" : "비공개"));
	}

	private int placeNearestBiomeStructure(ServerCommandSource source, String structureId) {
		var player = source.getPlayer();
		if (player == null) {
			return send(source, textConfig().commandPlaceNearestPlayerOnlyMessage);
		}
		return send(source, mod.placeNearestBiomeStructure(player, structureId));
	}

	private int resetAllBiomeStructures(ServerCommandSource source) {
		return send(source, mod.resetAllBiomeStructures());
	}

	private int chargeMana(ServerCommandSource source, ServerPlayerEntity player) {
		if (player == null || !mod.chargeCaptainMana(player)) {
			return send(source, textConfig().commandChargeManaFailedMessage);
		}
		return send(source, textConfig().commandChargeManaSuccessMessage.replace("{player}", player.getName().getString()));
	}

	private int triggerCaptainSkill(ServerCommandSource source, FactionId factionId) {
		if (!mod.triggerCaptainSkill(factionId)) {
			return send(source, textConfig().commandCaptainSkillFailedMessage);
		}
		return send(source, textConfig().commandCaptainSkillSuccessMessage.replace("{faction}", factionId.name().toLowerCase()));
	}

	private int advance(ServerCommandSource source, ServerPlayerEntity player) {
		if (!mod.forceAdvance(player)) {
			return send(source, textConfig().commandAdvanceFailedMessage);
		}
		return send(source, textConfig().commandAdvanceSuccessMessage.replace("{player}", player.getName().getString()));
	}

	private int forceUnit(ServerCommandSource source, ServerPlayerEntity player, String unitId) {
		var result = mod.forceAssignUnit(player, unitId);
		if (player != null && result.startsWith("&a")) {
			player.sendMessage(mod.getTextTemplateResolver().format(mod.getTextConfig().adminForceUnitMessage.replace("{unit}", unitId)), false);
		}
		return send(source, result);
	}

	private int openGui(ServerCommandSource source, String guiId) {
		var player = source.getPlayer();
		if (player == null) {
			return send(source, textConfig().commandOpenGuiPlayerOnlyMessage);
		}
		var result = mod.openAdminGui(player, guiId);
		return send(source, result);
	}

	private int send(ServerCommandSource source, String message) {
		source.sendFeedback(() -> mod.getTextTemplateResolver().format(message), false);
		return Command.SINGLE_SUCCESS;
	}

	private void registerSurrenderAlias(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher, String literal) {
		dispatcher.register(CommandManager.literal(literal)
			.requires(ServerCommandSource::isExecutedByPlayer)
			.executes(ctx -> send(ctx.getSource(), mod.voteSurrender(ctx.getSource().getPlayer()))));
	}

	private com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> registerOpenGuiTree() {
		var builder = CommandManager.literal("opengui");
		for (var guiId : ADMIN_GUI_IDS) {
			builder.then(CommandManager.literal(guiId).executes(ctx -> openGui(ctx.getSource(), guiId)));
		}
		return builder;
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}

	private TextConfigFile textConfig() {
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
