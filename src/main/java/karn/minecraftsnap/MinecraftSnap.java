package karn.minecraftsnap;

import karn.minecraftsnap.audio.PhaseMusicService;
import karn.minecraftsnap.command.McSnapCommandRegistrar;
import karn.minecraftsnap.command.TeamChatService;
import karn.minecraftsnap.config.MinecraftSnapConfigManager;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.game.CaptainSelectionService;
import karn.minecraftsnap.game.BiomeRevealService;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.FactionSelectionService;
import karn.minecraftsnap.game.InGameRuleService;
import karn.minecraftsnap.game.LobbyCoordinator;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.TeamAssignmentService;
import karn.minecraftsnap.ui.BossBarService;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.WikiGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 서버사이드 전용 모드 초기화 클래스
 * DedicatedServerModInitializer를 사용하여 서버에서만 로직이 실행됨
 */
public class MinecraftSnap implements DedicatedServerModInitializer {

	// 모드 ID 상수
	public static final String MOD_ID = "minecraftsnap";

	// 로거 인스턴스
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static MinecraftSnap instance;

	private final TextTemplateResolver textTemplateResolver = new TextTemplateResolver();
	private final MatchManager matchManager = new MatchManager();
	private final BossBarService bossBarService = new BossBarService(matchManager, textTemplateResolver);
	private final PhaseMusicService phaseMusicService = new PhaseMusicService(matchManager);
	private final TeamChatService teamChatService = new TeamChatService(matchManager, textTemplateResolver);
	private final MinecraftSnapConfigManager configManager = new MinecraftSnapConfigManager(getConfigDirectory(), LOGGER);
	private final TeamAssignmentService teamAssignmentService = new TeamAssignmentService();
	private final CaptainSelectionService captainSelectionService = new CaptainSelectionService();
	private final FactionSelectionService factionSelectionService = new FactionSelectionService();
	private final WikiGuiService wikiGuiService = new WikiGuiService(textTemplateResolver);
	private final FactionSelectionGuiService factionSelectionGuiService = new FactionSelectionGuiService(textTemplateResolver);
	private final PreparationGuiService preparationGuiService = new PreparationGuiService(textTemplateResolver);
	private final LobbyScoreboardService lobbyScoreboardService = new LobbyScoreboardService(textTemplateResolver);
	private final McSnapCommandRegistrar commandRegistrar = new McSnapCommandRegistrar(this);
	private CapturePointService capturePointService;
	private BiomeRevealService biomeRevealService;
	private InGameRuleService inGameRuleService;
	private LobbyCoordinator lobbyCoordinator;

	@Override
	public void onInitializeServer() {
		instance = this;
		LOGGER.info("[{}] 서버사이드 모드 초기화 시작", MOD_ID);
		StyledChatSupport.initialize();
		configManager.load();
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		biomeRevealService = new BiomeRevealService(matchManager, textTemplateResolver);
		inGameRuleService = new InGameRuleService(matchManager, configManager.getStatsRepository(), textTemplateResolver);
		lobbyCoordinator = createLobbyCoordinator();

		// 커맨드 등록
		registerCommands();

		// 이벤트 리스너 등록
		registerEvents();

		LOGGER.info("[{}] 서버사이드 모드 초기화 완료", MOD_ID);
	}

	/**
	 * 서버 커맨드 등록
	 * CommandRegistrationCallback 사용
	 */
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register(commandRegistrar::register);
	}

	/**
	 * 서버 이벤트 리스너 등록
	 */
	private void registerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			matchManager.bindServer(server);
			LOGGER.info("[{}] 서버 시작 완료", MOD_ID);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> configManager.getStatsRepository().save());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			matchManager.tick();
			lobbyCoordinator.tick(server, configManager.getSystemConfig());
			biomeRevealService.tick(server, configManager.getSystemConfig());
			inGameRuleService.tick(server, configManager.getSystemConfig());
			capturePointService.tick(server, configManager.getSystemConfig());
			bossBarService.tick(server, configManager.getSystemConfig());
			phaseMusicService.tick(server, configManager.getSystemConfig());
			if (matchManager.getServerTicks() % 200L == 0L) {
				configManager.getStatsRepository().saveIfDirty();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			matchManager.handleJoin(handler.getPlayer());
			getStatsRepository().getOrCreate(handler.getPlayer().getUuid(), handler.getPlayer().getName().getString());
			lobbyCoordinator.handleJoin(handler.getPlayer(), configManager.getSystemConfig());
			phaseMusicService.handleJoin(handler.getPlayer(), configManager.getSystemConfig());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			matchManager.handleDisconnect(handler.getPlayer());
			lobbyCoordinator.handleDisconnect(server);
		});
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
			teamChatService.handleChatMessage(message, sender, params, configManager.getSystemConfig()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			inGameRuleService.allowDamage(entity, source));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
			inGameRuleService.handlePotentialDeath(entity, source, amount));
	}

	public void reload() {
		configManager.getStatsRepository().saveIfDirty();
		configManager.reload();
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		biomeRevealService = new BiomeRevealService(matchManager, textTemplateResolver);
		inGameRuleService = new InGameRuleService(matchManager, configManager.getStatsRepository(), textTemplateResolver);
		lobbyCoordinator = createLobbyCoordinator();
	}

	public MatchManager getMatchManager() {
		return matchManager;
	}

	public StatsRepository getStatsRepository() {
		return configManager.getStatsRepository();
	}

	public TextTemplateResolver getTextTemplateResolver() {
		return textTemplateResolver;
	}

	public CapturePointService getCapturePointService() {
		return capturePointService;
	}

	public void startTeamSelection() {
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.setPhase(MatchPhase.TEAM_SELECT);
			return;
		}
		lobbyCoordinator.startTeamSelection(server, configManager.getSystemConfig());
	}

	public void forceStartGame() {
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.enterGameStart();
			return;
		}
		lobbyCoordinator.forceStartGame(server, configManager.getSystemConfig());
	}

	public void forcePhase(MatchPhase phase) {
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.setPhase(phase);
			return;
		}
		lobbyCoordinator.forcePhase(phase, server, configManager.getSystemConfig());
	}

	public void openWiki(net.minecraft.server.network.ServerPlayerEntity player) {
		lobbyCoordinator.openCurrentGui(player, configManager.getSystemConfig());
	}

	public boolean handleShortcut(net.minecraft.server.network.ServerPlayerEntity player) {
		return lobbyCoordinator.handleShortcut(player, configManager.getSystemConfig());
	}

	public static MinecraftSnap getInstance() {
		return instance;
	}

	public void setLaneRevealState(karn.minecraftsnap.game.LaneId laneId, boolean revealed) {
		lobbyCoordinator.setLaneRevealState(laneId, revealed);
	}

	private LobbyCoordinator createLobbyCoordinator() {
		return new LobbyCoordinator(
			matchManager,
			configManager.getStatsRepository(),
			teamAssignmentService,
			captainSelectionService,
			factionSelectionService,
			wikiGuiService,
			factionSelectionGuiService,
			lobbyScoreboardService,
			preparationGuiService
		);
	}

	private Path getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}
}
