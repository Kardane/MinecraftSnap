package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.lane.LaneCaptureStatus;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.unit.UnitClass;
import karn.minecraftsnap.unit.UnitClassRegistry;
import karn.minecraftsnap.unit.UnitContext;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class UnitHookService {
	private final MatchManager matchManager;
	private final UnitRegistry unitRegistry;
	private final UnitClassRegistry unitClassRegistry;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final StatsRepository statsRepository;
	private final TextTemplateResolver textTemplateResolver;
	private final UnitAbilityService unitAbilityService;
	private final UnitLoadoutService unitLoadoutService;
	private final AdvanceService advanceService;
	private final TradeGuiService tradeGuiService;
	private final AdvanceGuiService advanceGuiService;
	private final CaptainManaService captainManaService;

	public UnitHookService(
		MatchManager matchManager,
		UnitRegistry unitRegistry,
		UnitClassRegistry unitClassRegistry,
		LaneRuntimeRegistry laneRuntimeRegistry,
		StatsRepository statsRepository,
		TextTemplateResolver textTemplateResolver,
		UnitAbilityService unitAbilityService,
		UnitLoadoutService unitLoadoutService,
		AdvanceService advanceService,
		TradeGuiService tradeGuiService,
		AdvanceGuiService advanceGuiService,
		CaptainManaService captainManaService
	) {
		this.matchManager = matchManager;
		this.unitRegistry = unitRegistry;
		this.unitClassRegistry = unitClassRegistry;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.statsRepository = statsRepository;
		this.textTemplateResolver = textTemplateResolver;
		this.unitAbilityService = unitAbilityService;
		this.unitLoadoutService = unitLoadoutService;
		this.advanceService = advanceService;
		this.tradeGuiService = tradeGuiService;
		this.advanceGuiService = advanceGuiService;
		this.captainManaService = captainManaService;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null || matchManager == null || matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			var context = createContext(player, systemConfig, null);
			if (context == null) {
				continue;
			}
			var previousLaneId = context.state().getLastLaneId();
			var currentLaneId = context.laneRuntime() == null ? null : context.laneRuntime().laneId();
			if (currentLaneId != previousLaneId && currentLaneId != null) {
				dispatchOnLaneEnter(unitClassOf(context.unitDefinition().id()), context, previousLaneId, currentLaneId);
			}
			context.state().setLastLaneId(currentLaneId);
			dispatchOnTick(unitClassOf(context.unitDefinition().id()), context);
			if (context.laneRuntime() != null
				&& context.laneRuntime().aliveCaptureUnitPlayerIds().contains(player.getUuid())
				&& context.laneRuntime().captureStatus() != LaneCaptureStatus.IDLE) {
				dispatchOnCaptureTick(unitClassOf(context.unitDefinition().id()), context);
			}
		}
	}

	public boolean handleShiftF(ServerPlayerEntity player, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context == null) {
			return false;
		}
		dispatchOnShiftF(unitClassOf(context.unitDefinition().id()), context);
		return true;
	}

	public boolean handleSkillUse(ServerPlayerEntity player, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context == null) {
			return false;
		}
		dispatchOnSkillUse(unitClassOf(context.unitDefinition().id()), context);
		return true;
	}

	public void handleDamaged(ServerPlayerEntity victim, DamageSource source, float amount, SystemConfig systemConfig) {
		var context = createContext(victim, systemConfig, null);
		if (context != null) {
			dispatchOnDamaged(unitClassOf(context.unitDefinition().id()), context, source, amount);
		}
	}

	public void handleAttack(ServerPlayerEntity attacker, ServerPlayerEntity victim, float amount, SystemConfig systemConfig) {
		var context = createContext(attacker, systemConfig, null);
		if (context != null) {
			dispatchOnAttack(unitClassOf(context.unitDefinition().id()), context, victim, amount);
		}
	}

	public void handleDeath(ServerPlayerEntity victim, DamageSource source, SystemConfig systemConfig) {
		var context = createContext(victim, systemConfig, null);
		if (context != null) {
			dispatchOnDeath(unitClassOf(context.unitDefinition().id()), context, source);
		}
	}

	public void handleKill(ServerPlayerEntity killer, ServerPlayerEntity victim, SystemConfig systemConfig) {
		var context = createContext(killer, systemConfig, null);
		if (context != null) {
			dispatchOnKill(unitClassOf(context.unitDefinition().id()), context, victim);
		}
	}

	public void handleCaptureScore(ServerPlayerEntity player, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context != null) {
			dispatchOnCaptureScore(unitClassOf(context.unitDefinition().id()), context);
		}
	}

	public void applyLoadout(ServerPlayerEntity player, UnitDefinition definition, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, definition);
		if (context == null) {
			return;
		}
		var unitClass = unitClassOf(definition.id());
		unitClass.buildLoadout(context);
		unitClass.applyAttributes(context);
	}

	public void openAdvanceGui(ServerPlayerEntity player, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context == null) {
			return;
		}
		var state = context.state();
		var condition = advanceService.findCondition(state.getCurrentUnitId(), systemConfig.advance);
		var biomeId = context.currentBiomeId();
		var weather = context.currentWeather();
		var targetDefinition = context.targetAdvanceDefinition();
		if (targetDefinition == null && condition != null) {
			targetDefinition = unitRegistry.get(condition.resultUnitId);
		}
		var requiredExp = condition == null ? 0 : condition.requiredExp;
		advanceGuiService.open(player, state, biomeId, weather, requiredExp, targetDefinition, () -> {
			var definition = advanceService.applyAdvance(state);
			if (definition == null) {
				player.sendMessage(textTemplateResolver.format(systemConfig.advance.notAvailableMessage), false);
				return;
			}
			applyLoadout(player, definition, systemConfig);
			DisguiseSupport.applyDisguise(player, definition.disguiseId());
			player.sendMessage(textTemplateResolver.format("&a전직 완료: &f" + definition.displayName()), false);
		});
	}

	public void dispatchOnTick(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onTick(context);
		}
	}

	public void dispatchOnAttack(UnitClass unitClass, UnitContext context, ServerPlayerEntity victim, float amount) {
		if (unitClass != null && context != null) {
			unitClass.onAttack(context, victim, amount);
		}
	}

	public void dispatchOnDamaged(UnitClass unitClass, UnitContext context, DamageSource source, float amount) {
		if (unitClass != null && context != null) {
			unitClass.onDamaged(context, source, amount);
		}
	}

	public void dispatchOnDeath(UnitClass unitClass, UnitContext context, DamageSource source) {
		if (unitClass != null && context != null) {
			unitClass.onDeath(context, source);
		}
	}

	public void dispatchOnKill(UnitClass unitClass, UnitContext context, ServerPlayerEntity victim) {
		if (unitClass != null && context != null) {
			unitClass.onKill(context, victim);
		}
	}

	public void dispatchOnSkillUse(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onSkillUse(context);
		}
	}

	public void dispatchOnShiftF(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onShiftF(context);
		}
	}

	public void dispatchOnLaneEnter(UnitClass unitClass, UnitContext context, LaneId previousLaneId, LaneId currentLaneId) {
		if (unitClass != null && context != null) {
			unitClass.onLaneEnter(context, previousLaneId, currentLaneId);
		}
	}

	public void dispatchOnCaptureTick(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onCaptureTick(context);
		}
	}

	public void dispatchOnCaptureScore(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onCaptureScore(context);
		}
	}

	private UnitContext createContext(ServerPlayerEntity player, SystemConfig systemConfig, UnitDefinition overrideDefinition) {
		if (player == null || systemConfig == null || matchManager == null || unitRegistry == null) {
			return null;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		var definition = overrideDefinition != null ? overrideDefinition : unitRegistry.get(state.getCurrentUnitId());
		if (state.getRoleType() != RoleType.UNIT || definition == null) {
			return null;
		}
		LaneRuntime laneRuntime = laneRuntimeRegistry == null ? null : laneRuntimeRegistry.findByPlayer(player);
		return new UnitContext(
			matchManager,
			state,
			definition,
			unitRegistry.getEntry(definition.id()),
			player,
			laneRuntime,
			systemConfig,
			textTemplateResolver,
			unitAbilityService,
			unitLoadoutService,
			advanceService,
			unitRegistry,
			tradeGuiService,
			advanceGuiService,
			statsRepository,
			captainManaService,
			this
		);
	}

	private UnitClass unitClassOf(String unitId) {
		return unitClassRegistry == null ? null : unitClassRegistry.get(unitId);
	}
}
