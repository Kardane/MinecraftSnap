package karn.minecraftsnap.config;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class FactionUnitEntry {
	public String id = "";
	public String displayName = "";
	public boolean captainSpawnable = true;
	public int cost;
	public int spawnCooldownSeconds;
	public double maxHealth = 20.0;
	public double moveSpeedScale = 1.0;
	public UnitItemEntry mainHand = UnitItemEntry.create("minecraft:wooden_sword");
	public UnitItemEntry offHand = new UnitItemEntry();
	public UnitItemEntry abilityItem = new UnitItemEntry();
	public String mainHandItemId = "minecraft:wooden_sword";
	public String offHandItemId = "";
	public String helmetItemId = "";
	public String chestItemId = "";
	public String legsItemId = "";
	public String bootsItemId = "";
	public String abilityItemId = "";
	public String abilityName = "";
	public int abilityCooldownSeconds;
	public String abilityType = UnitDefinition.UnitAbilityType.NONE.name();
	public String passiveType = UnitDefinition.UnitPassiveType.NONE.name();
	public String ammoType = UnitDefinition.AmmoType.NONE.name();
	public String disguiseId = "";
	public List<String> descriptionLines = new ArrayList<>();

	public void normalize() {
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
		if (mainHand == null) {
			mainHand = UnitItemEntry.create(mainHandItemId);
		}
		if (offHand == null) {
			offHand = UnitItemEntry.create(offHandItemId);
		}
		if (abilityItem == null) {
			abilityItem = UnitItemEntry.create(abilityItemId);
		}
		if (mainHandItemId == null || mainHandItemId.isBlank()) {
			mainHandItemId = "minecraft:air";
		}
		if (mainHand.itemId == null || mainHand.itemId.isBlank()) {
			mainHand.itemId = mainHandItemId;
		}
		if (offHand.itemId == null || offHand.itemId.isBlank()) {
			offHand.itemId = offHandItemId;
		}
		if (abilityItem.itemId == null || abilityItem.itemId.isBlank()) {
			abilityItem.itemId = abilityItemId;
		}
		mainHand.normalize();
		offHand.normalize();
		abilityItem.normalize();
		if (abilityType == null || abilityType.isBlank()) {
			abilityType = UnitDefinition.UnitAbilityType.NONE.name();
		}
		if (passiveType == null || passiveType.isBlank()) {
			passiveType = UnitDefinition.UnitPassiveType.NONE.name();
		}
		if (ammoType == null || ammoType.isBlank()) {
			ammoType = UnitDefinition.AmmoType.NONE.name();
		}
	}

	public UnitDefinition toUnitDefinition(FactionId factionId) {
		return toUnitDefinition(factionId, this::parseItem);
	}

	public UnitDefinition toUnitDefinition(FactionId factionId, Function<String, Item> itemResolver) {
		return new UnitDefinition(
			id,
			displayName,
			factionId,
			captainSpawnable,
			cost,
			spawnCooldownSeconds,
			maxHealth,
			moveSpeedScale,
			mainHand.resolve(itemResolver),
			offHand.resolve(itemResolver),
			itemResolver.apply(helmetItemId),
			itemResolver.apply(chestItemId),
			itemResolver.apply(legsItemId),
			itemResolver.apply(bootsItemId),
			abilityItem.resolve(itemResolver),
			abilityName,
			abilityCooldownSeconds,
			parseAbilityType(abilityType),
			parsePassiveType(passiveType),
			parseAmmoType(ammoType),
			disguiseId,
			List.copyOf(descriptionLines)
		);
	}

	private Item parseItem(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}
		return switch (itemId.toLowerCase(Locale.ROOT)) {
			case "minecraft:air" -> null;
			case "minecraft:wooden_sword" -> Items.WOODEN_SWORD;
			case "minecraft:shield" -> Items.SHIELD;
			case "minecraft:iron_chestplate" -> Items.IRON_CHESTPLATE;
			case "minecraft:iron_axe" -> Items.IRON_AXE;
			case "minecraft:crossbow" -> Items.CROSSBOW;
			case "minecraft:firework_rocket" -> Items.FIREWORK_ROCKET;
			case "minecraft:bread" -> Items.BREAD;
			case "minecraft:iron_shovel" -> Items.IRON_SHOVEL;
			case "minecraft:bow" -> Items.BOW;
			case "minecraft:bone" -> Items.BONE;
			case "minecraft:slime_ball" -> Items.SLIME_BALL;
			case "minecraft:tnt" -> Items.TNT;
			case "minecraft:golden_sword" -> Items.GOLDEN_SWORD;
			case "minecraft:blaze_rod" -> Items.BLAZE_ROD;
			case "minecraft:golden_axe" -> Items.GOLDEN_AXE;
			default -> Items.STONE;
		};
	}

	private UnitDefinition.UnitAbilityType parseAbilityType(String value) {
		return UnitDefinition.UnitAbilityType.valueOf(value.toUpperCase(Locale.ROOT));
	}

	private UnitDefinition.UnitPassiveType parsePassiveType(String value) {
		return UnitDefinition.UnitPassiveType.valueOf(value.toUpperCase(Locale.ROOT));
	}

	private UnitDefinition.AmmoType parseAmmoType(String value) {
		return UnitDefinition.AmmoType.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
