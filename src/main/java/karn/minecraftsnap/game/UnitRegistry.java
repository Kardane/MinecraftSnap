package karn.minecraftsnap.game;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.FactionConfigFile;
import karn.minecraftsnap.config.UnitItemEntry;

public class UnitRegistry {
	private final Map<String, UnitDefinition> units = new LinkedHashMap<>();
	private final Map<String, karn.minecraftsnap.config.FactionUnitEntry> unitEntries = new LinkedHashMap<>();
	private final Map<FactionId, FactionConfigFile> factionConfigs = new EnumMap<>(FactionId.class);

	public UnitRegistry() {
		this(true);
	}

	public UnitRegistry(boolean registerDefaults) {
		if (!registerDefaults) {
			return;
		}
		register(def("villager", "주민", FactionId.VILLAGER, true, 1, 5, 20.0, 0.8, "minecraft:wooden_sword", "", "", "", "", "", "minecraft:bread", "밥먹기", 10, UnitDefinition.AmmoType.NONE, "minecraft:villager"));
		register(def("armorer_villager", "대장장이 주민", FactionId.VILLAGER, true, 2, 9, 30.0, 0.9, "minecraft:wooden_sword", "minecraft:shield", "", "minecraft:iron_chestplate", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:villager"));
		register(def("vindicator", "변명자", FactionId.VILLAGER, true, 4, 18, 30.0, 1.0, "minecraft:iron_axe", "", "", "", "", "", "minecraft:iron_axe", "도약", 10, UnitDefinition.AmmoType.NONE, "minecraft:vindicator"));
		register(def("pillager", "약탈자", FactionId.VILLAGER, true, 3, 15, 16.0, 1.1, "minecraft:crossbow", "", "", "", "", "", "minecraft:firework_rocket", "폭죽 화살 지급", 15, UnitDefinition.AmmoType.FIREWORK, "minecraft:pillager"));
		register(def("zombie", "좀비", FactionId.MONSTER, true, 1, 6, 20.0, 0.8, "minecraft:iron_shovel", "", "", "", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:zombie"));
		register(def("skeleton", "스켈레톤", FactionId.MONSTER, true, 2, 13, 14.0, 1.0, "minecraft:bow", "", "", "", "", "", "minecraft:bone", "뼈 폭발", 15, UnitDefinition.AmmoType.ARROW, "minecraft:skeleton"));
		register(def("slime", "슬라임", FactionId.MONSTER, true, 2, 10, 10.0, 1.1, "minecraft:slime_ball", "", "", "", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:slime"));
		register(def("creeper", "크리퍼", FactionId.MONSTER, true, 5, 25, 20.0, 1.0, "minecraft:tnt", "", "", "", "", "", "minecraft:tnt", "자폭", 20, UnitDefinition.AmmoType.NONE, "minecraft:creeper"));
		register(def("piglin", "피글린", FactionId.NETHER, true, 2, 5, 20.0, 1.0, "minecraft:golden_sword", "", "", "", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:piglin"));
		register(def("zombified_piglin", "좀비 피글린", FactionId.NETHER, true, 2, 8, 20.0, 0.8, "minecraft:golden_sword", "", "", "", "", "", "minecraft:golden_sword", "분노", 15, UnitDefinition.AmmoType.NONE, "minecraft:zombified_piglin"));
		register(def("blaze", "블레이즈", FactionId.NETHER, true, 3, 16, 16.0, 1.2, "minecraft:blaze_rod", "", "", "", "", "", "minecraft:blaze_rod", "화염구", 5, UnitDefinition.AmmoType.NONE, "minecraft:blaze"));
		register(def("piglin_brute", "피글린 브루트", FactionId.NETHER, true, 6, 25, 40.0, 1.0, "minecraft:golden_axe", "", "", "", "", "", "minecraft:golden_axe", "광란", 30, UnitDefinition.AmmoType.NONE, "minecraft:piglin_brute"));
		register(def("zombie_veteran", "강화 좀비", FactionId.MONSTER, false, 0, 0, 26.0, 0.95, "minecraft:iron_shovel", "", "", "minecraft:iron_chestplate", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:husk"));
		register(def("skeleton_sniper", "강화 스켈레톤", FactionId.MONSTER, false, 0, 0, 18.0, 1.05, "minecraft:bow", "", "", "", "", "", "minecraft:bone", "뼈 폭발", 12, UnitDefinition.AmmoType.ARROW, "minecraft:stray"));
		register(def("slime_brute", "강화 슬라임", FactionId.MONSTER, false, 0, 0, 18.0, 1.2, "minecraft:slime_ball", "", "", "", "", "", "", "", 0, UnitDefinition.AmmoType.NONE, "minecraft:slime"));
		register(def("charged_creeper", "대전된 크리퍼", FactionId.MONSTER, false, 0, 0, 24.0, 1.05, "minecraft:tnt", "", "", "", "", "", "minecraft:tnt", "자폭", 15, UnitDefinition.AmmoType.NONE, "minecraft:creeper"));
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

	private void register(UnitDefinition definition) {
		units.put(definition.id(), definition);
	}

	private UnitDefinition def(
		String id,
		String displayName,
		FactionId factionId,
		boolean captainSpawnable,
		int cost,
		int spawnCooldownSeconds,
		double maxHealth,
		double moveSpeedScale,
		String mainHandId,
		String offHandId,
		String helmetId,
		String chestId,
		String legsId,
		String bootsId,
		String abilityItemId,
		String abilityName,
		int abilityCooldownSeconds,
		UnitDefinition.AmmoType ammoType,
		String disguiseId
	) {
		return new UnitDefinition(
			id,
			displayName,
			factionId,
			captainSpawnable,
			cost,
			spawnCooldownSeconds,
			maxHealth,
			moveSpeedScale,
			UnitItemEntry.create(mainHandId),
			UnitItemEntry.create(offHandId),
			UnitItemEntry.create(helmetId),
			UnitItemEntry.create(chestId),
			UnitItemEntry.create(legsId),
			UnitItemEntry.create(bootsId),
			UnitItemEntry.create(abilityItemId),
			abilityName,
			abilityCooldownSeconds,
			ammoType,
			EntitySpecEntry.create(disguiseId),
			List.of()
		);
	}

	public void loadFromFactionConfigs(Map<FactionId, FactionConfigFile> configs) {
		loadFromFactionConfigs(configs, null);
	}

	public void loadFromFactionConfigs(Map<FactionId, FactionConfigFile> configs, Function<String, net.minecraft.item.Item> itemResolver) {
		units.clear();
		unitEntries.clear();
		factionConfigs.clear();
		for (var entry : configs.entrySet()) {
			var config = entry.getValue();
			if (config == null) {
				continue;
			}
			factionConfigs.put(entry.getKey(), config);
			for (var unit : config.units) {
				unitEntries.put(unit.id, unit);
				register(unit.toUnitDefinition(entry.getKey()));
			}
		}
	}

	public karn.minecraftsnap.config.FactionUnitEntry getEntry(String unitId) {
		return unitEntries.get(unitId);
	}

	public FactionConfigFile getFactionConfig(FactionId factionId) {
		return factionConfigs.get(factionId);
	}
}
