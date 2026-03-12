package karn.minecraftsnap;

import karn.minecraftsnap.audio.PhaseMusicService;
import karn.minecraftsnap.biome.BiomeEffectRegistry;
import karn.minecraftsnap.command.McSnapCommandRegistrar;
import karn.minecraftsnap.command.TeamChatService;
import karn.minecraftsnap.config.MinecraftSnapConfigManager;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.AnnouncementFormatter;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.CaptainSelectionService;
import karn.minecraftsnap.game.BiomeRevealService;
import karn.minecraftsnap.game.CaptainManaService;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.FactionSelectionService;
import karn.minecraftsnap.game.GameEndService;
import karn.minecraftsnap.game.InGameRuleService;
import karn.minecraftsnap.game.LaneBiomeService;
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
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.BossBarService;
import karn.minecraftsnap.ui.CaptainSpawnGuiService;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.PlayerDisplayNameService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.ui.WikiGuiService;
import karn.minecraftsnap.unit.UnitClassRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
	private final CaptainManaService captainManaService = new CaptainManaService();
	private final LaneRuntimeRegistry laneRuntimeRegistry = new LaneRuntimeRegistry();
	private final BiomeEffectRegistry biomeEffectRegistry = new BiomeEffectRegistry();
	private final LaneStructureService laneStructureService = new LaneStructureService();
	private final UnitRegistry unitRegistry = new UnitRegistry(false);
	private final UnitClassRegistry unitClassRegistry = new UnitClassRegistry();
	private final AdvanceService advanceService = new AdvanceService(unitRegistry);
	private final UnitLoadoutService unitLoadoutService = new UnitLoadoutService();
	private final UnitAbilityService unitAbilityService = new UnitAbilityService(textTemplateResolver, laneRuntimeRegistry);
	private final UnitSpawnService unitSpawnService = new UnitSpawnService(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService);
	private final WikiGuiService wikiGuiService = new WikiGuiService(textTemplateResolver, unitRegistry, configManager::getBiomeCatalog, matchManager::getPhase);
	private final FactionSelectionGuiService factionSelectionGuiService = new FactionSelectionGuiService(textTemplateResolver, unitRegistry);
	private final PreparationGuiService preparationGuiService = new PreparationGuiService(textTemplateResolver, unitRegistry);
	private final CaptainSpawnGuiService captainSpawnGuiService = new CaptainSpawnGuiService(textTemplateResolver, unitRegistry);
	private final TradeGuiService tradeGuiService = new TradeGuiService(
		textTemplateResolver,
		unitLoadoutService,
		configManager::getShopConfig,
		configManager::getStatsRepository
	);
	private final AdvanceGuiService advanceGuiService = new AdvanceGuiService(textTemplateResolver);
	private final PlayerDisplayNameService playerDisplayNameService = new PlayerDisplayNameService();
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
		playerDisplayNameService
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
		this::returnPlayersToLobby,
		() -> laneBiomeService.restoreAll(matchManager.getServer())
	);
	private CapturePointService capturePointService;
	private BiomeRevealService biomeRevealService;
	private InGameRuleService inGameRuleService;
	private LobbyCoordinator lobbyCoordinator;
	private MatchPhase observedPhase = MatchPhase.LOBBY;

	@Override
	public void onInitializeServer() {
		instance = this;
		LOGGER.info("[{}] 서버사이드 모드 초기화 시작", MOD_ID);
		StyledChatSupport.initialize();
		configManager.load();
		unitRegistry.loadFromFactionConfigs(configManager.getFactionConfigs());
		unitClassRegistry.validateAgainst(unitRegistry);
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		capturePointService.setUnitHookService(unitHookService);
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
			unitHookService
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
			handlePhaseTransition();
			lobbyCoordinator.tick(server, configManager.getSystemConfig());
			capturePointService.tick(server, configManager.getSystemConfig());
			laneRuntimeRegistry.refresh(server, configManager.getSystemConfig(), matchManager, capturePointService);
			var revealed = biomeRevealService.tick(server, configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
			if (!revealed.isEmpty()) {
				refillCaptainMana();
			}
			tickCaptainMana(server);
			unitHookService.tick(server, configManager.getSystemConfig());
			unitAbilityService.tick(server, matchManager);
			unitSpawnService.maintainActiveUnits(matchManager);
			inGameRuleService.tick(server, configManager.getSystemConfig());
			bossBarService.tick(server, configManager.getSystemConfig());
			if (matchManager.getServerTicks() % 20L == 0L) {
					playerDisplayNameService.sync(server, matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
			}
			phaseMusicService.tick(server, configManager.getSystemConfig());
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
			phaseMusicService.handleJoin(handler.getPlayer(), configManager.getSystemConfig());
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
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
			inGameRuleService.allowDamage(entity, source, amount));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
			inGameRuleService.handlePotentialDeath(entity, source, amount));
	}

	public void reload() {
		configManager.getStatsRepository().saveIfDirty();
		configManager.reload();
		unitRegistry.loadFromFactionConfigs(configManager.getFactionConfigs());
		unitClassRegistry.validateAgainst(unitRegistry);
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		capturePointService.setUnitHookService(unitHookService);
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
			unitHookService
		);
		unitSpawnService.setUnitHookService(unitHookService);
		lobbyCoordinator = createLobbyCoordinator();
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
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() == RoleType.CAPTAIN && matchManager.getPhase() == MatchPhase.GAME_RUNNING) {
				openCaptainSpawnGui(player);
				return true;
		}
		if (UnitHookService.canUseUnitActions(state.getRoleType(), state.getCurrentUnitId(), player.isSpectator())) {
			return unitHookService.handleShiftF(player, configManager.getSystemConfig());
		}
		return lobbyCoordinator.handleShortcut(player, configManager.getSystemConfig());
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
		captainManaService.refillMana(player.getUuid());
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

	public boolean forceAdvance(ServerPlayerEntity player) {
		return player != null && advanceService.forceAdvance(matchManager.getPlayerState(player.getUuid()));
	}

	public String forceAssignUnit(ServerPlayerEntity player, String unitId) {
		if (player == null) {
			return "&c플레이어를 찾지 못함";
		}
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getTeamId() == null) {
			return "&c팀이 없는 플레이어는 유닛 강제 배정 불가";
		}
		var definition = unitRegistry.get(unitId);
		if (definition == null) {
			return "&c알 수 없는 유닛";
		}

		var selectedFaction = state.getFactionId() != null ? state.getFactionId() : matchManager.getFactionSelection(state.getTeamId());
		if (selectedFaction != null && selectedFaction != definition.factionId()) {
			return "&c현재 팩션과 다른 유닛은 강제 배정 불가";
		}

		matchManager.setRole(player, state.getTeamId(), RoleType.UNIT);
		if (state.getFactionId() == null) {
			state.setFactionId(definition.factionId());
		}
		matchManager.setCurrentUnit(player.getUuid(), definition.id());
		unitAbilityService.clearPlayerState(player.getUuid());
		if (player.isSpectator()) {
			player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
		}
		unitHookService.applyLoadout(player, definition, configManager.getSystemConfig());
		karn.minecraftsnap.integration.DisguiseSupport.applyDisguise(player, definition.disguise());
		playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, configManager.getStatsRepository(), configManager.getSystemConfig());
		return "&a유닛 강제 배정 완료: &f" + definition.displayName();
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
		if (state.getRoleType() == RoleType.CAPTAIN) {
			if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
				return false;
			}
			if (unitLoadoutService.isCaptainMenuItem(stack)) {
				openCaptainSpawnGui(player);
				return true;
			}
			if (unitLoadoutService.isCaptainSkillItem(stack)) {
				return unitAbilityService.useCaptainSkill(player, matchManager, captainManaService);
			}
			return false;
		}

		if (UnitHookService.canUseUnitActions(state.getRoleType(), state.getCurrentUnitId(), player.isSpectator())
			&& unitLoadoutService.isUnitAbilityItem(stack, state.getCurrentUnitId())) {
			return unitHookService.handleSkillUse(player, configManager.getSystemConfig());
		}
		return false;
	}

	private void openCaptainSpawnGui(net.minecraft.server.network.ServerPlayerEntity player) {
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getFactionId() == null) {
			player.sendMessage(textTemplateResolver.format("&c팩션이 정해지지 않았음"), false);
			return;
		}

		captainSpawnGuiService.open(
			player,
			state.getFactionId(),
			captainManaService.getOrCreate(player.getUuid()),
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
			for (var captainId : matchManager.getCaptainIds()) {
				captainManaService.initializeCaptain(captainId);
			}
			biomeRevealService.prepareForMatch(matchManager.getServer(), configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
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
		captainManaService.tickSecond(CaptainManaService.STARTING_MANA, elapsedSeconds);
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
			captainManaService.refillMana(captainId);
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
	}
}
