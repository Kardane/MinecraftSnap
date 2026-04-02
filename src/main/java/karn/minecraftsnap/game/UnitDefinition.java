package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitExtraAttributes;
import karn.minecraftsnap.config.UnitItemEntry;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public record UnitDefinition(
	String id,
	String displayName,
	FactionId factionId,
	boolean captainSpawnable,
	int cost,
	double maxHealth,
	double moveSpeedScale,
	UnitItemEntry mainHand,
	UnitItemEntry offHand,
	UnitItemEntry helmet,
	UnitItemEntry chest,
	UnitItemEntry legs,
	UnitItemEntry boots,
	UnitItemEntry abilityItemSpec,
	String abilityName,
	int abilityCooldownSeconds,
	AmmoType ammoType,
	EntitySpecEntry disguise,
	List<String> descriptionLines,
	List<AdvanceOptionEntry> advanceOptions,
	UnitExtraAttributes extraAttributes
) {
	public UnitDefinition(
		String id,
		String displayName,
		FactionId factionId,
		boolean captainSpawnable,
		int cost,
		double maxHealth,
		double moveSpeedScale,
		UnitItemEntry mainHand,
		UnitItemEntry offHand,
		UnitItemEntry helmet,
		UnitItemEntry chest,
		UnitItemEntry legs,
		UnitItemEntry boots,
		UnitItemEntry abilityItemSpec,
		String abilityName,
		int abilityCooldownSeconds,
		AmmoType ammoType,
		EntitySpecEntry disguise,
		List<String> descriptionLines,
		List<AdvanceOptionEntry> advanceOptions
	) {
		this(
			id,
			displayName,
			factionId,
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
			abilityItemSpec,
			abilityName,
			abilityCooldownSeconds,
			ammoType,
			disguise,
			descriptionLines,
			advanceOptions,
			new UnitExtraAttributes()
		);
	}

	public UnitDefinition {
		extraAttributes = extraAttributes == null ? new UnitExtraAttributes() : extraAttributes;
		extraAttributes.normalize();
	}

	public Item mainHandItem() {
		return mainHand == null ? null : mainHand.resolve();
	}

	public Item offHandItem() {
		return offHand == null ? null : offHand.resolve();
	}

	public Item helmetItem() {
		return helmet == null ? null : helmet.resolve();
	}

	public Item chestItem() {
		return chest == null ? null : chest.resolve();
	}

	public Item legsItem() {
		return legs == null ? null : legs.resolve();
	}

	public Item bootsItem() {
		return boots == null ? null : boots.resolve();
	}

	public Item abilityItem() {
		return abilityItemSpec == null ? null : abilityItemSpec.resolve();
	}

	public Item guiIconItem() {
		var item = firstResolvedItem(mainHand, offHand, abilityItemSpec, helmet, chest, legs, boots);
		return item != null ? item : Items.BARRIER;
	}

	String guiIconItemId() {
		var itemId = firstResolvedItemId(mainHand, offHand, abilityItemSpec, helmet, chest, legs, boots);
		return itemId != null && !itemId.isBlank() ? itemId : "minecraft:barrier";
	}

	public Item skillCooldownItem() {
		return firstResolvedItem(abilityItemSpec, mainHand, offHand);
	}

	String skillCooldownItemId() {
		return firstResolvedItemId(abilityItemSpec, mainHand, offHand);
	}

	public boolean hasActiveSkill() {
		return (abilityName != null && !abilityName.isBlank())
			|| abilityCooldownSeconds > 0
			|| (abilityItemSpec != null && !abilityItemSpec.isEmpty());
	}

	private Item firstResolvedItem(UnitItemEntry... entries) {
		if (entries == null) {
			return null;
		}
		for (var entry : entries) {
			if (entry == null) {
				continue;
			}
			var item = entry.resolve();
			if (item != null) {
				return item;
			}
		}
		return null;
	}

	private String firstResolvedItemId(UnitItemEntry... entries) {
		if (entries == null) {
			return null;
		}
		for (var entry : entries) {
			if (entry == null) {
				continue;
			}
			var itemId = entry.resolvedItemId();
			if (itemId != null && !itemId.isBlank() && !"minecraft:air".equals(itemId)) {
				return itemId;
			}
		}
		return null;
	}

	public enum AmmoType {
		NONE,
		ARROW,
		SLOWNESS_ARROW,
		POISON_ARROW,
		FIREWORK
	}
}
