package karn.minecraftsnap.game;

import net.minecraft.item.Items;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import karn.minecraftsnap.config.FactionConfigFile;

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
		register(new UnitDefinition("villager", "주민", FactionId.VILLAGER, true, 1, 5, 20.0, 0.8, Items.WOODEN_SWORD, null, null, null, null, null, Items.BREAD, "밥먹기", 10, UnitDefinition.UnitAbilityType.HEAL_SELF, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "villager", List.of()));
		register(new UnitDefinition("armorer_villager", "대장장이 주민", FactionId.VILLAGER, true, 2, 9, 30.0, 0.9, Items.WOODEN_SWORD, Items.SHIELD, null, Items.IRON_CHESTPLATE, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "villager", List.of()));
		register(new UnitDefinition("vindicator", "변명자", FactionId.VILLAGER, true, 4, 18, 30.0, 1.0, Items.IRON_AXE, null, null, null, null, null, Items.IRON_AXE, "도약", 10, UnitDefinition.UnitAbilityType.DASH, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "vindicator", List.of()));
		register(new UnitDefinition("pillager", "약탈자", FactionId.VILLAGER, true, 3, 15, 16.0, 1.1, Items.CROSSBOW, null, null, null, null, null, Items.FIREWORK_ROCKET, "폭죽 화살 지급", 15, UnitDefinition.UnitAbilityType.GIVE_FIREWORKS, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.FIREWORK, "pillager", List.of()));
		register(new UnitDefinition("zombie", "좀비", FactionId.MONSTER, true, 1, 6, 20.0, 0.8, Items.IRON_SHOVEL, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.ZOMBIE_COOLDOWN_REFUND, UnitDefinition.AmmoType.NONE, "zombie", List.of()));
		register(new UnitDefinition("skeleton", "스켈레톤", FactionId.MONSTER, true, 2, 13, 14.0, 1.0, Items.BOW, null, null, null, null, null, Items.BONE, "뼈 폭발", 15, UnitDefinition.UnitAbilityType.BONE_BLAST, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.ARROW, "skeleton", List.of()));
		register(new UnitDefinition("slime", "슬라임", FactionId.MONSTER, true, 2, 10, 10.0, 1.1, Items.SLIME_BALL, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.SLIME_SPLIT, UnitDefinition.AmmoType.NONE, "slime", List.of()));
		register(new UnitDefinition("creeper", "크리퍼", FactionId.MONSTER, true, 5, 25, 20.0, 1.0, Items.TNT, null, null, null, null, null, Items.TNT, "자폭", 20, UnitDefinition.UnitAbilityType.CREEPER_BOMB, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "creeper", List.of()));
		register(new UnitDefinition("piglin", "피글린", FactionId.NETHER, true, 2, 5, 20.0, 1.0, Items.GOLDEN_SWORD, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.PIGLIN_ZOMBIFY_ON_DEATH, UnitDefinition.AmmoType.NONE, "piglin", List.of()));
		register(new UnitDefinition("zombified_piglin", "좀비 피글린", FactionId.NETHER, true, 2, 8, 20.0, 0.8, Items.GOLDEN_SWORD, null, null, null, null, null, Items.GOLDEN_SWORD, "분노", 15, UnitDefinition.UnitAbilityType.ZOMBIFIED_PIGLIN_RAGE, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "zombified_piglin", List.of()));
		register(new UnitDefinition("blaze", "블레이즈", FactionId.NETHER, true, 3, 16, 16.0, 1.2, Items.BLAZE_ROD, null, null, null, null, null, Items.BLAZE_ROD, "화염구", 5, UnitDefinition.UnitAbilityType.BLAZE_BURST, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "blaze", List.of()));
		register(new UnitDefinition("piglin_brute", "피글린 브루트", FactionId.NETHER, true, 6, 25, 40.0, 1.0, Items.GOLDEN_AXE, null, null, null, null, null, Items.GOLDEN_AXE, "광란", 30, UnitDefinition.UnitAbilityType.BRUTE_FRENZY, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "piglin_brute", List.of()));
		register(new UnitDefinition("zombie_veteran", "강화 좀비", FactionId.MONSTER, false, 0, 0, 26.0, 0.95, Items.IRON_SHOVEL, null, null, Items.IRON_CHESTPLATE, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.ZOMBIE_COOLDOWN_REFUND, UnitDefinition.AmmoType.NONE, "husk", List.of()));
		register(new UnitDefinition("skeleton_sniper", "강화 스켈레톤", FactionId.MONSTER, false, 0, 0, 18.0, 1.05, Items.BOW, null, null, null, null, null, Items.BONE, "뼈 폭발", 12, UnitDefinition.UnitAbilityType.BONE_BLAST, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.ARROW, "stray", List.of()));
		register(new UnitDefinition("slime_brute", "강화 슬라임", FactionId.MONSTER, false, 0, 0, 18.0, 1.2, Items.SLIME_BALL, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.SLIME_SPLIT, UnitDefinition.AmmoType.NONE, "slime", List.of()));
		register(new UnitDefinition("charged_creeper", "대전된 크리퍼", FactionId.MONSTER, false, 0, 0, 24.0, 1.05, Items.TNT, null, null, null, null, null, Items.TNT, "자폭", 15, UnitDefinition.UnitAbilityType.CREEPER_BOMB, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "creeper", List.of()));
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
				register(itemResolver == null
					? unit.toUnitDefinition(entry.getKey())
					: unit.toUnitDefinition(entry.getKey(), itemResolver));
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
