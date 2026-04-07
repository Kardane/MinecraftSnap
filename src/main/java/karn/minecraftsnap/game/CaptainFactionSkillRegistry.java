package karn.minecraftsnap.game;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

final class CaptainFactionSkillRegistry {
	private final EnumMap<FactionId, CaptainFactionSkillStrategy> strategies = new EnumMap<>(FactionId.class);

	CaptainFactionSkillRegistry(Collection<? extends CaptainFactionSkillStrategy> strategies) {
		if (strategies == null) {
			return;
		}
		for (var strategy : strategies) {
			if (strategy == null) {
				continue;
			}
			var previous = this.strategies.putIfAbsent(strategy.factionId(), strategy);
			if (previous != null) {
				throw new IllegalArgumentException("Duplicate captain skill strategy for faction: " + strategy.factionId());
			}
		}
	}

	CaptainFactionSkillStrategy strategyFor(FactionId factionId) {
		return factionId == null ? null : strategies.get(factionId);
	}

	List<CaptainFactionSkillStrategy> all() {
		return List.copyOf(strategies.values());
	}
}
