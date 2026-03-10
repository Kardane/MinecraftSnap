package karn.minecraftsnap.game;

import net.minecraft.item.Item;

public record UnitDefinition(
	String id,
	String displayName,
	FactionId factionId,
	boolean captainSpawnable,
	int cost,
	int spawnCooldownSeconds,
	double maxHealth,
	double moveSpeedScale,
	Item mainHandItem,
	Item offHandItem,
	Item helmetItem,
	Item chestItem,
	Item legsItem,
	Item bootsItem,
	Item abilityItem,
	String abilityName,
	int abilityCooldownSeconds,
	UnitAbilityType abilityType,
	UnitPassiveType passiveType,
	AmmoType ammoType,
	String disguiseId
) {
	public enum UnitAbilityType {
		NONE,
		HEAL_SELF,
		DASH,
		GIVE_FIREWORKS,
		BONE_BLAST,
		CREEPER_BOMB,
		ZOMBIFIED_PIGLIN_RAGE,
		BLAZE_BURST,
		BRUTE_FRENZY
	}

	public enum UnitPassiveType {
		NONE,
		ZOMBIE_COOLDOWN_REFUND,
		SLIME_AUTO_JUMP,
		SLIME_SPLIT,
		PIGLIN_ZOMBIFY_ON_DEATH
	}

	public enum AmmoType {
		NONE,
		ARROW,
		FIREWORK
	}
}
