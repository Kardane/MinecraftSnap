package karn.minecraftsnap.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.PlayerStats;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
				.executes(ctx -> send(ctx.getSource(), "&e위키 GUI는 다음 단계에서 연결 예정"))
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
				.requires(ServerCommandSource::isExecutedByPlayer)
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(ctx -> assignCaptain(ctx.getSource(), TeamId.RED, EntityArgumentType.getPlayer(ctx, "player")))))
			.then(CommandManager.literal("captain_blue")
				.requires(ServerCommandSource::isExecutedByPlayer)
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
				.then(CommandManager.literal("teamsel").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.TEAM_SELECT)))
				.then(CommandManager.literal("gamestart").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_RUNNING)))
				.then(CommandManager.literal("gamestop").executes(ctx -> setPhase(ctx.getSource(), MatchPhase.GAME_END)))
				.then(CommandManager.literal("biomeshow").executes(ctx -> send(ctx.getSource(), "&e바이옴 공개 로직은 다음 단계에서 연결 예정")))
				.then(CommandManager.literal("biomehide").executes(ctx -> send(ctx.getSource(), "&e바이옴 비공개 로직은 다음 단계에서 연결 예정")))
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

	private int setPhase(ServerCommandSource source, MatchPhase phase) {
		mod.getMatchManager().setPhase(phase);
		return send(source, "&a현재 페이즈: &f" + phase.getDisplayName());
	}

	private int send(ServerCommandSource source, String message) {
		source.sendFeedback(() -> mod.getTextTemplateResolver().format(message), false);
		return Command.SINGLE_SUCCESS;
	}
}
