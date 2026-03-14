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
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.AnnouncementFormatter;
import karn.minecraftsnap.game.FactionId;
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
import karn.minecraftsnap.game.UnitSpawnService;
import karn.minecraftsnap.game.VictoryCountdownService;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.BossBarService;
import karn.minecraftsnap.ui.CaptainSpawnGuiService;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.ui.CaptureHudService;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.PlayerDisplayNameService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.ui.UnitHudService;
import karn.minecraftsnap.ui.WikiGuiService;
import karn.minecraftsnap.unit.UnitClassRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
	private final UnitAbilityService unitAbilityService = new UnitAbilityService(textTemplateResolver, laneRuntimeRegistry);
	private final UnitSpawnService unitSpawnService = new UnitSpawnService(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService, uiSoundService);
	private final WikiGuiService wikiGuiService = new WikiGuiService(textTemplateResolver, unitRegistry, configManager::getBiomeCatalog, matchManager::getPhase, uiSoundService);
	private final FactionSelectionGuiService factionSelectionGuiService = new FactionSelectionGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final PreparationGuiService preparationGuiService = new PreparationGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final CaptainSpawnGuiService captainSpawnGuiService = new CaptainSpawnGuiService(textTemplateResolver, unitRegistry, uiSoundService);
	private final CaptainWeatherGuiService captainWeatherGuiService = new CaptainWeatherGuiService(textTemplateResolver, uiSoundService);
	private final TradeGuiService tradeGuiService = new TradeGuiService(
		textTemplateResolver,
		unitLoadoutService,
		configManager::getShopConfig,
		configManager::getStatsRepository,
		uiSoundService
	);
	private final AdvanceGuiService advanceGuiService = new AdvanceGuiService(textTemplateResolver, uiSoundService);
	private final PlayerDisplayNameService playerDisplayNameService = new PlayerDisplayNameService();
	private final UnitHudService unitHudService = new UnitHudService(matchManager, unitRegistry, unitAbilityService, textTemplateResolver);
	private final CaptureHudService captureHudService = new CaptureHudService(matchManager, textTemplateResolver);
	private final GameStartCountdownService gameStartCountdownService = new GameStartCountdownService(matchManager, this::showGameStartCountdown, this::playCountdownSound);
	private final VictoryCountdownService victoryCountdownService = new VictoryCountdownService(matchManager, this::showVictoryCountdown, this::playVictoryCountdownSound);
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
		uiSoundService
	);
	private final LobbyScoreboardService lobbyScoreboardService = new LobbyScoreboardService(textTemplateResolver);
	private final McSnapCommandRegistrar commandRegistrar = new McSnapCommandRegistrar(this);
	private final LaneBiomeService laneBiomeService = new LaneBiomeService();
	private final GameEndService gameEndService = new GameEndService(
		matchManager,
		textTemplateResolver,
		this::showGameTitle,
		this::applyWinnerGlow,
		this::clearWinnerGlow,
		this::applyTickRate,
		this::applyCaptainLadderRewards,
		this::returnPlayersToLobby,
		() -> laneBiomeService.restoreAll(matchManager.getServer())
	);
	private CapturePointService capturePointService;
	private BiomeRevealService biomeRevealService;
	private InGameRuleService inGameRuleService;
	private LobbyCoordinator lobbyCoordinator;
	private CaptainSkillService captainSkillService;
	private MatchPhase observedPhase = MatchPhase.LOBBY;

	@Override
	public void onInitializeServer() {
		instance = this;
		LOGGER.info("[{}] 서버사이드 모드 초기화 시작", MOD_ID);
		StyledChatSupport.initialize();
		registerOverlaySounds();
		configManager.load();
		unitRegistry.loadFromConfiguredClasses(unitClassRegistry);
		unitClassRegistry.validateAgainst(unitRegistry);
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository(), uiSoundService, textTemplateResolver);
		capturePointService.setUnitHookService(unitHookService);
		capturePointService.setLaneRuntimeRegistry(laneRuntimeRegistry);
		captainSkillService = new CaptainSkillService(
			matchManager,
			laneRuntimeRegistry,
			captainWeatherGuiService,
			captainManaService,
			textTemplateResolver,
			uiSoundService
		);
		unitAbilityService.setCaptainSkillService(captainSkillService);
		unitSpawnService.setCaptainSkillService(captainSkillService);
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
			unitSpawnService,
			captainManaService,
			unitRegistry,
			unitAbilityService,
			laneRuntimeRegistry,
			unitHookService,
			uiSoundService
		);
		unitSpawnService.setUnitHookService(unitHookService);
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
			new MinecraftSnapResourcePackConfigurer(server, LOGGER).applyDefaults();
			LOGGER.info("[{}] 서버 시작 완료", MOD_ID);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> configManager.getStatsRepository().save());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			matchManager.tick();
			handlePhaseTransition();
			lobbyCoordinator.tick(server, configManager.getSystemConfig());
			gameStartCountdownService.tick(configManager.getSystemConfig());
			capturePointService.tick(server, configManager.getSystemConfig());
			captureHudService.tick(server, configManager.getSystemConfig(), capturePointService);
			laneRuntimeRegistry.refresh(server, configManager.getSystemConfig(), matchManager, capturePointService);
			var revealed = biomeRevealService.tick(server, configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
			laneBiomeService.tick(server);
			if (!revealed.isEmpty()) {
				refillCaptainMana();
			}
			tickCaptainMana(server);
			if (captainSkillService != null) {
				captainSkillService.tick(server, configManager.getSystemConfig());
			}
			unitHookService.tick(server, configManager.getSystemConfig());
			unitAbilityService.tick(server, matchManager);
			unitHudService.tick(server, configManager.getSystemConfig());
			unitSpawnService.maintainActiveUnits(matchManager);
			inGameRuleService.tick(server, configManager.getSystemConfig());
			victoryCountdownService.tick(configManager.getSystemConfig());
			bossBarService.tick(server, configManager.getSystemConfig());
			playFactionSelectionTickSound();
			if (matchManager.getServerTicks() % 20L == 0L) {
					playerDisplayNameService.sync(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
			}
			phaseMusicService.tick(server);
			gameEndService.tick(configManager.getSystemConfig());
			if (matchManager.getServerTicks() % 200L == 0L) {
				configManager.getStatsRepository().saveIfDirty();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			matchManager.handleJoin(handler.getPlayer());
			var stats = getStatsRepository().getOrCreate(handler.getPlayer().getUuid(), handler.getPlayer().getName().getString());
			matchManager.syncPersistentState(handler.getPlayer().getUuid(), stats.emeralds, stats.goldIngots);
			lobbyCoordinator.handleJoin(handler.getPlayer(), configManager.getSystemConfig());
				playerDisplayNameService.refreshAll(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
			phaseMusicService.handleJoin(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			matchManager.handleDisconnect(handler.getPlayer());
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
		configManager.reload();
		unitRegistry.loadFromConfiguredClasses(unitClassRegistry);
		unitClassRegistry.validateAgainst(unitRegistry);
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository(), uiSoundService, textTemplateResolver);
		capturePointService.setUnitHookService(unitHookService);
		capturePointService.setLaneRuntimeRegistry(laneRuntimeRegistry);
		captainSkillService = new CaptainSkillService(
			matchManager,
			laneRuntimeRegistry,
			captainWeatherGuiService,
			captainManaService,
			textTemplateResolver,
			uiSoundService
		);
		unitAbilityService.setCaptainSkillService(captainSkillService);
		unitSpawnService.setCaptainSkillService(captainSkillService);
		laneStructureService.reset();
		laneRuntimeRegistry.reset();
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
			unitSpawnService,
			captainManaService,
			unitRegistry,
			unitAbilityService,
			laneRuntimeRegistry,
			unitHookService,
			uiSoundService
		);
		unitSpawnService.setUnitHookService(unitHookService);
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

	public karn.minecraftsnap.config.BiomeCatalog getBiomeCatalog() {
		return configManager.getBiomeCatalog();
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
		var state = matchManager.getPlayerState(player.getUuid());
		if (UnitHookService.canUseUnitActions(state.getRoleType(), state.getCurrentUnitId(), player.isSpectator())) {
			return unitHookService.handleShiftF(player, configManager.getSystemConfig());
		}
		return lobbyCoordinator.handleShortcut(player, configManager.getSystemConfig());
	}

	public boolean shouldCancelPlayerMove(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		if (isMovementLocked(player)) {
			return true;
		}
		return isSlimeJumpLocked(player, packet);
	}

	public boolean isMovementLocked(net.minecraft.server.network.ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.UNIT) {
			return false;
		}
		var unitId = state.getCurrentUnitId();
		if (!"creeper".equals(unitId) && !"charged_creeper".equals(unitId)) {
			return false;
		}
		return state.getUnitRuntimeLong("creeper_bomb_tick") != null;
	}

	private boolean isSlimeJumpLocked(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket packet) {
		if (player == null || packet == null || !player.isOnGround()) {
			return false;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.UNIT) {
			return false;
		}
		var unitId = state.getCurrentUnitId();
		if (!"slime".equals(unitId) && !"giant_slime".equals(unitId)) {
			return false;
		}
		if (player.getVelocity().y > 0.05D) {
			return false;
		}
		return packet.getY(player.getY()) > player.getY() + 0.05D;
	}

	public static MinecraftSnap getInstance() {
		return instance;
	}

	public void setLaneRevealState(karn.minecraftsnap.game.LaneId laneId, boolean revealed) {
		lobbyCoordinator.setLaneRevealState(laneId, revealed);
	}

	public boolean chargeCaptainMana(ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() != RoleType.CAPTAIN) {
			return false;
		}
		captainManaService.refillMana(player.getUuid(), configManager.getSystemConfig().inGame.captainManaRecoverySeconds);
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

	public void handleLivingDamageApplied(net.minecraft.entity.LivingEntity entity, net.minecraft.entity.damage.DamageSource source, boolean damaged) {
		if (inGameRuleService != null) {
			inGameRuleService.handleDamageApplied(entity, source, damaged);
		}
	}

	public String spawnBots(net.minecraft.server.command.ServerCommandSource source, int count) {
		var textConfig = getTextConfig();
		if (source == null) {
			return "&c명령 실행 소스를 찾지 못함";
		}
		if (count <= 0) {
			return textConfig.adminBotsInvalidCountMessage;
		}
		var commandManager = source.getServer().getCommandManager();
		int created = 0;
		int nextIndex = 1;
		while (created < count && nextIndex <= count + 256) {
			var name = "BOT" + nextIndex;
			nextIndex++;
			if (source.getServer().getPlayerManager().getPlayer(name) != null) {
				continue;
			}
			commandManager.executeWithPrefix(source, "player " + name + " spawn");
			created++;
		}
		return created == count
			? textConfig.adminBotsSuccessMessage.replace("{count}", Integer.toString(created))
			: textConfig.adminBotsPartialMessage
				.replace("{created}", Integer.toString(created))
				.replace("{requested}", Integer.toString(count));
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
		if (player == null) {
			return "&c플레이어를 찾지 못함";
		}
		var state = matchManager.getPlayerState(player.getUuid());
		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			return "&c알 수 없는 유닛";
		}

		matchManager.setRole(player, state.getTeamId(), RoleType.UNIT);
		if (player.isSpectator()) {
			player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
		}
		unitHookService.assignUnit(player, definition, configManager.getSystemConfig());
		playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
		return "&a유닛 강제 배정 완료: &f" + definition.displayName();
	}

	public String placeNearestBiomeStructure(ServerPlayerEntity player) {
		return placeNearestBiomeStructure(player, null);
	}

	public String placeNearestBiomeStructure(ServerPlayerEntity player, String structureId) {
		if (player == null) {
			return "&c플레이어를 찾지 못함";
		}
		var laneId = UnitSpawnService.nearestLaneForCaptain(player, configManager.getSystemConfig());
		var resolvedStructureId = structureId;
		if (resolvedStructureId == null || resolvedStructureId.isBlank()) {
			var assignedBiomeId = matchManager.getAssignedBiomeId(laneId);
			if (assignedBiomeId == null || assignedBiomeId.isBlank()) {
				return "&c" + laneLabel(laneId) + "에 배정된 바이옴이 없음";
			}
			var biomeEntry = configManager.getBiomeCatalog().biomes.stream()
				.filter(entry -> assignedBiomeId.equals(entry.id))
				.findFirst()
				.orElse(null);
			if (biomeEntry == null || biomeEntry.structureId == null || biomeEntry.structureId.isBlank()) {
				return "&c" + laneLabel(laneId) + "의 structureId 가 비어 있음";
			}
			resolvedStructureId = biomeEntry.structureId;
		}
		var placed = laneStructureService.forcePlaceStructure(
			matchManager.getServer(),
			configManager.getSystemConfig().world,
			laneId,
			resolvedStructureId,
			laneStructureService.originFor(laneRegionOf(laneId))
		);
		return placed
			? "&a" + laneLabel(laneId) + " 구조물 설치 완료: &f" + resolvedStructureId
			: "&c" + laneLabel(laneId) + " 구조물 설치 실패";
	}

	public String resetAllBiomeStructures() {
		var server = matchManager.getServer();
		if (server == null) {
			return "&c서버가 아직 바인딩되지 않음";
		}
		int success = 0;
		for (var laneId : LaneId.values()) {
			if (laneStructureService.forcePlaceStructure(
				server,
				configManager.getSystemConfig().world,
				laneId,
				"minecraft:default",
				laneStructureService.originFor(laneRegionOf(laneId))
			)) {
				success++;
			}
		}
		laneStructureService.reset();
		return success == LaneId.values().length
			? "&a모든 라인 구조물을 minecraft:default 로 초기화 완료"
			: "&c일부 라인 구조물 초기화 실패 (&f" + success + "&7/&f" + LaneId.values().length + "&c)";
	}

	public String openAdminGui(ServerPlayerEntity player, String guiId) {
		var state = matchManager.getPlayerState(player.getUuid());
			return switch (guiId) {
			case "wiki" -> {
				wikiGuiService.open(player, matchManager.getPhase());
				yield "&a위키 GUI 오픈";
			}
				case "faction" -> {
					var teamId = state.getTeamId() == null ? TeamId.RED : state.getTeamId();
					factionSelectionGuiService.open(player, teamId, matchManager.getFactionSelection(teamId), factionId -> {
						if (state.getTeamId() != null) {
							setFactionSelectionAndAnnounce(state.getTeamId(), factionId);
						}
					});
					yield "&a팩션 GUI 오픈";
			}
			case "preparation" -> {
				preparationGuiService.open(player, state);
				yield "&a준비 GUI 오픈";
			}
			case "captain_spawn" -> {
				openCaptainSpawnGui(player);
				yield "&a사령관 소환 GUI 오픈";
			}
			case "trade" -> {
				tradeGuiService.open(player, state);
				yield "&a거래 GUI 오픈";
			}
			case "advance" -> {
				openAdvanceGui(player, state);
				yield "&a전직 GUI 오픈";
			}
			default -> "&c지원하지 않는 GUI";
		};
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
				preparationGuiService,
				unitSpawnService,
				textTemplateResolver,
				this::setFactionSelectionAndAnnounce
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

	private void openCaptainSpawnGui(net.minecraft.server.network.ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getFactionId() == null) {
			uiSoundService.playUiDeny(player);
			player.sendMessage(textTemplateResolver.format(configManager.getSystemConfig().gameStart.captainSpawnNoFactionMessage), false);
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

	private void handlePhaseTransition() {
		var phase = matchManager.getPhase();
		if (phase == observedPhase) {
			return;
		}

		if (phase == MatchPhase.GAME_START) {
			captainManaService.clear();
			laneStructureService.reset();
			laneRuntimeRegistry.reset();
			laneRuntimeRegistry.refresh(matchManager.getServer(), configManager.getSystemConfig(), matchManager, capturePointService);
			resetAllBiomeStructures();
			for (var captainId : matchManager.getCaptainIds()) {
				captainManaService.initializeCaptain(captainId, configManager.getSystemConfig().inGame.captainManaRecoverySeconds);
			}
			biomeRevealService.prepareForMatch(matchManager.getServer(), configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
			showGameStartCountdown(configManager.getSystemConfig().gameStart.waitSeconds);
		}
		if (phase == MatchPhase.LOBBY) {
			captainManaService.clear();
			laneStructureService.reset();
			laneRuntimeRegistry.reset();
		}

		broadcastPhaseAnnouncement(phase);
		playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
		observedPhase = phase;
	}

	private void tickCaptainMana(net.minecraft.server.MinecraftServer server) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return;
		}

		for (var captainId : matchManager.getCaptainIds()) {
			captainManaService.getOrCreate(captainId);
		}
		var elapsedSeconds = matchManager.getTotalSeconds() - matchManager.getRemainingSeconds();
		captainManaService.tickSecond(CaptainManaService.STARTING_MANA, elapsedSeconds, configManager.getSystemConfig().inGame.captainManaRecoverySeconds);
		for (var captainId : matchManager.getCaptainIds()) {
			var player = server.getPlayerManager().getPlayer(captainId);
			if (player == null) {
				continue;
			}
			var captainState = captainManaService.getOrCreate(captainId);
			player.sendMessage(textTemplateResolver.format(
				"&b마나 &f" + captainState.getCurrentMana()
					+ "&7/&f" + captainState.getMaxMana()
					+ " &8| &7회복 " + captainState.getSecondsUntilNextMana() + "초"
					+ " &8| &7소환쿨 " + captainState.getSpawnCooldownSeconds() + "초"
					+ " &8| &7스킬쿨 " + captainState.getSkillCooldownSeconds() + "초"
			), true);
		}
	}

	private void refillCaptainMana() {
		for (var captainId : matchManager.getCaptainIds()) {
			captainManaService.refillMana(captainId, configManager.getSystemConfig().inGame.captainManaRecoverySeconds);
		}
	}

	private Path getConfigDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
	}

	private void showGameTitle(String message) {
		var server = matchManager.getServer();
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

	private void showGameStartCountdown(int remainingSeconds) {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		var systemConfig = configManager.getSystemConfig();
		var title = textTemplateResolver.format(systemConfig.gameStart.countdownTitle);
		var subtitle = textTemplateResolver.format(systemConfig.gameStart.countdownSubtitleTemplate.replace("{seconds}", String.valueOf(remainingSeconds)));
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new ClearTitleS2CPacket(false));
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 0));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
	}

	private void showVictoryCountdown(TeamId teamId, int remainingSeconds) {
		var server = matchManager.getServer();
		if (server == null || teamId == null || remainingSeconds <= 0) {
			return;
		}
		var template = configManager.getSystemConfig().gameEnd.victoryCountdownSubtitleTemplate;
		var subtitle = textTemplateResolver.format(template
			.replace("{team}", teamId.getDisplayName())
			.replace("{seconds}", String.valueOf(remainingSeconds)));
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
		}
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

	private void applyCaptainLadderRewards() {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		var winnerTeam = matchManager.getWinnerTeam();
		if (winnerTeam == null) {
			return;
		}
		var loserTeam = winnerTeam == TeamId.RED ? TeamId.BLUE : TeamId.RED;
		var winnerCaptainId = matchManager.getCaptainId(winnerTeam);
		var loserCaptainId = matchManager.getCaptainId(loserTeam);
		var totalScore = matchManager.getRedScore() + matchManager.getBlueScore();
		if (winnerCaptainId != null) {
			var player = server.getPlayerManager().getPlayer(winnerCaptainId);
			var name = player == null ? winnerCaptainId.toString() : player.getName().getString();
			configManager.getStatsRepository().addLadder(winnerCaptainId, name, totalScore);
		}
		if (loserCaptainId != null) {
			var player = server.getPlayerManager().getPlayer(loserCaptainId);
			var name = player == null ? loserCaptainId.toString() : player.getName().getString();
			var penalty = loserTeam == TeamId.RED ? matchManager.getRedScore() : matchManager.getBlueScore();
			configManager.getStatsRepository().addLadder(loserCaptainId, name, -penalty);
		}
		applyUnitLadderRewards(server, winnerTeam, loserTeam);
		playerDisplayNameService.refreshAll(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
	}

	private void applyUnitLadderRewards(net.minecraft.server.MinecraftServer server, TeamId winnerTeam, TeamId loserTeam) {
		applyTeamUnitLadder(server, winnerTeam, 1);
		applyTeamUnitLadder(server, loserTeam, -1);
	}

	private void applyTeamUnitLadder(net.minecraft.server.MinecraftServer server, TeamId teamId, int sign) {
		var unitEntries = matchManager.getPlayerStatesSnapshot().entrySet().stream()
			.filter(entry -> {
				var state = entry.getValue();
				return state.getTeamId() == teamId && state.getRoleType() == RoleType.UNIT;
			})
			.toList();
		if (unitEntries.isEmpty()) {
			return;
		}
		int maxPerformance = unitEntries.stream()
			.mapToInt(entry -> entry.getValue().getMatchKills() + entry.getValue().getMatchCaptureScore())
			.max()
			.orElse(0);
		for (var entry : unitEntries) {
			var playerId = entry.getKey();
			var state = entry.getValue();
			int performance = state.getMatchKills() + state.getMatchCaptureScore();
			int amount = unitMatchLadderAmount(performance, maxPerformance);
			var player = server.getPlayerManager().getPlayer(playerId);
			var name = player == null
				? configManager.getStatsRepository().getLastKnownName(playerId, playerId.toString())
				: player.getName().getString();
			configManager.getStatsRepository().addLadder(playerId, name, amount * sign);
		}
	}

	static int unitMatchLadderAmount(int performance, int maxPerformance) {
		if (maxPerformance <= 0 || performance <= 0) {
			return 10;
		}
		return Math.max(10, Math.min(20, 10 + Math.round(10.0f * performance / maxPerformance)));
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

	private SystemConfig.LaneRegionConfig laneRegionOf(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> configManager.getSystemConfig().inGame.lane1Region;
			case LANE_2 -> configManager.getSystemConfig().inGame.lane2Region;
			case LANE_3 -> configManager.getSystemConfig().inGame.lane3Region;
		};
	}

	private String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1번 라인";
			case LANE_2 -> "2번 라인";
			case LANE_3 -> "3번 라인";
		};
	}
}
