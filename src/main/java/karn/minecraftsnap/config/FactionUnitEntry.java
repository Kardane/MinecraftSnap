package karn.minecraftsnap.config;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FactionUnitEntry {
	public String id = "";
	public String displayName = "";
	public boolean captainSpawnable = true;
	public int cost;
	public double maxHealth = 20.0;
	public double moveSpeedScale = 1.0;
	public UnitItemEntry mainHand = UnitItemEntry.create("minecraft:wooden_sword");
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

	public void normalize() {
		if (descriptionLines == null) {
			descriptionLines = new ArrayList<>();
		}
		if (mainHand == null) {
			mainHand = UnitItemEntry.create("minecraft:wooden_sword");
		}
		if (offHand == null) {
			offHand = new UnitItemEntry();
		}
		if (helmet == null) {
			helmet = new UnitItemEntry();
		}
		if (chest == null) {
			chest = new UnitItemEntry();
		}
		if (legs == null) {
			legs = new UnitItemEntry();
		}
		if (boots == null) {
			boots = new UnitItemEntry();
		}
		if (abilityItem == null) {
			abilityItem = new UnitItemEntry();
		}
		if (disguise == null) {
			disguise = new EntitySpecEntry();
		}
		if (advanceOptions == null) {
			advanceOptions = new ArrayList<>();
		}
		mainHand.normalize();
		offHand.normalize();
		helmet.normalize();
		chest.normalize();
		legs.normalize();
		boots.normalize();
		abilityItem.normalize();
		disguise.normalize();
		advanceOptions.forEach(AdvanceOptionEntry::normalize);
		if (ammoType == null || ammoType.isBlank()) {
			ammoType = UnitDefinition.AmmoType.NONE.name();
		}
	}

	public UnitDefinition toUnitDefinition(FactionId factionId) {
		return new UnitDefinition(
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
			abilityItem,
			abilityName,
			abilityCooldownSeconds,
			parseAmmoType(ammoType),
			disguise,
			List.copyOf(descriptionLines),
			List.copyOf(advanceOptions)
		);
	}

	private UnitDefinition.AmmoType parseAmmoType(String value) {
		return UnitDefinition.AmmoType.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
