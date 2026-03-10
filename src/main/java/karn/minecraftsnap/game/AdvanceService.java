package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class AdvanceService {
	private final UnitRegistry unitRegistry;

	public AdvanceService(UnitRegistry unitRegistry) {
		this.unitRegistry = unitRegistry;
	}

	public void tick(MinecraftServer server, MatchManager matchManager, SystemConfig.AdvanceConfig config) {
		if (matchManager.getPhase() != MatchPhase.GAME_RUNNING || matchManager.getServerTicks() % 20L != 0L) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (state.getRoleType() != RoleType.UNIT || state.getFactionId() != FactionId.MONSTER || player.isSpectator()) {
				continue;
			}
			var biomeId = player.getWorld().getBiome(player.getBlockPos()).getKey()
				.map(key -> key.getValue().toString())
				.orElse("minecraft:plains");
			updateProgress(state, biomeId, currentWeather(player), config);
		}
	}

	public void updateProgress(PlayerMatchState state, String biomeId, String weather, SystemConfig.AdvanceConfig config) {
		var condition = findCondition(state.getCurrentUnitId(), config);
		if (condition == null) {
			state.resetAdvanceState();
			return;
		}

		var biomeMatches = condition.biomes.contains(biomeId);
		var weatherMatches = condition.weathers.contains(weather);
		if (!biomeMatches || !weatherMatches) {
			state.resetAdvanceState();
			return;
		}

		state.setAdvanceTargetUnitId(condition.resultUnitId);
		state.setAdvanceProgressSeconds(state.getAdvanceProgressSeconds() + 1);
		if (state.getAdvanceProgressSeconds() >= condition.requiredSeconds) {
			state.setAdvanceAvailable(true);
			state.setAdvanceProgressSeconds(condition.requiredSeconds);
		}
	}

	public boolean forceAdvance(PlayerMatchState state, SystemConfig.AdvanceConfig config) {
		var condition = findCondition(state.getCurrentUnitId(), config);
		if (condition == null) {
			return false;
		}
		state.setAdvanceTargetUnitId(condition.resultUnitId);
		state.setAdvanceProgressSeconds(condition.requiredSeconds);
		state.setAdvanceAvailable(true);
		return true;
	}

	public UnitDefinition applyAdvance(PlayerMatchState state) {
		if (!state.isAdvanceAvailable() || state.getAdvanceTargetUnitId() == null) {
			return null;
		}

		var definition = unitRegistry.get(state.getAdvanceTargetUnitId());
		if (definition == null) {
			return null;
		}
		state.setCurrentUnitId(definition.id());
		state.resetAdvanceState();
		return definition;
	}

	public SystemConfig.AdvanceConditionConfig findCondition(String unitId, SystemConfig.AdvanceConfig config) {
		if (unitId == null || config == null || config.conditions == null) {
			return null;
		}
		return config.conditions.stream()
			.filter(condition -> unitId.equals(condition.unitId))
			.findFirst()
			.orElse(null);
	}

	public String currentWeather(ServerPlayerEntity player) {
		if (player.getWorld().isThundering()) {
			return "thunder";
		}
		if (player.getWorld().isRaining()) {
			return "rain";
		}
		return "clear";
	}
}
