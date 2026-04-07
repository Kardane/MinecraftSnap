package karn.minecraftsnap.config;

import karn.minecraftsnap.game.UnitDefinition;

import java.util.ArrayList;
import java.util.List;

public class UnitConfigEntry {
	public String id = "";
	public String displayName = "";
	public String factionId = "";
	public boolean captainSpawnable = true;
	public int cost = 1;
	public double maxHealth = 20.0;
	public double moveSpeedScale = 1.0;
	public UnitItemEntry mainHand = new UnitItemEntry();
	public UnitItemEntry offHand = new UnitItemEntry();
	public UnitItemEntry helmet = new UnitItemEntry();
	public UnitItemEntry chest = new UnitItemEntry();
	public UnitItemEntry legs = new UnitItemEntry();
	public UnitItemEntry boots = new UnitItemEntry();
	public UnitItemEntry abilityItem = new UnitItemEntry();
	public String abilityName = "";
	public int abilityCooldownSeconds;
	public String ammoType = UnitDefinition.AmmoType.NONE.name();
	public EntitySpecEntry disguise = new EntitySpecEntry();
	public List<String> descriptionLines = new ArrayList<>();
	public List<AdvanceOptionEntry> advanceOptions = new ArrayList<>();
	public UnitExtraAttributes attributes = new UnitExtraAttributes();

	public static UnitConfigEntry from(UnitDefinition definition) {
		var entry = new UnitConfigEntry();
		if (definition == null) {
			entry.normalize();
			return entry;
		}
		entry.id = definition.id();
		entry.displayName = definition.displayName();
		entry.factionId = definition.factionId().name();
		entry.captainSpawnable = definition.captainSpawnable();
		entry.cost = definition.cost();
		entry.maxHealth = definition.maxHealth();
		entry.moveSpeedScale = definition.moveSpeedScale();
		entry.mainHand = copyItem(definition.mainHand());
		entry.offHand = copyItem(definition.offHand());
		entry.helmet = copyItem(definition.helmet());
		entry.chest = copyItem(definition.chest());
		entry.legs = copyItem(definition.legs());
		entry.boots = copyItem(definition.boots());
		entry.abilityItem = copyItem(definition.abilityItemSpec());
		entry.abilityName = definition.abilityName();
		entry.abilityCooldownSeconds = definition.abilityCooldownSeconds();
		entry.ammoType = definition.ammoType().name();
		entry.disguise = copyEntity(definition.disguise());
		entry.descriptionLines = copyLines(definition.descriptionLines());
		entry.advanceOptions = copyAdvanceOptions(definition.advanceOptions());
		entry.attributes = copyAttributes(definition.extraAttributes());
		applyKnownAttributeDefaults(entry);
		entry.normalize();
		return entry;
	}

