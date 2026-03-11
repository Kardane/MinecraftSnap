package karn.minecraftsnap.unit;

import karn.minecraftsnap.config.FactionUnitEntry;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.CaptainManaService;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.UnitAbilityService;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitHookService;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;

public class UnitContext {
	private final MatchManager matchManager;
	private final PlayerMatchState state;
	private final UnitDefinition unitDefinition;
	private final FactionUnitEntry unitEntry;
	private final ServerPlayerEntity player;
	private final LaneRuntime laneRuntime;
	private final SystemConfig systemConfig;
	private final TextTemplateResolver textTemplateResolver;
	private final UnitAbilityService unitAbilityService;
	private final UnitLoadoutService unitLoadoutService;
	private final AdvanceService advanceService;
	private final UnitRegistry unitRegistry;
	private final TradeGuiService tradeGuiService;
	private final AdvanceGuiService advanceGuiService;
	private final StatsRepository statsRepository;
	private final CaptainManaService captainManaService;
	private final UnitHookService unitHookService;

	public UnitContext(
		MatchManager matchManager,
		PlayerMatchState state,
		UnitDefinition unitDefinition,
		FactionUnitEntry unitEntry,
		ServerPlayerEntity player,
		LaneRuntime laneRuntime,
		SystemConfig systemConfig,
		TextTemplateResolver textTemplateResolver,
		UnitAbilityService unitAbilityService,
		UnitLoadoutService unitLoadoutService,
		AdvanceService advanceService,
		UnitRegistry unitRegistry,
		TradeGuiService tradeGuiService,
		AdvanceGuiService advanceGuiService,
		StatsRepository statsRepository,
		CaptainManaService captainManaService,
		UnitHookService unitHookService
	) {
		this.matchManager = matchManager;
		this.state = state;
		this.unitDefinition = unitDefinition;
		this.unitEntry = unitEntry;
		this.player = player;
		this.laneRuntime = laneRuntime;
		this.systemConfig = systemConfig;
		this.textTemplateResolver = textTemplateResolver;
		this.unitAbilityService = unitAbilityService;
		this.unitLoadoutService = unitLoadoutService;
		this.advanceService = advanceService;
		this.unitRegistry = unitRegistry;
		this.tradeGuiService = tradeGuiService;
		this.advanceGuiService = advanceGuiService;
		this.statsRepository = statsRepository;
		this.captainManaService = captainManaService;
		this.unitHookService = unitHookService;
	}

	public MatchManager matchManager() {
		return matchManager;
	}

	public PlayerMatchState state() {
		return state;
	}

	public UnitDefinition unitDefinition() {
		return unitDefinition;
	}

	public ServerPlayerEntity player() {
		return player;
	}

	public LaneRuntime laneRuntime() {
		return laneRuntime;
	}

	public SystemConfig systemConfig() {
		return systemConfig;
	}

	public void baseBuildLoadout() {
		if (player != null && unitDefinition != null && unitLoadoutService != null && textTemplateResolver != null) {
			unitLoadoutService.applyBaseLoadout(player, unitDefinition, unitEntry, textTemplateResolver);
		}
	}

	public void baseApplyAttributes() {
		if (player != null && unitDefinition != null && unitLoadoutService != null) {
			unitLoadoutService.applyBaseAttributes(player, unitDefinition);
		}
	}

	public boolean activateSkill(java.util.function.BooleanSupplier action) {
		return player != null
			&& unitAbilityService != null
			&& matchManager != null
			&& unitDefinition != null
			&& unitAbilityService.activateUnitSkill(player, matchManager, unitDefinition, action);
	}

	public void rewardEmerald(int amount) {
		if (state == null || player == null || statsRepository == null) {
			return;
		}
		state.addEmeralds(amount);
		statsRepository.addEmeralds(player.getUuid(), player.getName().getString(), amount);
	}

	public void rewardGold(int amount) {
		if (state == null || player == null || statsRepository == null) {
			return;
		}
		state.addGoldIngots(amount);
		statsRepository.addGoldIngots(player.getUuid(), player.getName().getString(), amount);
	}

	public void reduceCaptainSpawnCooldown(int seconds) {
		if (matchManager == null || state == null || state.getTeamId() == null || captainManaService == null) {
			return;
		}
		var captainId = matchManager.getCaptainId(state.getTeamId());
		if (captainId != null) {
			captainManaService.reduceSpawnCooldown(captainId, seconds);
		}
	}

	public boolean heal(float amount) {
		return player != null && unitAbilityService != null && unitAbilityService.heal(player, amount);
	}

	public boolean dash(double horizontalSpeed, double upwardSpeed) {
		return player != null && unitAbilityService != null && unitAbilityService.dash(player, horizontalSpeed, upwardSpeed);
	}

	public boolean giveFireworks(int count) {
		return player != null && unitAbilityService != null && unitAbilityService.giveFireworks(player, count);
	}

	public boolean boneBlast(double radius, float damage, int slowSeconds, int amplifier) {
		return player != null && unitAbilityService != null && matchManager != null
			&& unitAbilityService.boneBlast(player, matchManager, radius, damage, slowSeconds, amplifier);
	}

	public boolean queueCreeperBomb(int warmupTicks, String message) {
		return player != null && unitAbilityService != null && matchManager != null
			&& unitAbilityService.queueCreeperBomb(player, matchManager, warmupTicks, message);
	}

	public boolean applyEffects(StatusEffectInstance... effects) {
		return player != null && unitAbilityService != null && unitAbilityService.applyEffects(player, effects);
	}

	public boolean spawnFireballBurst() {
		return player != null && unitAbilityService != null && unitAbilityService.spawnFireballBurst(player);
	}

	public void spawnSlimeSplit() {
		if (player != null && unitAbilityService != null) {
			unitAbilityService.spawnSlimeSplit(player);
		}
	}

	public void trySpawnZombifiedPiglin(float chance) {
		if (player != null && unitAbilityService != null) {
			unitAbilityService.trySpawnZombifiedPiglin(player, chance);
		}
	}

	public void openTrade() {
		if (player != null && tradeGuiService != null && state != null) {
			tradeGuiService.open(player, state);
		}
	}

	public void openAdvance() {
		if (player != null && unitHookService != null && systemConfig != null) {
			unitHookService.openAdvanceGui(player, systemConfig);
		}
	}

	public String currentBiomeId() {
		if (player == null) {
			return "minecraft:plains";
		}
		return player.getWorld().getBiome(player.getBlockPos()).getKey()
			.map(key -> key.getValue().toString())
			.orElse("minecraft:plains");
	}

	public String currentWeather() {
		if (player == null || advanceService == null) {
			return "clear";
		}
		return advanceService.currentWeather(player);
	}

	public void updateMonsterAdvance() {
		if (advanceService == null || state == null || systemConfig == null) {
			return;
		}
		var wasReady = state.isAdvanceAvailable();
		advanceService.updateProgress(state, currentBiomeId(), currentWeather(), systemConfig.advance);
		if (!wasReady && state.isAdvanceAvailable() && player != null && textTemplateResolver != null) {
			player.sendMessage(textTemplateResolver.format(systemConfig.advance.readyMessage), false);
		}
	}

	public UnitDefinition targetAdvanceDefinition() {
		if (unitRegistry == null || state == null) {
			return null;
		}
		return state.getAdvanceTargetUnitId() == null ? null : unitRegistry.get(state.getAdvanceTargetUnitId());
	}

	public AdvanceService advanceService() {
		return advanceService;
	}

	public AdvanceGuiService advanceGuiService() {
		return advanceGuiService;
	}
}
