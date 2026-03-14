package karn.minecraftsnap.unit;

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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class UnitContext {
	private final MatchManager matchManager;
	private final PlayerMatchState state;
	private final UnitDefinition unitDefinition;
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

	public Text format(String template) {
		return textTemplateResolver == null ? Text.literal(template) : textTemplateResolver.format(template);
	}

	public long serverTicks() {
		return matchManager == null ? 0L : matchManager.getServerTicks();
	}

	public ServerWorld world() {
		return player == null ? null : (ServerWorld) player.getWorld();
	}

	public LaneRuntime laneRuntime() {
		return laneRuntime;
	}

	public SystemConfig systemConfig() {
		return systemConfig;
	}

	public void baseBuildLoadout() {
		if (player != null && unitDefinition != null && unitLoadoutService != null && textTemplateResolver != null) {
			unitLoadoutService.applyBaseLoadout(player, unitDefinition, textTemplateResolver);
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

	public void openTrade() {
		if (player != null && tradeGuiService != null && state != null && unitDefinition != null) {
			tradeGuiService.open(player, state, unitDefinition.factionId());
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
		var wasReady = advanceService.hasReadyOption(state);
		advanceService.updateProgress(state, currentBiomeId(), currentWeather());
		if (!wasReady && advanceService.hasReadyOption(state) && player != null && textTemplateResolver != null) {
			player.sendMessage(textTemplateResolver.format(systemConfig.advance.readyMessage), false);
		}
	}

	public UnitDefinition targetAdvanceDefinition() {
		if (unitRegistry == null || state == null || advanceService == null) {
			return null;
		}
		return advanceService.describeOptions(state, currentBiomeId(), currentWeather()).stream()
			.filter(karn.minecraftsnap.game.AdvanceService.AdvanceOptionState::ready)
			.map(karn.minecraftsnap.game.AdvanceService.AdvanceOptionState::definition)
			.findFirst()
			.orElse(null);
	}

	public AdvanceService advanceService() {
		return advanceService;
	}

	public AdvanceGuiService advanceGuiService() {
		return advanceGuiService;
	}

	public Long getUnitRuntimeLong(String key) {
		return state == null ? null : state.getUnitRuntimeLong(key);
	}

	public void setUnitRuntimeLong(String key, long value) {
		if (state != null) {
			state.setUnitRuntimeLong(key, value);
		}
	}

	public void removeUnitRuntimeLong(String key) {
		if (state != null) {
			state.removeUnitRuntimeLong(key);
		}
	}

	public Double getUnitRuntimeDouble(String key) {
		return state == null ? null : state.getUnitRuntimeDouble(key);
	}

	public void setUnitRuntimeDouble(String key, double value) {
		if (state != null) {
			state.setUnitRuntimeDouble(key, value);
		}
	}

	public void removeUnitRuntimeDouble(String key) {
		if (state != null) {
			state.removeUnitRuntimeDouble(key);
		}
	}

	public void dealMobDamage(LivingEntity target, float amount) {
		if (player != null && target != null && world() != null) {
			target.damage(world(), target.getDamageSources().mobAttack(player), amount);
		}
	}

	public void dealExplosionDamage(LivingEntity target, float amount) {
		if (player != null && target != null && world() != null) {
			target.damage(world(), target.getDamageSources().explosion(player, player), amount);
		}
	}

	public boolean isEnemyUnit(ServerPlayerEntity target) {
		if (matchManager == null || state == null || target == null) {
			return false;
		}
		var targetState = matchManager.getPlayerState(target.getUuid());
		return targetState.getTeamId() != null
			&& state.getTeamId() != null
			&& targetState.getTeamId() != state.getTeamId()
			&& targetState.getRoleType() != karn.minecraftsnap.game.RoleType.SPECTATOR;
	}

	public boolean isEnemyTarget(LivingEntity target) {
		if (target == null) {
			return false;
		}
		if (target == player) {
			return false;
		}
		if (target instanceof ServerPlayerEntity targetPlayer) {
			return isEnemyUnit(targetPlayer);
		}
		return true;
	}
}
