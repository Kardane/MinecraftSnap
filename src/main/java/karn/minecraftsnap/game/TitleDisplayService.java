package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

/**
 * 타이틀/서브타이틀 패킷 전송 서비스.
 * MinecraftSnap에서 추출된 표시 로직.
 */
public class TitleDisplayService {
	private final TextTemplateResolver textTemplateResolver;

	public TitleDisplayService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void showGameTitle(MinecraftServer server, String message) {
		if (server == null) {
			return;
		}
		var title = textTemplateResolver.format(message);
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(
				GameEndService.TITLE_FADE_IN_TICKS,
				GameEndService.TITLE_STAY_TICKS,
				GameEndService.TITLE_FADE_OUT_TICKS
			));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.empty()));
		}
	}

	public void showGameStartCountdown(MinecraftServer server, SystemConfig systemConfig, int remainingSeconds) {
		if (server == null) {
			return;
		}
		var title = textTemplateResolver.format(systemConfig.gameStart.countdownTitle);
		var subtitle = textTemplateResolver.format(systemConfig.gameStart.countdownSubtitleTemplate.replace("{seconds}", String.valueOf(remainingSeconds)));
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
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}
}
