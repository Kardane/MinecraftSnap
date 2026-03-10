package karn.minecraftsnap.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class McSnapCommandRegistrar {
	private final MinecraftSnap mod;

	public McSnapCommandRegistrar(MinecraftSnap mod) {
		this.mod = mod;
	}

	public void register(net.minecraft.command.CommandRegistryAccess registryAccess, net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {
	}

	public void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher, net.minecraft.command.CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("mcsnap")
			.executes(ctx -> send(ctx.getSource(), "&fMCsnap 명령 트리 준비 완료"))
			.then(CommandManager.literal("reload")
				.executes(ctx -> {
					mod.reload();
					return send(ctx.getSource(), "&aMCsnap 컨픽 리로드 완료");
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
				.then(CommandManager.literal("unit").executes(ctx -> setPreference(ctx.getSource(), "unit")))
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
					.then(CommandManager.literal("game_running").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_RUNNING)))
					.then(CommandManager.literal("game_end").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_END))))
				.then(CommandManager.literal("teamsel").executes(ctx -> startTeamSelection(ctx.getSource())))
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
				.then(CommandManager.literal("opengui")
					.then(CommandManager.argument("gui", StringArgumentType.word())
						.executes(ctx -> send(ctx.getSource(), "&eGUI 열기 요청: &f" + StringArgumentType.getString(ctx, "gui")))))
				.then(CommandManager.literal("manacharge").executes(ctx -> send(ctx.getSource(), "&e마나 충전 로직은 다음 단계에서 연결 예정")))
				.then(CommandManager.literal("advance").executes(ctx -> send(ctx.getSource(), "&e전직 강제 로직은 다음 단계에서 연결 예정")))
				.then(CommandManager.literal("captainskill").executes(ctx -> send(ctx.getSource(), "&e사령관 스킬 강제 사용 로직은 다음 단계에서 연결 예정"))))
		);
	}

	private int showStat(ServerCommandSource source, ServerPlayerEntity player) {
		var stats = mod.getStatsRepository().getOrCreate(player.getUuid(), player.getName().getString());
		send(source, "&f플레이어: &e" + stats.lastKnownName);
		send(source, "&f래더: &b" + stats.ladder + " &8/ &f선호: &d" + stats.preference);
		send(source, "&f킬: &a" + stats.kills + " &8/ &f데스: &c" + stats.deaths + " &8/ &f점령: &6" + stats.captures);
		return Command.SINGLE_SUCCESS;
	}

	private int setPreference(ServerCommandSource source, String preference) {
		var player = source.getPlayer();
		mod.getStatsRepository().setPreference(player.getUuid(), player.getName().getString(), preference);
		return send(source, "&a선호 직업 갱신: &f" + preference);
	}

	private int assignCaptain(ServerCommandSource source, TeamId teamId, ServerPlayerEntity target) {
		mod.getMatchManager().setCaptain(teamId, target);
		return send(source, "&a" + target.getName().getString() + " 님을 " + teamId.getDisplayName() + " 사령관으로 지정 완료");
	}

	private int startTeamSelection(ServerCommandSource source) {
		mod.startTeamSelection();
		return send(source, "&a팀 자동 배정과 사령관 선출 시작");
	}

	private int forceStartGame(ServerCommandSource source) {
		mod.forceStartGame();
		return send(source, "&a기본값 보정 후 게임 강제 시작");
	}

	private int setPhase(ServerCommandSource source, MatchPhase phase) {
		mod.getMatchManager().setPhase(phase);
		if (phase == MatchPhase.LOBBY) {
			mod.getCapturePointService().resetAll();
		}
		return send(source, "&a현재 페이즈: &f" + phase.getDisplayName());
	}

	private int setLaneState(ServerCommandSource source, LaneId laneId, boolean active) {
		if (active) {
			mod.getMatchManager().activateLane(laneId);
		} else {
			mod.getMatchManager().deactivateLane(laneId);
		}
		return send(source, "&a" + laneLabel(laneId) + " 상태: &f" + (active ? "활성" : "비활성"));
	}

	private int send(ServerCommandSource source, String message) {
		source.sendFeedback(() -> mod.getTextTemplateResolver().format(message), false);
		return Command.SINGLE_SUCCESS;
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}
}
