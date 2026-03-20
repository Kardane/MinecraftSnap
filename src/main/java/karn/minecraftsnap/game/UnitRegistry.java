package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.unit.UnitClassRegistry;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitRegistry {
	private final Map<String, UnitDefinition> units = new LinkedHashMap<>();
	private final Map<FactionId, FactionSpec> factionSpecs = new EnumMap<>(FactionId.class);

	public UnitRegistry() {
		this(true);
	}

	public UnitRegistry(boolean registerDefaults) {
		if (registerDefaults) {
			loadFromConfiguredClasses(new UnitClassRegistry());
		}
	}

	public UnitDefinition get(String unitId) {
		return units.get(unitId);
	}

	public Collection<UnitDefinition> byFaction(FactionId factionId) {
		return units.values().stream()
			.filter(unit -> unit.factionId() == factionId && unit.captainSpawnable())
			.collect(Collectors.toList());
	}

	public Collection<UnitDefinition> allByFaction(FactionId factionId) {
		return units.values().stream()
			.filter(unit -> unit.factionId() == factionId)
			.collect(Collectors.toList());
	}

	public Collection<UnitDefinition> all() {
		return units.values();
	}

	public void registerUnit(UnitDefinition definition) {
		register(definition);
	}

	public void loadFromConfiguredClasses(UnitClassRegistry unitClassRegistry) {
		units.clear();
		factionSpecs.clear();
		factionSpecs.putAll(FactionSpecs.defaults());
		if (unitClassRegistry == null) {
			return;
		}
		for (var unitClass : unitClassRegistry.configuredUnits()) {
			register(unitClass.definition());
		}
	}

	public void applyTextConfig(karn.minecraftsnap.config.TextConfigFile textConfig) {
		for (var factionId : FactionId.values()) {
			var oldSpec = factionSpecs.getOrDefault(factionId, FactionSpecs.get(factionId));
			if (oldSpec != null) {
				var summary = textConfig.factionSummaries.get(factionId.name());
				var skillDesc = textConfig.captainSkillDescriptions.get(factionId.name());
				if (summary != null || skillDesc != null) {
					factionSpecs.put(factionId, new FactionSpec(
						oldSpec.displayName(),
						summary != null ? summary : oldSpec.summaryLines(),
						oldSpec.captainSkillName(),
						skillDesc != null ? skillDesc : oldSpec.captainSkillDescriptionLines()
					));
				}
			}
		}

		var updatedUnits = new LinkedHashMap<String, UnitDefinition>();
		for (var entry : units.entrySet()) {
			var unit = entry.getValue();
			var desc = textConfig.unitDescriptions.get(unit.id());
			if (desc != null) {
				updatedUnits.put(entry.getKey(), new UnitDefinition(
					unit.id(), unit.displayName(), unit.factionId(), unit.captainSpawnable(),
					unit.cost(), unit.maxHealth(), unit.moveSpeedScale(),
					unit.mainHand(), unit.offHand(), unit.helmet(), unit.chest(),
					unit.legs(), unit. boots(), unit.abilityItemSpec(), unit.abilityName(),
					unit.abilityCooldownSeconds(), unit.ammoType(), unit.disguise(),
					desc, unit.advanceOptions()
				));
			} else {
				updatedUnits.put(entry.getKey(), unit);
			}
		}
		units.clear();
		units.putAll(updatedUnits);
	}

	public List<AdvanceOptionEntry> getAdvanceOptions(String unitId) {
		var definition = get(unitId);
		if (definition == null || definition.advanceOptions() == null) {
			return List.of();
		}
		return definition.advanceOptions();
	}

	public FactionSpec getFactionSpec(FactionId factionId) {
		return factionSpecs.getOrDefault(factionId, FactionSpecs.get(factionId));
	}

	private void register(UnitDefinition definition) {
		units.put(definition.id(), definition);
	}
}
