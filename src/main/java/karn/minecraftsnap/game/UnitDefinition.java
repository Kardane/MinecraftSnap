package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import net.minecraft.item.Item;

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
	List<AdvanceOptionEntry> advanceOptions
) {
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

	public boolean hasActiveSkill() {
		return (abilityName != null && !abilityName.isBlank())
			|| abilityCooldownSeconds > 0
			|| (abilityItemSpec != null && !abilityItemSpec.isEmpty());
	}

	public enum AmmoType {
		NONE,
		ARROW,
		FIREWORK
	}
}
