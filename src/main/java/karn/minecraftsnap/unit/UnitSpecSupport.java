package karn.minecraftsnap.unit;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

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

	public static UnitItemEntry item(String itemId, int count) {
		var entry = item(itemId);
		entry.count = count;
		entry.normalize();
		return entry;
	}

	public static UnitItemEntry item(String itemId, String componentsNbt) {
		var entry = item(itemId);
		entry.componentsNbt = componentsNbt == null ? "" : componentsNbt;
		entry.normalize();
		return entry;
	}

	public static UnitItemEntry stackItem(String itemId, String stackNbt) {
		var entry = item(itemId);
		entry.stackNbt = stackNbt == null ? "" : stackNbt;
		entry.normalize();
		return entry;
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

	public static EntitySpecEntry disguise(String entityId, String entityNbt) {
		var entry = EntitySpecEntry.create(entityId);
		entry.entityNbt = entityNbt == null ? "" : entityNbt;
		entry.normalize();
		return entry;
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

	public static void applyCombatProfile(ItemStack stack, String modifierKeyPrefix, double attackDamage, double attackSpeed) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		var builder = AttributeModifiersComponent.builder();
		builder.add(
			EntityAttributes.ATTACK_DAMAGE,
			new EntityAttributeModifier(
				Identifier.of(MinecraftSnap.MOD_ID, modifierKeyPrefix + "_attack_damage"),
				attackDamage - 1.0D,
				EntityAttributeModifier.Operation.ADD_VALUE
			),
			AttributeModifierSlot.MAINHAND
		);
		builder.add(
			EntityAttributes.ATTACK_SPEED,
			new EntityAttributeModifier(
				Identifier.of(MinecraftSnap.MOD_ID, modifierKeyPrefix + "_attack_speed"),
				attackSpeed - 4.0D,
				EntityAttributeModifier.Operation.ADD_VALUE
			),
			AttributeModifierSlot.MAINHAND
		);
		stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
	}

	public static void applyEnchantment(ServerWorld world, ItemStack stack, RegistryKey<Enchantment> enchantmentKey, int level) {
		if (world == null || stack == null || stack.isEmpty() || enchantmentKey == null || level <= 0) {
			return;
		}
		var enchantmentRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
		stack.addEnchantment(enchantmentRegistry.getOrThrow(enchantmentKey), level);
	}
}
