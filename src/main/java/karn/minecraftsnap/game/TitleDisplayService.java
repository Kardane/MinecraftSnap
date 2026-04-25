package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 타이틀/서브타이틀 패킷 전송 서비스.
 * MinecraftSnap에서 추출된 표시 로직.
 */
public class TitleDisplayService {
	private static final int VICTORY_COUNTDOWN_STAY_TICKS = 25;
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;

	public TitleDisplayService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
	}

	public void showGameTitle(MinecraftServer server, String message) {
		showGameTitle(server, message, "");
	}

	public void showGameTitle(MinecraftServer server, String message, String subtitleMessage) {
		if (server == null) {
			return;
		}
		var title = textTemplateResolver.format(message);
		var subtitle = subtitleMessage == null || subtitleMessage.isBlank()
			? Text.empty()
			: textTemplateResolver.format(subtitleMessage);
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(
				GameEndService.TITLE_FADE_IN_TICKS,
				GameEndService.TITLE_STAY_TICKS,
				GameEndService.TITLE_FADE_OUT_TICKS
			));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}

	public void showGameStartCountdown(MinecraftServer server, SystemConfig systemConfig, int remainingSeconds) {
		if (server == null) {
			return;
		}
		var title = textTemplateResolver.format(systemConfig.gameStart.countdownTitle.replace("{seconds}", String.valueOf(remainingSeconds)));
		var subtitle = textTemplateResolver.format(systemConfig.gameStart.countdownSubtitleTemplate
			.replace("{seconds}", String.valueOf(remainingSeconds))
			.replace("{red_captain}", captainName(server, TeamId.RED))
			.replace("{red_faction}", factionName(TeamId.RED, systemConfig))
			.replace("{blue_faction}", factionName(TeamId.BLUE, systemConfig))
			.replace("{blue_captain}", captainName(server, TeamId.BLUE)));
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 0));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}

	public void showVictoryCountdown(MinecraftServer server, SystemConfig systemConfig, TeamId teamId, int remainingSeconds) {
		if (server == null || teamId == null || remainingSeconds <= 0) {
			return;
		}
		var template = systemConfig.gameEnd.victoryCountdownSubtitleTemplate;
		var subtitle = textTemplateResolver.format(template
			.replace("{team}", teamId.getDisplayName())
			.replace("{seconds}", String.valueOf(remainingSeconds)));
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, VICTORY_COUNTDOWN_STAY_TICKS, 0));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.empty()));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}

	public void showCaptainBiomeRevealWarning(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null) {
			return;
		}
		var title = textTemplateResolver.format(systemConfig.biomeReveal.captainWarningTitle);
		var subtitle = textTemplateResolver.format(systemConfig.biomeReveal.captainWarningSubtitle);
		for (var teamId : TeamId.values()) {
			var captainId = matchManager == null ? null : matchManager.getCaptainId(teamId);
			if (captainId == null) {
				continue;
			}
			var player = server.getPlayerManager().getPlayer(captainId);
			if (player == null) {
				continue;
			}
			player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 40, 10));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}

	public void showPersonalTitle(ServerPlayerEntity player, String titleMessage, String subtitleMessage) {
		if (player == null) {
			return;
		}
		var title = textTemplateResolver.format(titleMessage);
		var subtitle = textTemplateResolver.format(subtitleMessage);
		player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 40, 10));
		player.networkHandler.sendPacket(new TitleS2CPacket(title));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
	}

	private String captainName(MinecraftServer server, TeamId teamId) {
		if (server == null || teamId == null || matchManager == null) {
			return "미정";
		}
		var captainId = matchManager.getCaptainId(teamId);
		if (captainId == null) {
			return "미정";
		}
		var player = server.getPlayerManager().getPlayer(captainId);
		return player == null ? "미정" : player.getName().getString();
	}

	private String factionName(TeamId teamId, SystemConfig systemConfig) {
		if (teamId == null || systemConfig == null || matchManager == null) {
			return "미정";
		}
		var factionId = matchManager.getFactionSelection(teamId);
		if (factionId == null) {
			return "미정";
		}
		return switch (factionId) {
			case VILLAGER -> systemConfig.announcements.villagerFactionName;
			case MONSTER -> systemConfig.announcements.monsterFactionName;
			case NETHER -> systemConfig.announcements.netherFactionName;
		};
	}
}
