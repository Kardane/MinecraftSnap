package karn.minecraftsnap.unit;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;

import java.util.List;

public final class UnitSpecSupport {
	private UnitSpecSupport() {
	}

	public static UnitDefinition unit(
		String id,
		String displayName,
		FactionId factionId,
		boolean captainSpawnable,
		int cost,
		int spawnCooldownSeconds,
		double maxHealth,
		double moveSpeedScale,
		UnitItemEntry mainHand,
		UnitItemEntry offHand,
		UnitItemEntry helmet,
		UnitItemEntry chest,
		UnitItemEntry legs,
		UnitItemEntry boots,
		UnitItemEntry abilityItem,
		String abilityName,
		int abilityCooldownSeconds,
		UnitDefinition.AmmoType ammoType,
		EntitySpecEntry disguise,
		List<String> descriptionLines,
		List<AdvanceOptionEntry> advanceOptions
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
			mainHand,
			offHand,
			helmet,
			chest,
			legs,
			boots,
			abilityItem,
			abilityName,
			abilityCooldownSeconds,
			ammoType,
			disguise,
			descriptionLines,
			advanceOptions
		);
	}

	public static UnitItemEntry item(String itemId) {
		return UnitItemEntry.create(itemId);
	}

	public static UnitItemEntry none() {
		return new UnitItemEntry();
	}

	public static UnitItemEntry abilityItem(String itemId, String abilityName, int abilityCooldownSeconds) {
		var entry = item(itemId);
		if (abilityName != null && !abilityName.isBlank()) {
			entry.displayName = "&b" + abilityName;
			entry.loreLines = List.of("&7유닛 스킬 발동", "&8쿨다운: &f" + abilityCooldownSeconds + "초");
		}
		return entry;
	}

	public static EntitySpecEntry disguise(String entityId) {
		return EntitySpecEntry.create(entityId);
	}

	public static AdvanceOptionEntry advanceOption(
		String resultUnitId,
		String displayName,
		List<String> descriptionLines,
		List<String> biomes,
		List<String> weathers,
		int requiredTicks
	) {
		var option = new AdvanceOptionEntry();
		option.resultUnitId = resultUnitId;
		option.displayName = displayName;
		option.descriptionLines = descriptionLines;
		option.biomes = biomes;
		option.weathers = weathers;
		option.requiredTicks = requiredTicks;
		option.normalize();
		return option;
	}
}
