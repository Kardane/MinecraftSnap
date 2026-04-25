package karn.minecraftsnap;

import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import karn.minecraftsnap.audio.MinecraftSnapAudioCatalog;
import karn.minecraftsnap.audio.PhaseMusicService;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.biome.BiomeEffectRegistry;
import karn.minecraftsnap.command.McSnapCommandRegistrar;
import karn.minecraftsnap.command.TeamChatService;
import karn.minecraftsnap.config.MinecraftSnapConfigManager;
import karn.minecraftsnap.config.MinecraftSnapResourcePackConfigurer;
import karn.minecraftsnap.config.ServerStatsRepository;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.AnnouncementFormatter;
import karn.minecraftsnap.game.LadderRewardService;
import karn.minecraftsnap.game.TitleDisplayService;
import karn.minecraftsnap.game.AdminCommandService;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.FactionStructureResetService;
import karn.minecraftsnap.game.UnitSpawnQueueService;
import karn.minecraftsnap.game.CaptainSelectionService;
import karn.minecraftsnap.game.BiomeRevealService;
import karn.minecraftsnap.game.CaptainManaService;
import karn.minecraftsnap.game.CaptainSkillService;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.FactionSelectionService;
import karn.minecraftsnap.game.GameEndService;
import karn.minecraftsnap.game.GameStartCountdownService;
import karn.minecraftsnap.game.InGameRuleService;
import karn.minecraftsnap.game.LaneBiomeService;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.LaneStructureService;
import karn.minecraftsnap.game.LobbyCoordinator;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.TeamAssignmentService;
import karn.minecraftsnap.game.UnitAbilityService;
import karn.minecraftsnap.game.UnitHookService;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.game.OngoingMatchJoinService;
import karn.minecraftsnap.game.SurrenderVoteService;
import karn.minecraftsnap.game.UnitSpawnService;
import karn.minecraftsnap.game.VillagerEnchantService;
import karn.minecraftsnap.game.VictoryCountdownService;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.AdminToolsGuiService;
import karn.minecraftsnap.ui.BossBarService;
import karn.minecraftsnap.ui.CaptainSpawnGuiService;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.ui.CaptureHudService;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.MainLobbyGuiService;
import karn.minecraftsnap.ui.PlayerDisplayNameService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.ui.UnitHudService;
import karn.minecraftsnap.ui.WikiGuiService;
import karn.minecraftsnap.unit.SummonedMobSupport;
import karn.minecraftsnap.unit.UnitClassRegistry;
import karn.minecraftsnap.unit.nether.GhastUnit;
import karn.minecraftsnap.util.TextTemplateResolver;
import karn.minecraftsnap.integration.DisguiseAnimationSupport;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.UUID;

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
	private final UiSoundService uiSoundService = new UiSoundService();
	private final TeamChatService teamChatService = new TeamChatService(matchManager, textTemplateResolver);
	private final MinecraftSnapConfigManager configManager = new MinecraftSnapConfigManager(getConfigDirectory(), LOGGER);
	private final TeamAssignmentService teamAssignmentService = new TeamAssignmentService();
	private final CaptainSelectionService captainSelectionService = new CaptainSelectionService();
	private final FactionSelectionService factionSelectionService = new FactionSelectionService();
	private final CaptainManaService captainManaService = new CaptainManaService();
	private final LaneRuntimeRegistry laneRuntimeRegistry = new LaneRuntimeRegistry();
	private final BiomeEffectRegistry biomeEffectRegistry = new BiomeEffectRegistry();
	private final LaneStructureService laneStructureService = new LaneStructureService();
	private final UnitClassRegistry unitClassRegistry = new UnitClassRegistry();
	private final UnitRegistry unitRegistry = new UnitRegistry(false);
	private final AdvanceService advanceService = new AdvanceService(unitRegistry);
	private final UnitLoadoutService unitLoadoutService = new UnitLoadoutService();
	private final VillagerEnchantService villagerEnchantService = new VillagerEnchantService();
	private final WikiGuiService wikiGuiService = new WikiGuiService(textTemplateResolver, unitRegistry, configManager::getBiomeCatalog, matchManager::getPhase, uiSoundService);
	private final MainLobbyGuiService mainLobbyGuiService = new MainLobbyGuiService(
		textTemplateResolver,
		configManager::getStatsRepository,
		matchManager::getPhase,
		uiSoundService,
		player -> wikiGuiService.open(player, matchManager.getPhase()),
		wikiGuiService::openUnitFactionIndex,
		wikiGuiService::openBiomeIndex,
		this::openAdminToolsGui
	);
	private final AdminToolsGuiService adminToolsGuiService = new AdminToolsGuiService(
		textTemplateResolver,
		uiSoundService,
		player -> mainLobbyGuiService.open(player),
		this::handleAdminAssignTeams,
		this::handleAdminClearTeams,
		this::handleAdminOpenFactionSelect,
		this::handleAdminEndGame
	);
	private final FactionSelectionGuiService factionSelectionGuiService = new FactionSelectionGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final PreparationGuiService preparationGuiService = new PreparationGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final CaptainSpawnGuiService captainSpawnGuiService = new CaptainSpawnGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final CaptainWeatherGuiService captainWeatherGuiService = new CaptainWeatherGuiService(textTemplateResolver, uiSoundService);
	private final TradeGuiService tradeGuiService = new TradeGuiService(
		textTemplateResolver,
		unitLoadoutService,
		configManager::getShopConfig,
		configManager::getStatsRepository,
		uiSoundService,
		villagerEnchantService
	);
	private final AdvanceGuiService advanceGuiService = new AdvanceGuiService(textTemplateResolver, uiSoundService);
	private final PlayerDisplayNameService playerDisplayNameService = new PlayerDisplayNameService();
	private final LadderRewardService ladderRewardService = new LadderRewardService();
	private final TitleDisplayService titleDisplayService = new TitleDisplayService(matchManager, textTemplateResolver);
	private final AdminCommandService adminCommandService = new AdminCommandService();
	private final CaptureHudService captureHudService = new CaptureHudService(matchManager, textTemplateResolver);
	private final GameStartCountdownService gameStartCountdownService = new GameStartCountdownService(matchManager, seconds -> titleDisplayService.showGameStartCountdown(matchManager.getServer(), configManager.getSystemConfig(), seconds), this::playCountdownSound);
	private final VictoryCountdownService victoryCountdownService = new VictoryCountdownService(matchManager, (teamId, seconds) -> titleDisplayService.showVictoryCountdown(matchManager.getServer(), configManager.getSystemConfig(), teamId, seconds), this::playVictoryCountdownSound);
	private final LobbyScoreboardService lobbyScoreboardService = new LobbyScoreboardService(textTemplateResolver);
	private final McSnapCommandRegistrar commandRegistrar = new McSnapCommandRegistrar(this);
	private final LaneBiomeService laneBiomeService = new LaneBiomeService();
	private final FactionStructureResetService factionStructureResetService = new FactionStructureResetService(laneStructureService);
	private final GameEndService gameEndService = new GameEndService(
		matchManager,
		textTemplateResolver,
		(title, subtitle) -> titleDisplayService.showGameTitle(matchManager.getServer(), title, subtitle),
		this::applyWinnerGlow,
		this::resetOnlinePlayerAttributes,
		this::clearLaneEntities,
		this::clearWinnerGlow,
		this::applyTickRate,
		this::shouldApplyGameEndRewards,
		this::applyLadderRewardsAndRefresh,
		this::returnPlayersToLobby,
		() -> laneBiomeService.restoreAll(matchManager.getServer())
	);
	private CapturePointService capturePointService;
	private BiomeRevealService biomeRevealService;
	private InGameRuleService inGameRuleService;
	private LobbyCoordinator lobbyCoordinator;
	private CaptainSkillService captainSkillService;
	private final UnitAbilityService unitAbilityService = new UnitAbilityService(textTemplateResolver, laneRuntimeRegistry, () -> captainSkillService);
	private final UnitHookService unitHookService = new UnitHookService(
		matchManager,
		unitRegistry,
		unitClassRegistry,
		laneRuntimeRegistry,
		configManager::getStatsRepository,
		textTemplateResolver,
		unitAbilityService,
		unitLoadoutService,
		advanceService,
		tradeGuiService,
		advanceGuiService,
		captainManaService,
		playerDisplayNameService,
		uiSoundService,
		villagerEnchantService,
		() -> configManager.getShopConfig(FactionId.VILLAGER)
	);
	private final UnitSpawnQueueService unitSpawnQueueService = new UnitSpawnQueueService();
	private final UnitSpawnService unitSpawnService = new UnitSpawnService(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService, uiSoundService, unitSpawnQueueService, () -> captainSkillService, () -> unitHookService);
	private final UnitHudService unitHudService = new UnitHudService(matchManager, unitRegistry, captainManaService, unitAbilityService, textTemplateResolver, unitSpawnQueueService);
	private final OngoingMatchJoinService ongoingMatchJoinService = new OngoingMatchJoinService(matchManager, unitSpawnQueueService, unitSpawnService, textTemplateResolver);
	private final SurrenderVoteService surrenderVoteService = new SurrenderVoteService(matchManager, textTemplateResolver, message -> {
	});
	private MatchPhase observedPhase = MatchPhase.LOBBY;
	private boolean captainSkillUnlockAnnounced;
	private boolean suppressNextGameEndRewards;
	private final Map<UUID, LadderChange> pendingGameEndLadderChanges = new HashMap<>();
	private int lastCaptainRevealWarningElapsedSeconds = -1;

	@Override
	public void onInitializeServer() {
		instance = this;
		LOGGER.info("[{}] 서버사이드 모드 초기화 시작", MOD_ID);
		StyledChatSupport.initialize();
		registerOverlaySounds();
		configManager.load();
		unitRegistry.setDefinitions(configManager.getUnitDefinitions().values());
		unitRegistry.applyTextConfig(configManager.getTextConfig());
		unitClassRegistry.validateAgainst(unitRegistry);
		initializeServices();
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

	private void registerOverlaySounds() {
		for (var overlay : MinecraftSnapAudioCatalog.ALL_OVERLAY_SOUNDS) {
			registerOverlaySound(overlay.id(), overlay.fallback());
		}
	}

	private void registerOverlaySound(Identifier id, SoundEvent fallback) {
		SoundEvent sound = Registries.SOUND_EVENT.containsId(id)
			? Registries.SOUND_EVENT.get(id)
			: Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
		PolymerSoundEvent.registerOverlay(sound, fallback);
	}

	/**
	 * 서버 이벤트 리스너 등록
	 */
	private void registerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			matchManager.bindServer(server);
			prepareLobbyLaneBiomes();
			new MinecraftSnapResourcePackConfigurer(server, LOGGER).applyDefaults();
			LOGGER.info("[{}] 서버 시작 완료", MOD_ID);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> configManager.getStatsRepository().save());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> configManager.getServerStatsRepository().save());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			matchManager.syncOnlinePlayersFromScoreboard();
			matchManager.tick();
			DisguiseAnimationSupport.tick(server);
			handlePhaseTransition();
			factionStructureResetService.tick(server, configManager.getSystemConfig(), matchManager.getServerTicks());
			lobbyCoordinator.tick(server, configManager.getSystemConfig());
			gameStartCountdownService.tick(configManager.getSystemConfig());
			capturePointService.tick(server, configManager.getSystemConfig());
			long currentTicks = matchManager.getServerTicks();
			if (currentTicks % 5L == 0L) {
				captureHudService.tick(server, configManager.getSystemConfig(), capturePointService);
				laneRuntimeRegistry.refresh(server, configManager.getSystemConfig(), matchManager, capturePointService);
			}
			var revealed = biomeRevealService.tick(server, configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
			laneBiomeService.tick(server);
			if (!revealed.isEmpty()) {
				refillCaptainMana(revealed.size());
				handleBiomeRevealFollowUp(revealed);
			}
			maybeWarnCaptainsBeforeNextReveal(server, configManager.getSystemConfig());
			tickCaptainMana(server);
			if (captainSkillService != null) {
				captainSkillService.tick(server, configManager.getSystemConfig());
			}
			SummonedMobSupport.clearFriendlyTargets(server, matchManager);
			unitHookService.tick(server, configManager.getSystemConfig());
			unitAbilityService.tick(server, matchManager);
			unitHudService.tick(server, configManager.getSystemConfig());
			unitSpawnService.maintainActiveUnits(matchManager);
			unitSpawnService.tickSpawnProtection(server, matchManager);
			inGameRuleService.tick(server, configManager.getSystemConfig());
			victoryCountdownService.tick(configManager.getSystemConfig());
			if (currentTicks % 5L == 0L) {
				bossBarService.tick(server, configManager.getSystemConfig());
			}
			playFactionSelectionTickSound();
			if (matchManager.getServerTicks() % 20L == 0L) {
				tickPlayTimeStats(server);
				playerDisplayNameService.sync(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
			}
			phaseMusicService.tick(server);
			gameEndService.tick(configManager.getSystemConfig());
			if (matchManager.getServerTicks() % 200L == 0L) {
				configManager.getStatsRepository().saveIfDirty();
				configManager.getServerStatsRepository().saveIfDirty();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			matchManager.handleJoin(handler.getPlayer());
			unitSpawnService.getUnitLoadoutService().resetPlayerAttributes(handler.getPlayer());
			var stats = getStatsRepository().getOrCreate(handler.getPlayer().getUuid(), handler.getPlayer().getName().getString());
			matchManager.syncPersistentState(handler.getPlayer().getUuid(), stats.emeralds, stats.goldIngots);
			lobbyCoordinator.handleJoin(handler.getPlayer(), configManager.getSystemConfig());
			ongoingMatchJoinService.handleJoin(handler.getPlayer(), configManager.getSystemConfig(), configManager.getTextConfig());
			playerDisplayNameService.refreshAll(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
			phaseMusicService.handleJoin(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			matchManager.handleDisconnect(handler.getPlayer());
			surrenderVoteService.clearPlayer(handler.getPlayer().getUuid());
			if (!ongoingMatchJoinService.handleDisconnect(handler.getPlayer())) {
				unitSpawnQueueService.removePlayer(handler.getPlayer().getUuid());
			}
			lobbyCoordinator.handleDisconnect(server);
		});
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
			teamChatService.handleChatMessage(message, sender, params, configManager.getSystemConfig()));
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
				return ActionResult.PASS;
			}
			return handleUseItem(serverPlayer, hand) ? ActionResult.SUCCESS : ActionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
				return ActionResult.PASS;
			}
			return handleUseItem(serverPlayer, hand) ? ActionResult.SUCCESS : ActionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
				return ActionResult.PASS;
			}
			return handleUseItem(serverPlayer, hand) ? ActionResult.SUCCESS : ActionResult.PASS;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			inGameRuleService.allowDamage(entity, source, amount));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
			inGameRuleService.handlePotentialDeath(entity, source, amount));
	}

	public void reload() {
		configManager.getStatsRepository().saveIfDirty();
		configManager.getServerStatsRepository().saveIfDirty();
		configManager.reload();
		unitRegistry.setDefinitions(configManager.getUnitDefinitions().values());
		unitRegistry.applyTextConfig(configManager.getTextConfig());
		unitClassRegistry.validateAgainst(unitRegistry);

		initializeServices();

		lobbyCoordinator = createLobbyCoordinator();
		if (matchManager.getServer() != null) {
			new MinecraftSnapResourcePackConfigurer(matchManager.getServer(), LOGGER).applyDefaults();
		}
		for (var player : matchManager.getOnlinePlayers()) {
			var stats = getStatsRepository().getOrCreate(player.getUuid(), player.getName().getString());
			matchManager.syncPersistentState(player.getUuid(), stats.emeralds, stats.goldIngots);
		}
		playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
	}

	private void initializeServices() {
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		laneStructureService.reset();
		laneRuntimeRegistry.reset();

		capturePointService = new CapturePointService(
			matchManager,
			configManager.getStatsRepository(),
			uiSoundService,
			textTemplateResolver,
			laneRuntimeRegistry,
			unitHookService
		);

		captainSkillService = new CaptainSkillService(
			matchManager,
			laneRuntimeRegistry,
			captainWeatherGuiService,
			captainManaService,
			textTemplateResolver,
			uiSoundService
		);

		biomeRevealService = new BiomeRevealService(
			matchManager,
			textTemplateResolver,
			new java.util.Random(),
			laneRuntimeRegistry,
			laneStructureService,
			biomeEffectRegistry
		);

		inGameRuleService = new InGameRuleService(
			matchManager,
			configManager.getStatsRepository(),
			textTemplateResolver,
			unitSpawnQueueService,
			unitSpawnService,
			captainManaService,
			unitRegistry,
			unitAbilityService,
			laneRuntimeRegistry,
			unitHookService,
			uiSoundService,
			configManager.getServerStatsRepository()
		);
	}

	public MatchManager getMatchManager() {
		return matchManager;
	}

	public StatsRepository getStatsRepository() {
		return configManager.getStatsRepository();
	}

	public ServerStatsRepository getServerStatsRepository() {
		return configManager.getServerStatsRepository();
	}

	public TextTemplateResolver getTextTemplateResolver() {
		return textTemplateResolver;
	}

	public CapturePointService getCapturePointService() {
		return capturePointService;
	}

	public karn.minecraftsnap.config.BiomeCatalog getBiomeCatalog() {
		return configManager.getBiomeCatalog();
	}

	public BiomeRevealService getBiomeRevealService() {
		return biomeRevealService;
	}

	public boolean replaceAllAssignedBiomes(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return false;
		}
		var server = matchManager.getServer();
		var systemConfig = configManager.getSystemConfig();
		var biomeEntry = findBiomeEntry(biomeId);
		if (server == null || systemConfig == null || biomeEntry == null) {
			return false;
		}
		for (var laneId : LaneId.values()) {
			matchManager.setAssignedBiomeId(laneId, biomeEntry.id);
			if (!matchManager.isLaneRevealed(laneId)) {
				continue;
			}
			laneBiomeService.applyAssignedBiome(server, laneId, systemConfig, biomeEntry.minecraftBiomeId);
			var runtime = laneRuntimeRegistry.get(laneId);
			if (runtime == null) {
				continue;
			}
			runtime.revealBiome(biomeEntry, biomeEffectRegistry.create(biomeEntry), matchManager.getElapsedSeconds());
			runtime.markRevealEffectApplied();
			var laneRegion = runtime.laneRegion();
			if (laneRegion != null && biomeEntry.structureId != null && !biomeEntry.structureId.isBlank()) {
				laneStructureService.forcePlaceStructure(
					server,
					systemConfig.world,
					laneId,
					"minecraft:default",
					laneStructureService.originFor(laneRegion)
				);
				laneStructureService.forcePlaceStructure(
					server,
					systemConfig.world,
					laneId,
					biomeEntry.structureId,
					laneStructureService.originFor(laneRegion)
				);
			}
		}
		return true;
	}

	public void startTeamSelection() {
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.setPhase(MatchPhase.TEAM_SELECT);
			return;
		}
		lobbyCoordinator.startTeamSelection(server, configManager.getSystemConfig());
	}

	public void assignTeamsOnly() {
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.setPhase(MatchPhase.TEAM_SELECT);
			return;
		}
		lobbyCoordinator.assignTeamsOnly(server, configManager.getSystemConfig());
	}

	public boolean toggleAutoStart() {
		return lobbyCoordinator.toggleAutoAdvanceTeamSelection();
	}

	public boolean isAutoStartEnabled() {
		return lobbyCoordinator.isAutoAdvanceTeamSelection();
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
		if (phase != MatchPhase.GAME_END) {
			suppressNextGameEndRewards = false;
		}
		var server = matchManager.getOnlinePlayers().stream().findFirst().map(player -> player.getServer()).orElse(null);
		if (server == null) {
			matchManager.setPhase(phase);
			return;
		}
		lobbyCoordinator.forcePhase(phase, server, configManager.getSystemConfig());
	}

	public void forceAdminGameEnd() {
		suppressNextGameEndRewards = true;
		forcePhase(MatchPhase.GAME_END);
	}

	public void clearAllTeams() {
		matchManager.clearPlayerAssignments();
		var server = matchManager.getServer();
		if (server != null) {
			playerDisplayNameService.refreshAll(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
		}
	}

	public void openWiki(net.minecraft.server.network.ServerPlayerEntity player) {
		lobbyCoordinator.openWiki(player);
	}

	public boolean handleShortcut(net.minecraft.server.network.ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (shouldUseUnitShortcut(matchManager.getPhase(), state.getRoleType(), state.getCurrentUnitId(), player.isSpectator())) {
			return unitHookService.handleShiftF(player, configManager.getSystemConfig());
		}
		return lobbyCoordinator.handleShortcut(player, configManager.getSystemConfig());
	}

	public boolean shouldCancelPlayerMove(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		return unitHookService.shouldCancelMove(player, packet, configManager.getSystemConfig());
	}



	public static MinecraftSnap getInstance() {
		return instance;
	}

	static boolean shouldUseUnitShortcut(MatchPhase phase, RoleType roleType, String currentUnitId, boolean spectator) {
		return phase == MatchPhase.GAME_RUNNING
			&& UnitHookService.canUseUnitActions(roleType, currentUnitId, spectator);
	}

	static boolean isFallDamageEnabledForPhase(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING;
	}

	public void setLaneRevealState(karn.minecraftsnap.game.LaneId laneId, boolean revealed) {
		lobbyCoordinator.setLaneRevealState(laneId, revealed);
	}

	public boolean chargeCaptainMana(ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.CAPTAIN) {
			return false;
		}
		captainManaService.refillMana(player.getUuid(), captainManaRecoverySeconds(matchManager, state.getTeamId()));
		return true;
	}

	public boolean triggerCaptainSkill(karn.minecraftsnap.game.FactionId factionId) {
		var triggered = false;
		for (var captainId : matchManager.getCaptainIdsByFaction(factionId)) {
			var player = matchManager.getOnlinePlayers().stream()
				.filter(candidate -> candidate.getUuid().equals(captainId))
				.findFirst()
				.orElse(null);
			if (player != null && unitAbilityService.useCaptainSkill(player, matchManager, captainManaService)) {
				triggered = true;
			}
		}
		return triggered;
	}

	public void handleLivingDamageApplied(net.minecraft.entity.LivingEntity entity, net.minecraft.entity.damage.DamageSource source, float healthBefore, float healthAfter, boolean damaged) {
		if (inGameRuleService != null) {
			inGameRuleService.handleDamageApplied(entity, source, healthBefore, healthAfter, damaged);
		}
	}

	public void handleProjectileHit(ProjectileEntity projectile, Entity target) {
		if (projectile == null || target == null || unitHookService == null) {
			return;
		}
		var player = resolveProjectileHookOwner(projectile);
		if (player == null) {
			return;
		}
		unitHookService.handleProjectileHit(player, projectile, target, configManager.getSystemConfig());
	}

	public void handleProjectileImpact(ProjectileEntity projectile, Vec3d impactPos) {
		if (projectile == null || impactPos == null || unitHookService == null) {
			return;
		}
		var player = resolveProjectileHookOwner(projectile);
		if (player == null) {
			return;
		}
		unitHookService.handleProjectileImpact(player, projectile, impactPos, configManager.getSystemConfig());
	}

	public String spawnBots(net.minecraft.server.command.ServerCommandSource source, int count) {
		return adminCommandService.spawnBots(source, count, configManager.getTextConfig());
	}

	public String adjustMatchTime(int ticks) {
		if (matchManager.getPhase() != MatchPhase.GAME_START && matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return "&c게임 준비 또는 진행 중에만 시간을 조정할 수 있습니다";
		}
		matchManager.adjustDurationTicks(ticks);
		return configManager.getTextConfig().commandTimeAdjustedMessage
			.replace("{ticks}", Integer.toString(ticks))
			.replace("{time}", karn.minecraftsnap.ui.BossBarFormatter.formatTime(matchManager.getRemainingSeconds()))
			.replace("{remaining_ticks}", Integer.toString(matchManager.getRemainingTicks()));
	}

	public String voteSurrender(ServerPlayerEntity player) {
		if (player == null) {
			return configManager.getTextConfig().commandPlayerNotFoundMessage;
		}
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return configManager.getTextConfig().surrenderVoteUnavailableMessage;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getTeamId() == null || state.getRoleType() == RoleType.NONE) {
			return configManager.getTextConfig().surrenderVoteNoTeamMessage;
		}
		var result = surrenderVoteService.vote(new SurrenderVoteService.Voter(player.getUuid(), state.getTeamId(), player.getName().getString()));
		if (result.duplicate()) {
			return configManager.getTextConfig().surrenderVoteDuplicateMessage;
		}
		var message = configManager.getTextConfig().surrenderVoteProgressMessage
			.replace("{current}", Integer.toString(result.currentVotes()))
			.replace("{required}", Integer.toString(result.requiredVotes()))
			.replace("{unit_current}", Integer.toString(result.unitVotes()))
			.replace("{unit_required}", Integer.toString(result.totalUnits()));
		for (var member : matchManager.getOnlineTeamPlayers(state.getTeamId())) {
			member.sendMessage(textTemplateResolver.format(message), false);
		}
		return message;
	}

	public String useSnap(ServerPlayerEntity player) {
		if (player == null) {
			return configManager.getTextConfig().commandPlayerNotFoundMessage;
		}
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return configManager.getTextConfig().snapUnavailableMessage;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.CAPTAIN || state.getTeamId() == null) {
			return configManager.getTextConfig().snapCaptainOnlyMessage;
		}
		if (matchManager.isLaneRevealed(LaneId.LANE_3)) {
			return configManager.getTextConfig().snapClosedMessage;
		}
		int limit = Math.max(1, configManager.getSystemConfig().ladderReward.snapUseLimitPerTeam);
		if (!matchManager.useTeamSnap(state.getTeamId(), limit)) {
			return configManager.getTextConfig().snapAlreadyUsedMessage;
		}
		var message = configManager.getTextConfig().snapBroadcastMessage
			.replace("{team}", state.getTeamId().getDisplayName())
			.replace("{player}", player.getName().getString())
			.replace("{multiplier}", currentSnapMultiplierText());
		var server = matchManager.getServer();
		if (server != null) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(message), false);
		}
		uiSoundService.playGlobalSnap(matchManager.getServer());
		return null;
	}

	public String joinOngoingMatch(ServerPlayerEntity player) {
		return ongoingMatchJoinService.join(player, configManager.getSystemConfig(), configManager.getTextConfig());
	}

	public UnitSpawnService getUnitSpawnService() {
		return unitSpawnService;
	}

	public UnitRegistry getUnitRegistry() {
		return unitRegistry;
	}

	public CaptainManaService getCaptainManaService() {
		return captainManaService;
	}

	public SystemConfig getSystemConfig() {
		return configManager.getSystemConfig();
	}

	public karn.minecraftsnap.config.TextConfigFile getTextConfig() {
		return configManager.getTextConfig();
	}

	public boolean forceAdvance(ServerPlayerEntity player) {
		return player != null && advanceService.forceAdvance(matchManager.getPlayerState(player.getUuid()));
	}

	public String forceAssignUnit(ServerPlayerEntity player, String unitId) {
		return adminCommandService.forceAssignUnit(
			player,
			unitId,
			matchManager,
			unitRegistry,
			unitHookService,
			configManager.getSystemConfig(),
			configManager.getTextConfig()
		);
	}

	public String placeNearestBiomeStructure(ServerPlayerEntity player) {
		return placeNearestBiomeStructure(player, null);
	}

	public String placeNearestBiomeStructure(ServerPlayerEntity player, String structureId) {
		return adminCommandService.placeNearestBiomeStructure(
			player,
			structureId,
			matchManager,
			configManager.getSystemConfig(),
			configManager.getBiomeCatalog(),
			laneStructureService,
			configManager.getTextConfig()
		);
	}

	public String resetAllBiomeStructures() {
		return adminCommandService.resetAllBiomeStructures(
			matchManager.getServer(),
			configManager.getSystemConfig(),
			laneStructureService,
			configManager.getTextConfig()
		);
	}

	public String openAdminGui(ServerPlayerEntity player, String guiId) {
		return adminCommandService.openAdminGui(
			player,
			guiId,
			matchManager,
			configManager.getTextConfig(),
			p -> mainLobbyGuiService.open(p),
			p -> wikiGuiService.open(p, matchManager.getPhase()),
			wikiGuiService::openUnitFactionIndex,
			wikiGuiService::openBiomeIndex,
			this::openAdminToolsGui,
			(p, callback) -> {
				var state = matchManager.getPlayerState(p.getUuid());
				var teamId = state.getTeamId() == null ? TeamId.RED : state.getTeamId();
				factionSelectionGuiService.open(p, teamId, matchManager.getFactionSelection(teamId), callback);
			},
			p -> preparationGuiService.open(p, matchManager.getPlayerState(p.getUuid())),
			this::openCaptainSpawnGui,
			p -> tradeGuiService.open(p, matchManager.getPlayerState(p.getUuid())),
			p -> openAdvanceGui(p, matchManager.getPlayerState(p.getUuid())),
			this::setFactionSelectionAndAnnounce
		);
	}

	private LobbyCoordinator createLobbyCoordinator() {
		return new LobbyCoordinator(
			matchManager,
			configManager.getStatsRepository(),
			teamAssignmentService,
			captainSelectionService,
			factionSelectionService,
			wikiGuiService,
			mainLobbyGuiService,
			factionSelectionGuiService,
			lobbyScoreboardService,
				preparationGuiService,
				unitSpawnService,
				textTemplateResolver,
				this::setFactionSelectionAndAnnounce,
				() -> !factionStructureResetService.isActive()
			);
	}

	private boolean handleUseItem(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.util.Hand hand) {
		var stack = player.getStackInHand(hand);
		var state = matchManager.getPlayerState(player.getUuid());
		var unitDefinition = unitRegistry.get(state.getCurrentUnitId());
		if (state.getRoleType() == RoleType.CAPTAIN) {
			if (!canCaptainUseItems(matchManager.getPhase())) {
				return false;
			}
			if (unitLoadoutService.matchesCaptainMenuTrigger(stack)) {
				openCaptainSpawnGui(player);
				return true;
			}
			if (unitLoadoutService.matchesCaptainSnapTrigger(stack)) {
				var message = useSnap(player);
				if (message != null) {
					uiSoundService.playUiDeny(player);
					player.sendMessage(textTemplateResolver.format(message), false);
				}
				return true;
			}
			if (unitLoadoutService.matchesCaptainSkillTrigger(stack)) {
				return unitAbilityService.useCaptainSkill(player, matchManager, captainManaService);
			}
			return false;
		}

		if (UnitHookService.canUseUnitActions(state.getRoleType(), state.getCurrentUnitId(), player.isSpectator())
			&& unitLoadoutService.matchesUnitAbilityTrigger(stack, unitDefinition)) {
			return unitHookService.handleSkillUse(player, configManager.getSystemConfig());
		}
		return false;
	}

	static boolean canCaptainUseItems(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING;
	}

	static boolean shouldBlockCaptainItemDrop(RoleType roleType) {
		return roleType == RoleType.CAPTAIN || roleType == RoleType.UNIT;
	}

	static boolean shouldTrackPlayTime(MatchPhase phase, PlayerMatchState state, boolean spectator) {
		return (phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING)
			&& state != null
			&& state.getTeamId() != null
			&& state.getRoleType() != RoleType.NONE
			&& state.getRoleType() != RoleType.SPECTATOR
			&& !spectator;
	}

	static int countCaptainRecoveryTeammates(MatchManager matchManager, TeamId teamId) {
		if (matchManager == null || teamId == null) {
			return 0;
		}
		return (int) matchManager.getPlayerStatesSnapshot().values().stream()
			.filter(state -> state.getTeamId() == teamId)
			.filter(state -> state.getRoleType() != RoleType.NONE)
			.filter(state -> state.getRoleType() != RoleType.CAPTAIN)
			.count();
	}

	static int captainManaRecoverySeconds(MatchManager matchManager, TeamId teamId) {
		return CaptainManaService.recoverySecondsForTeamSize(countCaptainRecoveryTeammates(matchManager, teamId));
	}

	static boolean shouldAnnounceCaptainSkillUnlock(MatchPhase phase, int elapsedSeconds, boolean announced) {
		return !announced
			&& phase == MatchPhase.GAME_RUNNING
			&& CaptainSkillService.isSkillUnlocked(elapsedSeconds);
	}

	private void openCaptainSpawnGui(net.minecraft.server.network.ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getFactionId() == null) {
			uiSoundService.playUiDeny(player);
			player.sendMessage(textTemplateResolver.format(configManager.getSystemConfig().gameStart.captainSpawnNoFactionMessage), false);
			return;
		}
		if (player.getItemCooldownManager().isCoolingDown(Items.BELL.getDefaultStack())) {
			uiSoundService.playUiDeny(player);
			var cooldownTicks = Math.max(0, configManager.getSystemConfig().gameStart.unitSpawnItemCooldownTicks);
			var cooldownProgress = player.getItemCooldownManager().getCooldownProgress(Items.BELL.getDefaultStack(), 0.0F);
			var remainingSeconds = Math.max(1, (int) Math.ceil((cooldownTicks * cooldownProgress) / 20.0D));
			player.sendMessage(
				textTemplateResolver.format(
					configManager.getSystemConfig().gameStart.captainSpawnCooldownMessage.replace("{seconds}", Integer.toString(remainingSeconds))
				),
				false
			);
			return;
		}

		captainSpawnGuiService.open(
			player,
			state.getFactionId(),
			captainManaService.getOrCreate(player.getUuid()),
			configManager.getSystemConfig().gameStart.captainSpawnGuiTitle,
			definition -> {
				var result = unitSpawnService.spawnSelectedUnit(player, definition.id(), matchManager, configManager.getSystemConfig(), textTemplateResolver);
				if (!result.success() && result.message() != null) {
					player.sendMessage(textTemplateResolver.format(result.message()), false);
				}
			}
		);
	}

	private void openAdvanceGui(net.minecraft.server.network.ServerPlayerEntity player, PlayerMatchState state) {
		unitHookService.openAdvanceGui(player, configManager.getSystemConfig());
	}

	private void clearLaneEntities() {
		var server = matchManager.getServer();
		var systemConfig = configManager.getSystemConfig();
		if (server == null || systemConfig == null) {
			return;
		}
		var world = resolveConfiguredWorld(server, systemConfig.world);
		if (world == null) {
			return;
		}
		clearLaneEntities(world, systemConfig.inGame.lane1Region);
		clearLaneEntities(world, systemConfig.inGame.lane2Region);
		clearLaneEntities(world, systemConfig.inGame.lane3Region);
	}

	private void clearLaneEntities(net.minecraft.server.world.ServerWorld world, SystemConfig.LaneRegionConfig laneRegion) {
		if (world == null || laneRegion == null) {
			return;
		}
		var box = new net.minecraft.util.math.Box(
			laneRegion.minX,
			laneRegion.minY,
			laneRegion.minZ,
			laneRegion.maxX,
			laneRegion.maxY,
			laneRegion.maxZ
		);
		for (var entity : world.getOtherEntities(null, box, entity -> !(entity instanceof net.minecraft.server.network.ServerPlayerEntity))) {
			if (entity instanceof net.minecraft.entity.LivingEntity livingEntity) {
				livingEntity.kill(world);
				continue;
			}
			entity.discard();
		}
	}

	private net.minecraft.server.world.ServerWorld resolveConfiguredWorld(net.minecraft.server.MinecraftServer server, String worldId) {
		if (server == null) {
			return null;
		}
		try {
			var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	private void openAdminToolsGui(ServerPlayerEntity player) {
		if (player == null || !player.hasPermissionLevel(4)) {
			if (player != null) {
				uiSoundService.playUiDeny(player);
			}
			return;
		}
		adminToolsGuiService.open(player);
	}

	private void handleAdminAssignTeams(ServerPlayerEntity player) {
		assignTeamsOnly();
		if (player != null) {
			player.sendMessage(textTemplateResolver.formatUi(configManager.getTextConfig().commandTeamSelectionMessage), false);
		}
	}

	private void handleAdminClearTeams(ServerPlayerEntity player) {
		clearAllTeams();
		if (player != null) {
			player.sendMessage(textTemplateResolver.formatUi(configManager.getTextConfig().commandClearTeamsMessage), false);
		}
	}

	private void handleAdminOpenFactionSelect(ServerPlayerEntity player) {
		if (matchManager.getCaptainId(TeamId.RED) == null || matchManager.getCaptainId(TeamId.BLUE) == null) {
			if (player != null) {
				uiSoundService.playUiDeny(player);
				player.sendMessage(textTemplateResolver.formatUi(configManager.getTextConfig().adminToolsFactionSelectBlockedMessage), false);
			}
			return;
		}
		forcePhase(MatchPhase.FACTION_SELECT);
		if (player != null) {
			player.sendMessage(textTemplateResolver.formatUi(configManager.getTextConfig().commandPhaseChangedMessage.replace("{phase}", MatchPhase.FACTION_SELECT.getDisplayName())), false);
		}
	}

	private void handleAdminEndGame(ServerPlayerEntity player) {
		forcePhase(MatchPhase.GAME_END);
		if (player != null) {
			player.sendMessage(textTemplateResolver.formatUi(configManager.getTextConfig().commandPhaseChangedMessage.replace("{phase}", MatchPhase.GAME_END.getDisplayName())), false);
		}
	}

	private void handlePhaseTransition() {
		var phase = matchManager.getPhase();
		if (phase == observedPhase) {
			return;
		}

		applyFallDamageGameRule(phase);

		if (phase == MatchPhase.GAME_START) {
			captainSkillUnlockAnnounced = false;
			lastCaptainRevealWarningElapsedSeconds = -1;
			captainManaService.clear();
			laneStructureService.reset();
			laneBiomeService.restoreAll(matchManager.getServer());
			laneRuntimeRegistry.reset();
			capturePointService.resetAll();
			surrenderVoteService.clear();
			unitSpawnQueueService.resetForMatch(matchManager);
			laneRuntimeRegistry.refresh(matchManager.getServer(), configManager.getSystemConfig(), matchManager, capturePointService);
			for (var captainId : matchManager.getCaptainIds()) {
				var factionId = matchManager.getPlayerState(captainId).getFactionId();
				captainManaService.initializeCaptain(captainId, factionId, captainManaRecoverySeconds(matchManager, matchManager.getCaptainTeam(captainId)));
			}
			biomeRevealService.prepareForMatch(
				matchManager.getServer(),
				configManager.getSystemConfig(),
				configManager.getBiomeCatalog(),
				laneBiomeService
			);
			if (configManager.getSystemConfig().biomeReveal.lane1RevealSecond == 0) {
				handleBiomeRevealFollowUp(List.of(LaneId.LANE_1));
			}
			titleDisplayService.showGameStartCountdown(matchManager.getServer(), configManager.getSystemConfig(), configManager.getSystemConfig().gameStart.waitSeconds);
		}
		if (phase == MatchPhase.FACTION_SELECT) {
			factionStructureResetService.start(matchManager.getServerTicks());
			clearLaneEntities();
		} else {
			factionStructureResetService.cancel();
		}
		if (phase == MatchPhase.LOBBY) {
			captainSkillUnlockAnnounced = false;
			lastCaptainRevealWarningElapsedSeconds = -1;
			captainManaService.clear();
			laneStructureService.reset();
			laneRuntimeRegistry.reset();
			capturePointService.resetAll();
			surrenderVoteService.clear();
			unitSpawnQueueService.clear();
			prepareLobbyLaneBiomes();
		}
		if (phase != MatchPhase.GAME_RUNNING) {
			lastCaptainRevealWarningElapsedSeconds = -1;
			surrenderVoteService.clear();
		}
		if (phase == MatchPhase.TEAM_SELECT || phase == MatchPhase.FACTION_SELECT || phase == MatchPhase.GAME_END) {
			captainSkillUnlockAnnounced = false;
			unitSpawnQueueService.clear();
		}

		broadcastPhaseAnnouncement(phase);
		playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
		observedPhase = phase;
	}

	private void prepareLobbyLaneBiomes() {
		biomeRevealService.prepareForLobby(
			matchManager.getServer(),
			configManager.getSystemConfig(),
			configManager.getBiomeCatalog(),
			laneBiomeService
		);
	}

	private BiomeEntry findBiomeEntry(String biomeId) {
		var biomeCatalog = configManager.getBiomeCatalog();
		if (biomeCatalog == null || biomeCatalog.biomes == null) {
			return null;
		}
		return biomeCatalog.biomes.stream()
			.filter(entry -> entry != null && biomeId.equals(entry.id))
			.findFirst()
			.orElse(null);
	}

	private void applyFallDamageGameRule(MatchPhase phase) {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		server.getGameRules().get(GameRules.FALL_DAMAGE).set(isFallDamageEnabledForPhase(phase), server);
	}

	private void tickCaptainMana(net.minecraft.server.MinecraftServer server) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return;
		}

		for (var captainId : matchManager.getCaptainIds()) {
			captainManaService.getOrCreate(captainId);
		}
		var elapsedSeconds = matchManager.getElapsedSeconds();
		captainManaService.tickSecond(
			CaptainManaService.STARTING_MANA,
			elapsedSeconds,
			captainId -> captainManaRecoverySeconds(matchManager, matchManager.getCaptainTeam(captainId))
		);
		if (shouldAnnounceCaptainSkillUnlock(matchManager.getPhase(), elapsedSeconds, captainSkillUnlockAnnounced)) {
			broadcastMessage("이제 사령관 스킬을 사용할 수 있습니다!");
			captainSkillUnlockAnnounced = true;
		}
		for (var captainId : matchManager.getCaptainIds()) {
			var player = server.getPlayerManager().getPlayer(captainId);
			if (player == null) {
				continue;
			}
			captainManaService.getOrCreate(captainId);
		}
	}

	private void refillCaptainMana(int revealCount) {
		var losingTeam = losingTeam();
		for (var teamId : TeamId.values()) {
			if (!CaptainManaService.shouldRefillOnReveal(teamId, matchManager.getRedScore(), matchManager.getBlueScore())) {
				continue;
			}
			var captainId = matchManager.getCaptainId(teamId);
			if (captainId == null) {
				continue;
			}
			if (losingTeam == teamId && revealCount > 0) {
				var state = captainManaService.getOrCreate(captainId);
				state.addBonusMaxMana(1);
			}
			captainManaService.refillMana(captainId, captainManaRecoverySeconds(matchManager, teamId));
		}
	}

	private TeamId losingTeam() {
		if (matchManager.getRedScore() == matchManager.getBlueScore()) {
			return null;
		}
		return matchManager.getRedScore() < matchManager.getBlueScore() ? TeamId.RED : TeamId.BLUE;
	}

	private void handleBiomeRevealFollowUp(List<LaneId> revealed) {
		if (revealed == null || revealed.isEmpty()) {
			return;
		}
		for (var laneId : revealed) {
			if (laneId == LaneId.LANE_1) {
				broadcastLines(configManager.getSystemConfig().biomeReveal.firstRevealGuideMessages);
				continue;
			}
			if (losingTeam() == null) {
				continue;
			}
			broadcastLine(configManager.getSystemConfig().biomeReveal.laterRevealManaMessage);
		}
	}

	private void maybeWarnCaptainsBeforeNextReveal(net.minecraft.server.MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null || matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return;
		}
		var leadSeconds = Math.max(0, systemConfig.biomeReveal.captainWarningLeadSeconds);
		if (leadSeconds <= 0) {
			return;
		}
		var remainingSeconds = biomeRevealService.nextRevealRemainingSeconds(systemConfig);
		var elapsedSeconds = matchManager.getElapsedSeconds();
		if (remainingSeconds == leadSeconds && elapsedSeconds != lastCaptainRevealWarningElapsedSeconds) {
			lastCaptainRevealWarningElapsedSeconds = elapsedSeconds;
			titleDisplayService.showCaptainBiomeRevealWarning(server, systemConfig);
		}
	}

	private void broadcastLines(List<String> lines) {
		if (lines == null || lines.isEmpty() || matchManager.getServer() == null) {
			return;
		}
		for (var line : lines) {
			broadcastLine(line);
		}
	}

	private void broadcastLine(String line) {
		if (line == null || line.isBlank() || matchManager.getServer() == null) {
			return;
		}
		matchManager.getServer().getPlayerManager().broadcast(textTemplateResolver.format(line), false);
	}

	private Path getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}


	private void playCountdownSound(int remainingSeconds) {
		uiSoundService.playCountdownTick(matchManager.getServer(), remainingSeconds <= 5);
	}

	private void playVictoryCountdownSound(int remainingSeconds) {
		uiSoundService.playCountdownTick(matchManager.getServer(), remainingSeconds <= 5);
	}

	private void playFactionSelectionTickSound() {
		if (matchManager.getPhase() != MatchPhase.FACTION_SELECT || matchManager.getServerTicks() % 20L != 0L) {
			return;
		}
		uiSoundService.playCountdownTick(matchManager.getServer(), false);
	}

	private void applyWinnerGlow(TeamId winnerTeam, Integer seconds) {
		if (winnerTeam == null) {
			return;
		}
		for (var player : matchManager.getOnlineTeamPlayers(winnerTeam)) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, seconds * 20, 0, false, false, true));
		}
	}

	private void clearWinnerGlow() {
		for (var player : matchManager.getOnlinePlayers()) {
			player.removeStatusEffect(StatusEffects.GLOWING);
		}
	}

	private void resetOnlinePlayerAttributes() {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			unitSpawnService.getUnitLoadoutService().resetPlayerAttributes(player);
		}
	}

	private void applyLadderRewardsAndRefresh() {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		var statsRepository = configManager.getStatsRepository();
		var beforeLadders = snapshotParticipantLadders(statsRepository);
		if (!shouldSkipGameEndRewards()) {
			ladderRewardService.applyMatchRewards(server, matchManager, statsRepository, configManager.getSystemConfig());
			ladderRewardService.applyServerMatchStats(matchManager, configManager.getServerStatsRepository());
		}
		capturePendingLadderChanges(beforeLadders, statsRepository);
		playerDisplayNameService.refreshAll(server, matchManager, statsRepository, configManager.getSystemConfig());
	}

	private boolean shouldApplyGameEndRewards() {
		return !shouldSkipGameEndRewards();
	}

	private boolean shouldSkipGameEndRewards() {
		if (!suppressNextGameEndRewards) {
			return false;
		}
		suppressNextGameEndRewards = false;
		return true;
	}

	private void tickPlayTimeStats(net.minecraft.server.MinecraftServer server) {
		if (server == null) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (shouldTrackPlayTime(matchManager.getPhase(), state, player.isSpectator())) {
				configManager.getStatsRepository().addPlayTimeSeconds(player.getUuid(), player.getName().getString(), 1);
			}
		}
	}

	private void applyTickRate(int tickRate) {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		try {
			server.getTickManager().setTickRate((float) tickRate);
		} catch (Exception exception) {
			LOGGER.warn("[{}] tick rate 적용 실패: {}", MOD_ID, tickRate, exception);
		}
	}

	private void returnPlayersToLobby() {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		lobbyCoordinator.forcePhase(MatchPhase.LOBBY, server, configManager.getSystemConfig());
		showPendingLadderChanges(server);
	}

	private ServerPlayerEntity resolveProjectileHookOwner(ProjectileEntity projectile) {
		if (projectile == null) {
			return null;
		}
		UUID currentOwnerId = projectile.getOwner() instanceof ServerPlayerEntity player ? player.getUuid() : null;
		UUID taggedOwnerId = GhastUnit.findOriginalOwnerId(projectile.getCommandTags());
		UUID resolvedOwnerId = resolveProjectileHookOwnerId(currentOwnerId, taggedOwnerId);
		if (resolvedOwnerId == null || projectile.getServer() == null) {
			return null;
		}
		return projectile.getServer().getPlayerManager().getPlayer(resolvedOwnerId);
	}

	static UUID resolveProjectileHookOwnerId(UUID currentOwnerId, UUID taggedOwnerId) {
		return taggedOwnerId != null ? taggedOwnerId : currentOwnerId;
	}

	static String formatSignedLadderDelta(int delta) {
		if (delta > 0) {
			return "+" + delta;
		}
		return Integer.toString(delta);
	}

	static String formatLadderChangeSubtitle(int previous, int current) {
		return "(" + previous + ") -> (" + current + ")";
	}

	private String currentSnapMultiplierText() {
		var ladderRewardConfig = configManager.getSystemConfig().ladderReward;
		float multiplier = 1.0f;
		int snapCount = matchManager.getSnapCount();
		if (snapCount == 1) {
			multiplier = ladderRewardConfig.firstSnapMultiplier;
		} else if (snapCount >= 2) {
			multiplier = ladderRewardConfig.doubleSnapMultiplier;
		}
		return String.format(java.util.Locale.ROOT, "%.1f", multiplier);
	}

	private Map<UUID, Integer> snapshotParticipantLadders(StatsRepository statsRepository) {
		var snapshot = new HashMap<UUID, Integer>();
		if (statsRepository == null) {
			return snapshot;
		}
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			var state = entry.getValue();
			if (state.getTeamId() == null || state.getRoleType() == RoleType.NONE || state.getRoleType() == RoleType.SPECTATOR) {
				continue;
			}
			var playerId = entry.getKey();
			snapshot.put(playerId, statsRepository.getLadder(playerId, resolveStatsName(playerId, statsRepository)));
		}
		return snapshot;
	}

	private void capturePendingLadderChanges(Map<UUID, Integer> beforeLadders, StatsRepository statsRepository) {
		pendingGameEndLadderChanges.clear();
		if (statsRepository == null) {
			return;
		}
		for (var entry : beforeLadders.entrySet()) {
			var playerId = entry.getKey();
			var previous = entry.getValue();
			var current = statsRepository.getLadder(playerId, resolveStatsName(playerId, statsRepository));
			pendingGameEndLadderChanges.put(playerId, new LadderChange(previous, current));
		}
	}

	private void showPendingLadderChanges(net.minecraft.server.MinecraftServer server) {
		if (server == null || pendingGameEndLadderChanges.isEmpty()) {
			pendingGameEndLadderChanges.clear();
			return;
		}
		var gameEndConfig = configManager.getSystemConfig().gameEnd;
		for (var player : server.getPlayerManager().getPlayerList()) {
			var change = pendingGameEndLadderChanges.get(player.getUuid());
			if (change == null) {
				continue;
			}
			titleDisplayService.showPersonalTitle(
				player,
				gameEndConfig.ladderDeltaTitleTemplate.replace("{delta}", formatSignedLadderDelta(change.delta())),
				gameEndConfig.ladderDeltaSubtitleTemplate
					.replace("{previous}", Integer.toString(change.previous()))
					.replace("{current}", Integer.toString(change.current()))
			);
		}
		pendingGameEndLadderChanges.clear();
	}

	private String resolveStatsName(UUID playerId, StatsRepository statsRepository) {
		var server = matchManager.getServer();
		if (server != null) {
			var player = server.getPlayerManager().getPlayer(playerId);
			if (player != null) {
				return player.getName().getString();
			}
		}
		return statsRepository.getLastKnownName(playerId, playerId.toString());
	}

	private record LadderChange(int previous, int current) {
		int delta() {
			return current - previous;
		}
	}

	private void setFactionSelectionAndAnnounce(TeamId teamId, FactionId factionId) {
		var previous = matchManager.getFactionSelection(teamId);
		matchManager.setFactionSelection(teamId, factionId);
		if (previous != factionId) {
			broadcastMessage(AnnouncementFormatter.factionSelectionMessage(teamId, factionId, configManager.getSystemConfig().announcements));
		}
	}

	private void broadcastPhaseAnnouncement(MatchPhase phase) {
		broadcastMessage(AnnouncementFormatter.phaseMessage(phase, configManager.getSystemConfig().announcements));
	}

	private void broadcastMessage(String message) {
		var server = matchManager.getServer();
		if (server == null || message == null || message.isBlank()) {
			return;
		}
		server.getPlayerManager().broadcast(textTemplateResolver.format(message), false);
		uiSoundService.playGlobalAnnouncement(server);
	}

	public boolean shouldBlockItemDrop(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		return shouldBlockCaptainItemDrop(matchManager.getPlayerState(player.getUuid()).getRoleType());
	}


}
