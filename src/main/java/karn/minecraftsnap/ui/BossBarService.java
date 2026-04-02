package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.MinecraftSnap;
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
		this.bossBar = new ServerBossBar(textTemplateResolver.format(textConfig().bossBarTitle), BossBar.Color.WHITE, BossBar.Style.PROGRESS);
		this.bossBar.setVisible(false);
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING && matchManager.getPhase() != MatchPhase.FACTION_SELECT) {
			if (lastRenderedPhase == MatchPhase.GAME_RUNNING || lastRenderedPhase == MatchPhase.FACTION_SELECT) {
				bossBar.clearPlayers();
				bossBar.setVisible(false);
				lastRenderedText = "";
			}
			lastRenderedPhase = matchManager.getPhase();
			return;
		}

		if (matchManager.getPhase() == MatchPhase.FACTION_SELECT) {
			var rendered = factionSelectText(
				factionSelectRemainingSeconds(matchManager.getPhaseTicks(), systemConfig.lobby.factionSelectDurationSeconds),
				systemConfig.lobby.factionSelectBossBarTemplate
			);
			if (!rendered.equals(lastRenderedText) || lastRenderedPhase != MatchPhase.FACTION_SELECT) {
				bossBar.setName(textTemplateResolver.format(rendered));
				lastRenderedText = rendered;
			}
			bossBar.setPercent(factionSelectPercent(matchManager.getPhaseTicks(), systemConfig.lobby.factionSelectDurationSeconds));
			bossBar.setColor(BossBar.Color.YELLOW);
			bossBar.setStyle(BossBar.Style.PROGRESS);
			bossBar.setVisible(true);
			for (var player : server.getPlayerManager().getPlayerList()) {
				if (!bossBar.getPlayers().contains(player)) {
					bossBar.addPlayer(player);
				}
			}
			lastRenderedPhase = MatchPhase.FACTION_SELECT;
			return;
		}

		var bossBarConfig = systemConfig.bossBar;
		var rendered = BossBarFormatter.format(
			bossBarConfig.template,
			matchManager.getRemainingSeconds(),
			matchManager.getRedScore(),
			matchManager.getBlueScore(),
			resolveNextRevealTime(systemConfig)
		);

		if (!rendered.equals(lastRenderedText) || lastRenderedPhase != MatchPhase.GAME_RUNNING) {
			bossBar.setName(textTemplateResolver.format(rendered));
			lastRenderedText = rendered;
		}

		bossBar.setPercent(BossBarFormatter.percentTicks(matchManager.getRemainingTicks(), matchManager.getTotalTicks()));
		bossBar.setColor(gameRunningColor(matchManager.getRedScore(), matchManager.getBlueScore()));
		bossBar.setStyle(parseStyle(bossBarConfig.style));
		bossBar.setVisible(true);

		for (var player : server.getPlayerManager().getPlayerList()) {
			if (!bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			}
		}

		lastRenderedPhase = MatchPhase.GAME_RUNNING;
	}

	private String resolveNextRevealTime(SystemConfig systemConfig) {
		var mod = MinecraftSnap.getInstance();
		if (mod == null || mod.getBiomeRevealService() == null) {
			return "--:--";
		}
		return mod.getBiomeRevealService().nextRevealRemainingTime(systemConfig);
	}

	static String factionSelectText(int remainingSeconds, String template) {
		return template.replace("{time}", BossBarFormatter.formatTime(Math.max(remainingSeconds, 0)));
	}

	static int factionSelectRemainingSeconds(long phaseTicks, int durationSeconds) {
		return Math.max(0, durationSeconds - (int) (phaseTicks / 20L));
	}

	static float factionSelectPercent(long phaseTicks, int durationSeconds) {
		if (durationSeconds <= 0) {
			return 0.0f;
		}
		var remaining = factionSelectRemainingSeconds(phaseTicks, durationSeconds);
		return Math.max(0.0f, Math.min(1.0f, remaining / (float) durationSeconds));
	}

	static BossBar.Color gameRunningColor(int redScore, int blueScore) {
		if (redScore > blueScore) {
			return BossBar.Color.RED;
		}
		if (blueScore > redScore) {
			return BossBar.Color.BLUE;
		}
		return BossBar.Color.YELLOW;
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

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
