package karn.minecraftsnap.game;

import net.minecraft.item.Items;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitRegistry {
	private final Map<String, UnitDefinition> units = new LinkedHashMap<>();

	public UnitRegistry() {
		register(new UnitDefinition("villager", "주민", FactionId.VILLAGER, 1, 5, 20.0, 0.8, Items.WOODEN_SWORD, null, null, null, null, null, Items.BREAD, "밥먹기", 10, UnitDefinition.UnitAbilityType.HEAL_SELF, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "villager"));
		register(new UnitDefinition("armorer_villager", "대장장이 주민", FactionId.VILLAGER, 2, 9, 30.0, 0.9, Items.WOODEN_SWORD, Items.SHIELD, null, Items.IRON_CHESTPLATE, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "villager"));
		register(new UnitDefinition("vindicator", "변명자", FactionId.VILLAGER, 4, 18, 30.0, 1.0, Items.IRON_AXE, null, null, null, null, null, Items.IRON_AXE, "도약", 10, UnitDefinition.UnitAbilityType.DASH, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "vindicator"));
		register(new UnitDefinition("pillager", "약탈자", FactionId.VILLAGER, 3, 15, 16.0, 1.1, Items.CROSSBOW, null, null, null, null, null, Items.FIREWORK_ROCKET, "폭죽 화살 지급", 15, UnitDefinition.UnitAbilityType.GIVE_FIREWORKS, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.FIREWORK, "pillager"));
		register(new UnitDefinition("zombie", "좀비", FactionId.MONSTER, 1, 6, 20.0, 0.8, Items.IRON_SHOVEL, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.ZOMBIE_COOLDOWN_REFUND, UnitDefinition.AmmoType.NONE, "zombie"));
		register(new UnitDefinition("skeleton", "스켈레톤", FactionId.MONSTER, 2, 13, 14.0, 1.0, Items.BOW, null, null, null, null, null, Items.BONE, "뼈 폭발", 15, UnitDefinition.UnitAbilityType.BONE_BLAST, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.ARROW, "skeleton"));
		register(new UnitDefinition("slime", "슬라임", FactionId.MONSTER, 2, 10, 10.0, 1.1, Items.SLIME_BALL, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.SLIME_SPLIT, UnitDefinition.AmmoType.NONE, "slime"));
		register(new UnitDefinition("creeper", "크리퍼", FactionId.MONSTER, 5, 25, 20.0, 1.0, Items.TNT, null, null, null, null, null, Items.TNT, "자폭", 20, UnitDefinition.UnitAbilityType.CREEPER_BOMB, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "creeper"));
		register(new UnitDefinition("piglin", "피글린", FactionId.NETHER, 2, 5, 20.0, 1.0, Items.GOLDEN_SWORD, null, null, null, null, null, null, "", 0, UnitDefinition.UnitAbilityType.NONE, UnitDefinition.UnitPassiveType.PIGLIN_ZOMBIFY_ON_DEATH, UnitDefinition.AmmoType.NONE, "piglin"));
		register(new UnitDefinition("zombified_piglin", "좀비 피글린", FactionId.NETHER, 2, 8, 20.0, 0.8, Items.GOLDEN_SWORD, null, null, null, null, null, Items.GOLDEN_SWORD, "분노", 15, UnitDefinition.UnitAbilityType.ZOMBIFIED_PIGLIN_RAGE, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "zombified_piglin"));
		register(new UnitDefinition("blaze", "블레이즈", FactionId.NETHER, 3, 16, 16.0, 1.2, Items.BLAZE_ROD, null, null, null, null, null, Items.BLAZE_ROD, "화염구", 5, UnitDefinition.UnitAbilityType.BLAZE_BURST, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "blaze"));
		register(new UnitDefinition("piglin_brute", "피글린 브루트", FactionId.NETHER, 6, 25, 40.0, 1.0, Items.GOLDEN_AXE, null, null, null, null, null, Items.GOLDEN_AXE, "광란", 30, UnitDefinition.UnitAbilityType.BRUTE_FRENZY, UnitDefinition.UnitPassiveType.NONE, UnitDefinition.AmmoType.NONE, "piglin_brute"));
	}

	public UnitDefinition get(String unitId) {
		return units.get(unitId);
	}

	public Collection<UnitDefinition> byFaction(FactionId factionId) {
		return units.values().stream()
			.filter(unit -> unit.factionId() == factionId)
			.collect(Collectors.toList());
	}

	public Collection<UnitDefinition> all() {
		return units.values();
	}

	private void register(UnitDefinition definition) {
		units.put(definition.id(), definition);
	}
}
