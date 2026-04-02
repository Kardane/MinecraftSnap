package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.CaptainManaService;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.UnitAbilityService;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.game.UnitSpawnQueueService;
import karn.minecraftsnap.game.UnitSpawnService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;

public class UnitHudService {
	private final MatchManager matchManager;
	private final UnitRegistry unitRegistry;
	private final CaptainManaService captainManaService;
	private final UnitAbilityService unitAbilityService;
	private final TextTemplateResolver textTemplateResolver;
	private final UnitSpawnQueueService unitSpawnQueueService;
	private final java.util.Map<java.util.UUID, String> lastRenderedActionBars = new java.util.HashMap<>();

	public UnitHudService(
		MatchManager matchManager,
		UnitRegistry unitRegistry,
		CaptainManaService captainManaService,
		UnitAbilityService unitAbilityService,
		TextTemplateResolver textTemplateResolver,
		UnitSpawnQueueService unitSpawnQueueService
	) {
		this.matchManager = matchManager;
		this.unitRegistry = unitRegistry;
		this.captainManaService = captainManaService;
		this.unitAbilityService = unitAbilityService;
		this.textTemplateResolver = textTemplateResolver;
		this.unitSpawnQueueService = unitSpawnQueueService;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || matchManager.getServerTicks() % 10L != 0L) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.isCaptain()) {
				renderActionBar(player, formatCaptainActionBar(player, state.getTeamId(), systemConfig), true);
				continue;
			}
			if (state.getRoleType() == RoleType.UNIT && state.getCurrentUnitId() == null) {
				renderActionBar(player, formatSpectatorQueueActionBar(player.getUuid(), state.getTeamId(), systemConfig), true);
				continue;
			}
			if (state.getRoleType() != RoleType.UNIT || state.getCurrentUnitId() == null || player.isSpectator()) {
				clearActionBar(player);
				continue;
			}
			var definition = unitRegistry.get(state.getCurrentUnitId());
			if (definition == null) {
				clearActionBar(player);
				continue;
			}
			var cooldown = unitAbilityService.remainingUnitCooldownSeconds(player.getUuid(), matchManager.getServerTicks());
			renderActionBar(player, formatActionBar(definition, cooldown, systemConfig), false);
		}
	}

	private void clearActionBar(net.minecraft.server.network.ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		var previous = lastRenderedActionBars.remove(player.getUuid());
		if (shouldSendActionBar(previous, "")) {
			player.sendMessage(net.minecraft.text.Text.empty(), true);
		}
	}

	private void renderActionBar(net.minecraft.server.network.ServerPlayerEntity player, String message, boolean persistent) {
		if (player == null) {
			return;
		}
		var actionBar = textTemplateResolver.format(message);
		var rendered = actionBar.getString();
		if (!shouldRenderActionBar(lastRenderedActionBars.get(player.getUuid()), rendered, persistent)) {
			return;
		}
		player.sendMessage(actionBar, true);
		if (rendered.isBlank()) {
			lastRenderedActionBars.remove(player.getUuid());
		} else {
			lastRenderedActionBars.put(player.getUuid(), rendered);
		}
	}

	static String formatActionBar(UnitDefinition definition, int remainingCooldownSeconds, SystemConfig systemConfig) {
		var displayConfig = systemConfig == null ? new SystemConfig.DisplayConfig() : systemConfig.display;
		var unitName = definition == null ? displayConfig.unitHudUnknownUnitName : definition.displayName();
		if (definition == null || !definition.hasActiveSkill() || definition.abilityName() == null || definition.abilityName().isBlank()) {
			return unitName;
		}
		var skillName = definition.abilityName();
		var textConfig = modTextConfig();
		var cooldown = remainingCooldownSeconds <= 0
			? displayConfig.unitHudReadyMessage
			: textConfig.unitHudCooldownTemplate.replace("{seconds}", Integer.toString(remainingCooldownSeconds));
		return displayConfig.unitHudTemplate
			.replace("{unit}", unitName)
			.replace("{skill}", skillName)
			.replace("{cooldown}", cooldown);
	}

	static String formatCaptainActionBar(
		int currentMana,
		int maxMana,
		int manaCooldownSeconds,
		String laneLabel,
		String nextPlayerName,
		int skillCooldownSeconds,
		SystemConfig systemConfig
	) {
		var displayConfig = systemConfig == null ? new SystemConfig.DisplayConfig() : systemConfig.display;
		var playerName = nextPlayerName == null || nextPlayerName.isBlank()
			? displayConfig.captainHudNoTargetPlayerName
			: nextPlayerName;
		var normalizedLaneLabel = laneLabel == null || laneLabel.isBlank() ? "-" : laneLabel;
		return displayConfig.captainHudTemplate
			.replace("{current_mana}", Integer.toString(currentMana))
			.replace("{max_mana}", Integer.toString(maxMana))
			.replace("{mana_cooldown}", Integer.toString(Math.max(0, manaCooldownSeconds)))
			.replace("{lane}", normalizedLaneLabel)
			.replace("{player}", playerName)
			.replace("{skill_cooldown}", Integer.toString(Math.max(0, skillCooldownSeconds)));
	}

	static String formatSpectatorQueueActionBar(int position, SystemConfig systemConfig) {
		if (position <= 0) {
			return "";
		}
		var displayConfig = systemConfig == null ? new SystemConfig.DisplayConfig() : systemConfig.display;
		return displayConfig.spectatorQueueHudTemplate.replace("{position}", Integer.toString(position));
	}

	static boolean shouldSendActionBar(String previous, String current) {
		var normalizedPrevious = previous == null ? "" : previous;
		var normalizedCurrent = current == null ? "" : current;
		return !normalizedPrevious.equals(normalizedCurrent)
			&& (!normalizedCurrent.isBlank() || !normalizedPrevious.isBlank());
	}

	static boolean shouldRenderPersistentActionBar(String previous, String current) {
		var normalizedPrevious = previous == null ? "" : previous;
		var normalizedCurrent = current == null ? "" : current;
		return !normalizedCurrent.isBlank() || !normalizedPrevious.isBlank();
	}

	private static boolean shouldRenderActionBar(String previous, String current, boolean persistent) {
		return persistent
			? shouldRenderPersistentActionBar(previous, current)
			: shouldSendActionBar(previous, current);
	}

	private String formatCaptainActionBar(net.minecraft.server.network.ServerPlayerEntity player, TeamId teamId, SystemConfig systemConfig) {
		var lane = UnitSpawnService.nearestLaneForCaptain(player, systemConfig);
		var captainState = captainManaService == null ? null : captainManaService.getOrCreate(player.getUuid());
		return formatCaptainActionBar(
			captainState == null ? 0 : captainState.getCurrentMana(),
			captainState == null ? 0 : captainState.getMaxMana(),
			captainState == null ? 0 : captainState.getSecondsUntilNextMana(),
			laneLabel(lane),
			nextSpawnPlayerName(teamId),
			captainState == null ? 0 : captainState.getSkillCooldownSeconds(),
			systemConfig
		);
	}

	private String formatSpectatorQueueActionBar(java.util.UUID playerId, TeamId teamId, SystemConfig systemConfig) {
		if (unitSpawnQueueService == null || teamId == null) {
			return "";
		}
		return formatSpectatorQueueActionBar(unitSpawnQueueService.queueDisplayPosition(matchManager, teamId, playerId), systemConfig);
	}

	private String nextSpawnPlayerName(TeamId teamId) {
		if (unitSpawnQueueService == null || teamId == null || matchManager.getServer() == null) {
			return "";
		}
		var nextPlayerId = unitSpawnQueueService.peekNextPlayer(matchManager, teamId);
		if (nextPlayerId == null) {
			return "";
		}
		var player = matchManager.getServer().getPlayerManager().getPlayer(nextPlayerId);
		return player == null ? "" : player.getName().getString();
	}
	private static String laneLabel(LaneId laneId) {
		return switch (laneId) {
			case LANE_1 -> "1라인";
			case LANE_2 -> "2라인";
			case LANE_3 -> "3라인";
		};
	}

	private static karn.minecraftsnap.config.TextConfigFile modTextConfig() {
		var mod = karn.minecraftsnap.MinecraftSnap.getInstance();
		return mod == null ? new karn.minecraftsnap.config.TextConfigFile() : mod.getTextConfig();
	}
}
