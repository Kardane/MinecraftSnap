package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.ShopConfigFile;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.lane.LaneCaptureStatus;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.AdvanceGuiService;
import karn.minecraftsnap.ui.PlayerDisplayNameService;
import karn.minecraftsnap.ui.TradeGuiService;
import karn.minecraftsnap.unit.UnitClass;
import karn.minecraftsnap.unit.UnitClassRegistry;
import karn.minecraftsnap.unit.UnitContext;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class UnitHookService {
	private final MatchManager matchManager;
	private final UnitRegistry unitRegistry;
	private final UnitClassRegistry unitClassRegistry;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final Supplier<StatsRepository> statsRepositorySupplier;
	private final TextTemplateResolver textTemplateResolver;
	private final UnitAbilityService unitAbilityService;
	private final UnitLoadoutService unitLoadoutService;
	private final AdvanceService advanceService;
	private final TradeGuiService tradeGuiService;
	private final AdvanceGuiService advanceGuiService;
	private final CaptainManaService captainManaService;
	private final PlayerDisplayNameService playerDisplayNameService;
	private final UiSoundService uiSoundService;
	private final VillagerEnchantService villagerEnchantService;
	private final Supplier<ShopConfigFile> villagerShopConfigSupplier;

	public UnitHookService(
		MatchManager matchManager,
		UnitRegistry unitRegistry,
		UnitClassRegistry unitClassRegistry,
		LaneRuntimeRegistry laneRuntimeRegistry,
		Supplier<StatsRepository> statsRepositorySupplier,
		TextTemplateResolver textTemplateResolver,
		UnitAbilityService unitAbilityService,
		UnitLoadoutService unitLoadoutService,
		AdvanceService advanceService,
		TradeGuiService tradeGuiService,
		AdvanceGuiService advanceGuiService,
		CaptainManaService captainManaService,
		PlayerDisplayNameService playerDisplayNameService,
		UiSoundService uiSoundService,
		VillagerEnchantService villagerEnchantService,
		Supplier<ShopConfigFile> villagerShopConfigSupplier
	) {
		this.matchManager = matchManager;
		this.unitRegistry = unitRegistry;
		this.unitClassRegistry = unitClassRegistry;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.statsRepositorySupplier = statsRepositorySupplier;
		this.textTemplateResolver = textTemplateResolver;
		this.unitAbilityService = unitAbilityService;
		this.unitLoadoutService = unitLoadoutService;
		this.advanceService = advanceService;
		this.tradeGuiService = tradeGuiService;
		this.advanceGuiService = advanceGuiService;
		this.captainManaService = captainManaService;
		this.playerDisplayNameService = playerDisplayNameService;
		this.uiSoundService = uiSoundService;
		this.villagerEnchantService = villagerEnchantService;
		this.villagerShopConfigSupplier = villagerShopConfigSupplier;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null || matchManager == null) {
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

	public void handleAttack(ServerPlayerEntity attacker, LivingEntity victim, float amount, SystemConfig systemConfig) {
		var context = createContext(attacker, systemConfig, null);
		if (context != null) {
			dispatchOnAttack(unitClassOf(context.unitDefinition().id()), context, victim, amount);
		}
	}

	public void handleProjectileHit(ServerPlayerEntity owner, ProjectileEntity projectile, Entity target, SystemConfig systemConfig) {
		var context = createContext(owner, systemConfig, null);
		if (context != null) {
			dispatchOnProjectileHit(unitClassOf(context.unitDefinition().id()), context, projectile, target);
		}
	}

	public void handleProjectileImpact(ServerPlayerEntity owner, ProjectileEntity projectile, Vec3d impactPos, SystemConfig systemConfig) {
		var context = createContext(owner, systemConfig, null);
		if (context != null) {
			dispatchOnProjectileImpact(unitClassOf(context.unitDefinition().id()), context, projectile, impactPos);
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

	public boolean shouldCancelMove(ServerPlayerEntity player, PlayerMoveC2SPacket packet, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context == null) {
			return false;
		}
		return dispatchShouldCancelMove(unitClassOf(context.unitDefinition().id()), context, packet);
	}

	public void applyLoadout(ServerPlayerEntity player, UnitDefinition definition, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, definition);
		if (context == null) {
			return;
		}
		var unitClass = unitClassOf(definition.id());
		unitClass.buildLoadout(context);
		unitClass.applyAttributes(context);
		reapplyVillagerEnchants(player, context.state(), definition);
	}

	public void assignUnit(ServerPlayerEntity player, UnitDefinition definition, SystemConfig systemConfig) {
		if (player == null || definition == null || systemConfig == null) {
			return;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		state.setFactionId(definition.factionId());
		matchManager.setCurrentUnit(player.getUuid(), definition.id());
		if (unitAbilityService != null) {
			unitAbilityService.clearPlayerState(player.getUuid());
		}
		applyLoadout(player, definition, systemConfig);
		DisguiseSupport.applyDisguise(player, definition.disguise());
		player.setHealth((float) definition.maxHealth());
	}

	public void openAdvanceGui(ServerPlayerEntity player, SystemConfig systemConfig) {
		var context = createContext(player, systemConfig, null);
		if (context == null) {
			return;
		}
		var state = context.state();
		var biomeId = context.currentBiomeId();
		var weather = context.currentWeather();
		var options = advanceService.describeOptions(state, biomeId, weather).stream()
			.limit(5)
			.map(AdvanceGuiService.AdvanceOptionView::from)
			.toList();
		advanceGuiService.open(player, options, resultUnitId -> {
			var definition = advanceService.applyAdvance(state, resultUnitId);
			if (definition == null) {
				if (uiSoundService != null) {
					uiSoundService.playUiDeny(player);
				}
				player.sendMessage(textTemplateResolver.format(systemConfig.advance.notAvailableMessage), false);
				return;
			}
			assignUnit(player, definition, systemConfig);
			if (playerDisplayNameService != null && matchManager != null && matchManager.getServer() != null) {
				playerDisplayNameService.refreshAll(matchManager.getServer(), matchManager, currentStatsRepository(), systemConfig);
				matchManager.getServer().getPlayerManager().broadcast(
					textTemplateResolver.format(textConfig().advanceBroadcastMessage
						.replace("{player}", player.getName().getString())
						.replace("{unit}", definition.displayName())),
					false
				);
				if (uiSoundService != null) {
					uiSoundService.playEventSuccess(matchManager.getServer());
				}
			}
			if (uiSoundService != null) {
				uiSoundService.playUiConfirm(player);
			}
			player.sendMessage(textTemplateResolver.format(textConfig().advanceCompleteMessage.replace("{unit}", definition.displayName())), false);
		});
	}

	public void dispatchOnTick(UnitClass unitClass, UnitContext context) {
		if (unitClass != null && context != null) {
			unitClass.onTick(context);
		}
	}

	public void dispatchOnAttack(UnitClass unitClass, UnitContext context, LivingEntity victim, float amount) {
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

	public void dispatchOnProjectileHit(UnitClass unitClass, UnitContext context, ProjectileEntity projectile, Entity target) {
		if (unitClass != null && context != null) {
			unitClass.onProjectileHit(context, projectile, target);
		}
	}

	public void dispatchOnProjectileImpact(UnitClass unitClass, UnitContext context, ProjectileEntity projectile, Vec3d impactPos) {
		if (unitClass != null && context != null) {
			unitClass.onProjectileImpact(context, projectile, impactPos);
		}
	}

	public boolean dispatchShouldCancelMove(UnitClass unitClass, UnitContext context, PlayerMoveC2SPacket packet) {
		if (unitClass != null && context != null) {
			return unitClass.shouldCancelMove(context, packet);
		}
		return false;
	}

	private UnitContext createContext(ServerPlayerEntity player, SystemConfig systemConfig, UnitDefinition overrideDefinition) {
		if (player == null || systemConfig == null || matchManager == null || unitRegistry == null) {
			return null;
		}
		var state = matchManager.getPlayerState(player.getUuid());
		var definition = overrideDefinition != null ? overrideDefinition : unitRegistry.get(state.getCurrentUnitId());
		if (!canUseUnitActions(state.getRoleType(), state.getCurrentUnitId(), player.isSpectator()) || definition == null) {
			return null;
		}
		LaneRuntime laneRuntime = laneRuntimeRegistry == null ? null : laneRuntimeRegistry.findByPlayer(player);
		return new UnitContext(
			matchManager,
			state,
			definition,
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
			currentStatsRepository(),
			captainManaService,
			this
		);
	}

	private StatsRepository currentStatsRepository() {
		return statsRepositorySupplier == null ? null : statsRepositorySupplier.get();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	private void reapplyVillagerEnchants(ServerPlayerEntity player, PlayerMatchState state, UnitDefinition definition) {
		if (player == null
			|| state == null
			|| definition == null
			|| definition.factionId() != FactionId.VILLAGER
			|| villagerEnchantService == null
			|| villagerShopConfigSupplier == null) {
			return;
		}
		var config = villagerShopConfigSupplier.get();
		if (config == null || config.entries == null || config.entries.isEmpty()) {
			return;
		}
		villagerEnchantService.reapplyCurrentEnchantments(player, state, config.entries);
	}

	private UnitClass unitClassOf(String unitId) {
		return unitClassRegistry == null ? null : unitClassRegistry.get(unitId);
	}

	public static boolean canUseUnitActions(RoleType roleType, String currentUnitId, boolean spectator) {
		return roleType == RoleType.UNIT && currentUnitId != null && !currentUnitId.isBlank() && !spectator;
	}
}
