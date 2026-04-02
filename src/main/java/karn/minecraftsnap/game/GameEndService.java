package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class GameEndService {
	public static final int TITLE_FADE_IN_TICKS = 0;
	public static final int TITLE_STAY_TICKS = 30;
	public static final int TITLE_FADE_OUT_TICKS = 0;
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Consumer<String> titleSender;
	private final BiConsumer<TeamId, Integer> winnerGlowApplier;
	private final Runnable playerAttributeResetter;
	private final Runnable glowClearer;
	private final IntConsumer tickRateController;
	private final Runnable rewardAction;
	private final Runnable lobbyReturnAction;
	private final Runnable biomeRestoreAction;
	private boolean active;

	public GameEndService(
		MatchManager matchManager,
		TextTemplateResolver textTemplateResolver,
		Consumer<String> titleSender,
		BiConsumer<TeamId, Integer> winnerGlowApplier,
		Runnable playerAttributeResetter,
		Runnable glowClearer,
		IntConsumer tickRateController,
		Runnable rewardAction,
		Runnable lobbyReturnAction,
		Runnable biomeRestoreAction
	) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
		this.titleSender = titleSender;
		this.winnerGlowApplier = winnerGlowApplier;
		this.playerAttributeResetter = playerAttributeResetter;
		this.glowClearer = glowClearer;
		this.tickRateController = tickRateController;
		this.rewardAction = rewardAction;
		this.lobbyReturnAction = lobbyReturnAction;
		this.biomeRestoreAction = biomeRestoreAction;
	}

	public void tick(SystemConfig systemConfig) {
		if (matchManager.getPhase() != MatchPhase.GAME_END) {
			active = false;
			return;
		}

		if (!active) {
			active = true;
			announce(systemConfig.gameEnd);
			rewardAction.run();
			if (matchManager.getWinnerTeam() != null) {
				winnerGlowApplier.accept(matchManager.getWinnerTeam(), systemConfig.gameEnd.winnerGlowSeconds);
			}
			playerAttributeResetter.run();
			tickRateController.accept(systemConfig.gameEnd.finalTickRate);
		}

		if (matchManager.getPhaseTicks() < systemConfig.gameEnd.returnToLobbyDelaySeconds * 20L) {
			return;
		}

		tickRateController.accept(systemConfig.gameEnd.restoreTickRate);
		glowClearer.run();
		biomeRestoreAction.run();
		lobbyReturnAction.run();
		active = false;
	}

	private void announce(SystemConfig.GameEndConfig config) {
		if (matchManager.getWinnerTeam() == null) {
			titleSender.accept(config.drawTitleTemplate);
			return;
		}

		titleSender.accept(config.titleTemplate.replace("{winner}", matchManager.getWinnerTeam().getDisplayName()));
	}
}
