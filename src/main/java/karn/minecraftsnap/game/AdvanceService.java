package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdvanceService {
	private final UnitRegistry unitRegistry;

	public AdvanceService(UnitRegistry unitRegistry) {
		this.unitRegistry = unitRegistry;
	}

	public void updateProgress(PlayerMatchState state, String biomeId, String weather) {
		var options = findOptions(state == null ? null : state.getCurrentUnitId());
		if (state == null || options.isEmpty()) {
			if (state != null) {
				state.resetAdvanceState();
			}
			return;
		}

		Set<String> validResultIds = new LinkedHashSet<>();
		for (var option : options) {
			validResultIds.add(option.resultUnitId);
			if (matches(option, biomeId, weather)) {
				var nextTicks = Math.min(option.requiredTicks, state.getAdvanceOptionTicks(option.resultUnitId) + 1);
				state.setAdvanceOptionTicks(option.resultUnitId, nextTicks);
			} else {
				state.setAdvanceOptionTicks(option.resultUnitId, 0);
			}
		}
		state.clearAdvanceOptionTicksExcept(validResultIds);
	}

	public boolean forceAdvance(PlayerMatchState state) {
		var options = findOptions(state == null ? null : state.getCurrentUnitId());
		if (state == null || options.isEmpty()) {
			return false;
		}
		var first = options.getFirst();
		state.setAdvanceOptionTicks(first.resultUnitId, first.requiredTicks);
		return true;
	}

	public UnitDefinition applyAdvance(PlayerMatchState state, String resultUnitId) {
		if (state == null || resultUnitId == null || resultUnitId.isBlank()) {
			return null;
		}
		var option = findOption(state.getCurrentUnitId(), resultUnitId);
		if (option == null || !state.isAdvanceReady(option.resultUnitId, option.requiredTicks)) {
			return null;
		}

		var definition = unitRegistry.get(option.resultUnitId);
		if (definition == null) {
			return null;
		}
		state.setCurrentUnitId(definition.id());
		state.resetAdvanceState();
		return definition;
	}

	public List<AdvanceOptionState> describeOptions(PlayerMatchState state, String biomeId, String weather) {
		if (state == null) {
			return List.of();
		}
		return findOptions(state.getCurrentUnitId()).stream()
			.map(option -> new AdvanceOptionState(
				option,
				unitRegistry.get(option.resultUnitId),
				state.getAdvanceOptionTicks(option.resultUnitId),
				matches(option, biomeId, weather),
				state.isAdvanceReady(option.resultUnitId, option.requiredTicks)
			))
			.toList();
	}

	public List<AdvanceOptionEntry> findOptions(String unitId) {
		return unitRegistry.getAdvanceOptions(unitId);
	}

	public AdvanceOptionEntry findOption(String unitId, String resultUnitId) {
		return findOptions(unitId).stream()
			.filter(option -> resultUnitId.equals(option.resultUnitId))
			.findFirst()
			.orElse(null);
	}

	public boolean hasReadyOption(PlayerMatchState state) {
		if (state == null) {
			return false;
		}
		return findOptions(state.getCurrentUnitId()).stream()
			.anyMatch(option -> state.isAdvanceReady(option.resultUnitId, option.requiredTicks));
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

	private boolean matches(AdvanceOptionEntry option, String biomeId, String weather) {
		var biomeMatches = option.biomes.isEmpty() || option.biomes.contains(biomeId);
		var weatherMatches = option.weathers.isEmpty() || option.weathers.contains(weather);
		return biomeMatches && weatherMatches;
	}

	public record AdvanceOptionState(
		AdvanceOptionEntry option,
		UnitDefinition definition,
		int currentTicks,
		boolean conditionsMet,
		boolean ready
	) {
	}
}