	public void normalize() {
		if (id == null) {
			id = "";
		}
		if (displayName == null) {
			displayName = "";
		}
		if (factionId == null) {
			factionId = "";
		}
		if (cost < 0) {
			cost = 0;
		}
		if (maxHealth <= 0.0) {
			maxHealth = 20.0;
		}
		if (moveSpeedScale <= 0.0) {
			moveSpeedScale = 1.0;
		}
		mainHand = normalizedItem(mainHand);
		offHand = normalizedItem(offHand);
		helmet = normalizedItem(helmet);
		chest = normalizedItem(chest);
		legs = normalizedItem(legs);
		boots = normalizedItem(boots);
		abilityItem = normalizedItem(abilityItem);
		if (abilityName == null) {
			abilityName = "";
		}
		if (abilityCooldownSeconds < 0) {
			abilityCooldownSeconds = 0;
		}
		if (ammoType == null || ammoType.isBlank()) {
			ammoType = UnitDefinition.AmmoType.NONE.name();
		}
		if (disguise == null) {
			disguise = new EntitySpecEntry();
		}
		disguise.normalize();
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		} else {
			descriptionLines = new ArrayList<>(descriptionLines);
		}
		if (advanceOptions == null) {
			advanceOptions = new ArrayList<>();
		} else {
			advanceOptions = new ArrayList<>(advanceOptions);
			advanceOptions.forEach(AdvanceOptionEntry::normalize);
		}
		if (attributes == null) {
			attributes = new UnitExtraAttributes();
		}
		attributes.normalize();
	}

	public UnitDefinition mergeOnto(UnitDefinition defaults) {
		if (defaults == null) {
			return null;
		}
		normalize();
		return new UnitDefinition(
			defaults.id(),
			displayName.isBlank() ? defaults.displayName() : displayName,
			parseFaction(defaults),
			captainSpawnable,
			cost,
			maxHealth,
			moveSpeedScale,
			mainHand,
			offHand,
			helmet,
			chest,
			legs,
			boots,
			abilityItem,
			abilityName,
			abilityCooldownSeconds,
			parseAmmoType(defaults),
			disguise,
			descriptionLines,
			advanceOptions,
			attributes
		);
	}

	public UnitDefinition toDefinition() {
		normalize();
		return new UnitDefinition(
			id,
			displayName,
			parseFactionId(),
			captainSpawnable,
			cost,
			maxHealth,
			moveSpeedScale,
			mainHand,
			offHand,
			helmet,
			chest,
			legs,
			boots,
			abilityItem,
			abilityName,
			abilityCooldownSeconds,
			parseAmmoType(),
			disguise,
			descriptionLines,
			advanceOptions,
			attributes
		);
	}

	private karn.minecraftsnap.game.FactionId parseFaction(UnitDefinition defaults) {
		try {
			return karn.minecraftsnap.game.FactionId.valueOf(factionId);
		} catch (Exception ignored) {
			return defaults.factionId();
		}
	}

	private karn.minecraftsnap.game.FactionId parseFactionId() {
		try {
			return karn.minecraftsnap.game.FactionId.valueOf(factionId);
		} catch (Exception ignored) {
			return karn.minecraftsnap.game.FactionId.VILLAGER;
		}
	}

	private UnitDefinition.AmmoType parseAmmoType(UnitDefinition defaults) {
		try {
			return UnitDefinition.AmmoType.valueOf(ammoType);
		} catch (Exception ignored) {
			return defaults.ammoType();
		}
	}

	private UnitDefinition.AmmoType parseAmmoType() {
		try {
			return UnitDefinition.AmmoType.valueOf(ammoType);
		} catch (Exception ignored) {
			return UnitDefinition.AmmoType.NONE;
		}
	}

	private static UnitItemEntry normalizedItem(UnitItemEntry entry) {
		var normalized = entry == null ? new UnitItemEntry() : entry;
		normalized.normalize();
		return normalized;
	}

	private static UnitItemEntry copyItem(UnitItemEntry source) {
		var copy = new UnitItemEntry();
		if (source == null) {
			copy.normalize();
			return copy;
		}
		copy.itemId = source.itemId;
		copy.count = source.count;
		copy.displayName = source.displayName;
		copy.loreLines = source.loreLines == null ? new ArrayList<>() : new ArrayList<>(source.loreLines);
		copy.componentsNbt = source.componentsNbt;
		copy.stackNbt = source.stackNbt;
		copy.normalize();
		return copy;
	}

	private static EntitySpecEntry copyEntity(EntitySpecEntry source) {
		var copy = new EntitySpecEntry();
		if (source == null) {
			copy.normalize();
			return copy;
		}
		copy.entityId = source.entityId;
		copy.entityNbt = source.entityNbt;
		copy.normalize();
		return copy;
	}

	private static List<String> copyLines(List<String> source) {
		return source == null ? new ArrayList<>() : new ArrayList<>(source);
	}

	private static List<AdvanceOptionEntry> copyAdvanceOptions(List<AdvanceOptionEntry> source) {
		var copies = new ArrayList<AdvanceOptionEntry>();
		if (source == null) {
			return copies;
		}
		for (var option : source) {
			if (option == null) {
				continue;
			}
			var copy = new AdvanceOptionEntry();
			copy.resultUnitId = option.resultUnitId;
			copy.displayName = option.displayName;
			copy.descriptionLines = copyLines(option.descriptionLines);
			copy.biomes = copyLines(option.biomes);
			copy.weathers = copyLines(option.weathers);
			copy.requiredTicks = option.requiredTicks;
			copy.normalize();
			copies.add(copy);
		}
		return copies;
	}

	private static UnitExtraAttributes copyAttributes(UnitExtraAttributes source) {
		var copy = new UnitExtraAttributes();
		if (source == null) {
			copy.normalize();
			return copy;
		}
		copy.jumpStrength = source.jumpStrength;
		copy.stepHeight = source.stepHeight;
		copy.scale = source.scale;
		copy.attackRange = source.attackRange;
		copy.knockbackResistance = source.knockbackResistance;
		copy.safeFallDistance = source.safeFallDistance;
		copy.normalize();
		return copy;
	}

	private static void applyKnownAttributeDefaults(UnitConfigEntry entry) {
		if (entry == null || entry.attributes == null) {
			return;
		}
		switch (entry.id) {
			case "breeze" -> entry.attributes.jumpStrength = defaultIfNull(entry.attributes.jumpStrength, 0.6D);
			case "slime", "magma_cube" -> {
				entry.attributes.jumpStrength = defaultIfNull(entry.attributes.jumpStrength, 0.8D);
				entry.attributes.attackRange = defaultIfNull(entry.attributes.attackRange, 4.0D);
				entry.attributes.knockbackResistance = defaultIfNull(entry.attributes.knockbackResistance, 0.3D);
				entry.attributes.safeFallDistance = defaultIfNull(entry.attributes.safeFallDistance, 999.0D);
			}
			case "giant_slime" -> {
				entry.attributes.jumpStrength = defaultIfNull(entry.attributes.jumpStrength, 0.8D);
				entry.attributes.stepHeight = defaultIfNull(entry.attributes.stepHeight, 1.6D);
				entry.attributes.scale = defaultIfNull(entry.attributes.scale, 2.0D);
				entry.attributes.attackRange = defaultIfNull(entry.attributes.attackRange, 6.0D);
				entry.attributes.safeFallDistance = defaultIfNull(entry.attributes.safeFallDistance, 999.0D);
			}
			case "iron_golem" -> {
				entry.attributes.jumpStrength = defaultIfNull(entry.attributes.jumpStrength, 0.0D);
				entry.attributes.stepHeight = defaultIfNull(entry.attributes.stepHeight, 1.3D);
				entry.attributes.knockbackResistance = defaultIfNull(entry.attributes.knockbackResistance, 1.0D);
				entry.attributes.safeFallDistance = defaultIfNull(entry.attributes.safeFallDistance, 999.0D);
			}
			case "hoglin" -> {
				entry.attributes.attackRange = defaultIfNull(entry.attributes.attackRange, 3.5D);
				entry.attributes.knockbackResistance = defaultIfNull(entry.attributes.knockbackResistance, 0.6D);
			}
			case "creeper" -> entry.attributes.knockbackResistance = defaultIfNull(entry.attributes.knockbackResistance, 1.0D);
			case "snow_golem", "blaze", "ghast" -> entry.attributes.safeFallDistance = defaultIfNull(entry.attributes.safeFallDistance, 999.0D);
			default -> {
			}
		}
	}

	private static Double defaultIfNull(Double value, double fallback) {
		return value == null ? fallback : value;
	}
}
