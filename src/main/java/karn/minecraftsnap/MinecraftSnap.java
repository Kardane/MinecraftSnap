package karn.minecraftsnap;

import karn.minecraftsnap.audio.PhaseMusicService;
import karn.minecraftsnap.command.McSnapCommandRegistrar;
import karn.minecraftsnap.command.TeamChatService;
import karn.minecraftsnap.config.MinecraftSnapConfigManager;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.CaptainSelectionService;
import karn.minecraftsnap.game.BiomeRevealService;
import karn.minecraftsnap.game.CaptainManaService;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.FactionSelectionService;
import karn.minecraftsnap.game.GameEndService;
import karn.minecraftsnap.game.InGameRuleService;
import karn.minecraftsnap.game.LaneBiomeService;
import karn.minecraftsnap.game.LobbyCoordinator;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.TeamAssignmentService;
import karn.minecraftsnap.game.UnitAbilityService;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.game.UnitSpawnService;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.BossBarService;
import karn.minecraftsnap.ui.CaptainSpawnGuiService;
import karn.minecraftsnap.ui.FactionSelectionGuiService;
import karn.minecraftsnap.ui.LobbyScoreboardService;
import karn.minecraftsnap.ui.PreparationGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.ui.WikiGuiService;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.ActionResult;
import net.minecraft.server.network.ServerPlayerEntity;
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
	private final UnitRegistry unitRegistry = new UnitRegistry(false);
	private final AdvanceService advanceService = new AdvanceService(unitRegistry);
	private final UnitLoadoutService unitLoadoutService = new UnitLoadoutService();
	private final UnitAbilityService unitAbilityService = new UnitAbilityService(textTemplateResolver);
	private final UnitSpawnService unitSpawnService = new UnitSpawnService(captainManaService, unitRegistry, unitLoadoutService, unitAbilityService);
	private final WikiGuiService wikiGuiService = new WikiGuiService(textTemplateResolver, unitRegistry, configManager::getBiomeCatalog);
	private final FactionSelectionGuiService factionSelectionGuiService = new FactionSelectionGuiService(textTemplateResolver, unitRegistry);
	private final PreparationGuiService preparationGuiService = new PreparationGuiService(textTemplateResolver, unitRegistry);
	private final CaptainSpawnGuiService captainSpawnGuiService = new CaptainSpawnGuiService(textTemplateResolver, unitRegistry);
	private final TradeGuiService tradeGuiService = new TradeGuiService(textTemplateResolver);
	private final AdvanceGuiService advanceGuiService = new AdvanceGuiService(textTemplateResolver);
	private final LobbyScoreboardService lobbyScoreboardService = new LobbyScoreboardService(textTemplateResolver);
	private final McSnapCommandRegistrar commandRegistrar = new McSnapCommandRegistrar(this);
	private final LaneBiomeService laneBiomeService = new LaneBiomeService();
	private final GameEndService gameEndService = new GameEndService(
		matchManager,
		textTemplateResolver,
		this::broadcastGameMessage,
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
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		biomeRevealService = new BiomeRevealService(matchManager, textTemplateResolver);
		inGameRuleService = new InGameRuleService(
			matchManager,
			configManager.getStatsRepository(),
			textTemplateResolver,
			unitSpawnService,
			captainManaService,
			unitRegistry,
			unitAbilityService
		);
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
			var revealed = biomeRevealService.tick(server, configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
			if (!revealed.isEmpty()) {
				refillCaptainMana();
			}
			tickCaptainMana(server);
			advanceService.tick(server, matchManager, configManager.getSystemConfig().advance);
			unitAbilityService.tick(server, matchManager);
			unitSpawnService.maintainActiveUnits(matchManager);
			inGameRuleService.tick(server, configManager.getSystemConfig());
			capturePointService.tick(server, configManager.getSystemConfig());
			bossBarService.tick(server, configManager.getSystemConfig());
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
			inGameRuleService.allowDamage(entity, source));
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
			inGameRuleService.handlePotentialDeath(entity, source, amount));
	}

	public void reload() {
		configManager.getStatsRepository().saveIfDirty();
		configManager.reload();
		unitRegistry.loadFromFactionConfigs(configManager.getFactionConfigs());
		matchManager.applyGameDuration(configManager.getSystemConfig().gameDurationSeconds);
		capturePointService = new CapturePointService(matchManager, configManager.getStatsRepository());
		biomeRevealService = new BiomeRevealService(matchManager, textTemplateResolver);
		inGameRuleService = new InGameRuleService(
			matchManager,
			configManager.getStatsRepository(),
			textTemplateResolver,
			unitSpawnService,
			captainManaService,
			unitRegistry,
			unitAbilityService
		);
		lobbyCoordinator = createLobbyCoordinator();
		for (var player : matchManager.getOnlinePlayers()) {
			var stats = getStatsRepository().getOrCreate(player.getUuid(), player.getName().getString());
			matchManager.syncPersistentState(player.getUuid(), stats.emeralds, stats.goldIngots);
		}
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
		if (matchManager.getPhase() == MatchPhase.GAME_RUNNING) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getRoleType() == RoleType.CAPTAIN) {
				openCaptainSpawnGui(player);
				return true;
			}
			if (state.getRoleType() == RoleType.UNIT && state.getCurrentUnitId() != null) {
				if (state.getFactionId() == karn.minecraftsnap.game.FactionId.VILLAGER
					|| state.getFactionId() == karn.minecraftsnap.game.FactionId.NETHER) {
					tradeGuiService.open(player, state);
					return true;
				}
				if (state.getFactionId() == karn.minecraftsnap.game.FactionId.MONSTER) {
					openAdvanceGui(player, state);
					return true;
				}
			}
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

	public CaptainManaService getCaptainManaService() {
		return captainManaService;
	}

	public SystemConfig getSystemConfig() {
		return configManager.getSystemConfig();
	}

	public boolean forceAdvance(ServerPlayerEntity player) {
		return player != null && advanceService.forceAdvance(matchManager.getPlayerState(player.getUuid()), configManager.getSystemConfig().advance);
	}

	public String openAdminGui(ServerPlayerEntity player, String guiId) {
		var state = matchManager.getPlayerState(player.getUuid());
		return switch (guiId) {
			case "wiki" -> {
				wikiGuiService.open(player, matchManager.getPhase());
				yield "&a위키 GUI 오픈";
			}
			case "faction" -> {
				if (matchManager.getPhase() != MatchPhase.FACTION_SELECT || state.getRoleType() != RoleType.CAPTAIN || state.getTeamId() == null) {
					yield "&c팩션 GUI는 팩션 선택 페이즈의 사령관만 사용 가능";
				}
				factionSelectionGuiService.open(player, state.getTeamId(), matchManager.getFactionSelection(state.getTeamId()), factionId -> matchManager.setFactionSelection(state.getTeamId(), factionId));
				yield "&a팩션 GUI 오픈";
			}
			case "preparation" -> {
				if (matchManager.getPhase() != MatchPhase.GAME_START) {
					yield "&c준비 GUI는 게임 준비 페이즈에서만 사용 가능";
				}
				preparationGuiService.open(player, state);
				yield "&a준비 GUI 오픈";
			}
			case "captain_spawn" -> {
				if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || state.getRoleType() != RoleType.CAPTAIN) {
					yield "&c사령관 소환 GUI는 게임 진행 중 사령관만 사용 가능";
				}
				openCaptainSpawnGui(player);
				yield "&a사령관 소환 GUI 오픈";
			}
			case "trade" -> {
				if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || (state.getFactionId() != FactionId.VILLAGER && state.getFactionId() != FactionId.NETHER)) {
					yield "&c거래 GUI는 게임 진행 중 주민/네더 유닛만 사용 가능";
				}
				tradeGuiService.open(player, state);
				yield "&a거래 GUI 오픈";
			}
			case "advance" -> {
				if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || state.getFactionId() != FactionId.MONSTER) {
					yield "&c전직 GUI는 게임 진행 중 몬스터 유닛만 사용 가능";
				}
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
			textTemplateResolver
		);
	}

	private boolean handleUseItem(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.util.Hand hand) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return false;
		}

		var stack = player.getStackInHand(hand);
		var state = matchManager.getPlayerState(player.getUuid());
		if (state.getRoleType() == RoleType.CAPTAIN) {
			if (unitLoadoutService.isCaptainMenuItem(stack)) {
				openCaptainSpawnGui(player);
				return true;
			}
			if (unitLoadoutService.isCaptainSkillItem(stack)) {
				return unitAbilityService.useCaptainSkill(player, matchManager, captainManaService);
			}
			return false;
		}

		if (state.getCurrentUnitId() != null && unitLoadoutService.isUnitAbilityItem(stack, state.getCurrentUnitId())) {
			return unitAbilityService.useUnitAbility(player, matchManager, unitRegistry);
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
		var condition = advanceService.findCondition(state.getCurrentUnitId(), configManager.getSystemConfig().advance);
		var biomeId = player.getWorld().getBiome(player.getBlockPos()).getKey()
			.map(key -> key.getValue().toString())
			.orElse("minecraft:plains");
		var weather = advanceService.currentWeather(player);
		var targetDefinition = state.getAdvanceTargetUnitId() == null ? null : unitRegistry.get(state.getAdvanceTargetUnitId());
		if (targetDefinition == null && condition != null) {
			targetDefinition = unitRegistry.get(condition.resultUnitId);
		}
		var requiredSeconds = condition == null ? 0 : condition.requiredSeconds;

		advanceGuiService.open(player, state, biomeId, weather, requiredSeconds, targetDefinition, () -> {
			var definition = advanceService.applyAdvance(state);
			if (definition == null) {
				player.sendMessage(textTemplateResolver.format(configManager.getSystemConfig().advance.notAvailableMessage), false);
				return;
			}
			unitLoadoutService.applyUnitLoadout(player, definition, textTemplateResolver);
			karn.minecraftsnap.integration.DisguiseSupport.applyDisguise(player, definition.disguiseId());
			player.sendMessage(textTemplateResolver.format("&a전직 완료: &f" + definition.displayName()), false);
		});
	}

	private void handlePhaseTransition() {
		var phase = matchManager.getPhase();
		if (phase == observedPhase) {
			return;
		}

		if (phase == MatchPhase.GAME_START) {
			captainManaService.clear();
			for (var captainId : matchManager.getCaptainIds()) {
				captainManaService.initializeCaptain(captainId);
			}
			biomeRevealService.prepareForMatch(matchManager.getServer(), configManager.getSystemConfig(), configManager.getBiomeCatalog(), laneBiomeService);
		}
		if (phase == MatchPhase.LOBBY) {
			captainManaService.clear();
		}

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

	private void broadcastGameMessage(String message) {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		server.getPlayerManager().broadcast(textTemplateResolver.format(configManager.getSystemConfig().prefix + message), false);
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
			server.getCommandManager().executeWithPrefix(server.getCommandSource().withLevel(4), "tick rate " + tickRate);
		} catch (Exception exception) {
			LOGGER.warn("[{}] tick rate 명령 실행 실패: {}", MOD_ID, tickRate, exception);
		}
	}

	private void returnPlayersToLobby() {
		var server = matchManager.getServer();
		if (server == null) {
			return;
		}
		lobbyCoordinator.forcePhase(MatchPhase.LOBBY, server, configManager.getSystemConfig());
	}
}
