package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;

public class BossBarService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final ServerBossBar bossBar;
	private MatchPhase lastRenderedPhase = null;
	private String lastRenderedText = "";

	public BossBarService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.bossBar = new ServerBossBar(textTemplateResolver.format("&fMCsnap"), BossBar.Color.WHITE, BossBar.Style.PROGRESS);
		this.bossBar.setVisible(false);
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			if (lastRenderedPhase == MatchPhase.GAME_RUNNING) {
				bossBar.clearPlayers();
				bossBar.setVisible(false);
				lastRenderedText = "";
			}
			lastRenderedPhase = matchManager.getPhase();
			return;
		}

		var bossBarConfig = systemConfig.bossBar;
		var rendered = BossBarFormatter.format(
			bossBarConfig.template,
			matchManager.getRemainingSeconds(),
			matchManager.getRedScore(),
			matchManager.getBlueScore()
		);

		if (!rendered.equals(lastRenderedText) || lastRenderedPhase != MatchPhase.GAME_RUNNING) {
			bossBar.setName(textTemplateResolver.format(rendered));
			lastRenderedText = rendered;
		}

		bossBar.setPercent(BossBarFormatter.percent(matchManager.getRemainingSeconds(), matchManager.getTotalSeconds()));
		bossBar.setColor(parseColor(bossBarConfig.color));
		bossBar.setStyle(parseStyle(bossBarConfig.style));
		bossBar.setVisible(true);

		for (var player : server.getPlayerManager().getPlayerList()) {
			if (!bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			}
		}

		lastRenderedPhase = MatchPhase.GAME_RUNNING;
	}

	private BossBar.Color parseColor(String color) {
		try {
			return BossBar.Color.valueOf(color.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return BossBar.Color.WHITE;
		}
	}

	private BossBar.Style parseStyle(String style) {
		try {
			return BossBar.Style.valueOf(style.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return BossBar.Style.PROGRESS;
		}
	}
}
