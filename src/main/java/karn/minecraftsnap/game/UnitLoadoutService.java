package karn.minecraftsnap.game;

import karn.minecraftsnap.config.FactionUnitEntry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;

public class UnitLoadoutService {
	public static final String CUSTOM_DATA_KIND = "minecraftsnap_kind";
	public static final String CUSTOM_DATA_UNIT_ID = "minecraftsnap_unit_id";
	public static final String CUSTOM_DATA_FACTION_ID = "minecraftsnap_faction_id";
	public static final String KIND_UNIT_ABILITY = "unit_ability";
	public static final String KIND_CAPTAIN_MENU = "captain_menu";
	public static final String KIND_CAPTAIN_SKILL = "captain_skill";

	public void applyUnitLoadout(ServerPlayerEntity player, UnitDefinition definition, TextTemplateResolver textTemplateResolver) {
		applyBaseLoadout(player, definition, null, textTemplateResolver);
		applyBaseAttributes(player, definition);
	}

	public void applyBaseLoadout(ServerPlayerEntity player, UnitDefinition definition, FactionUnitEntry unitEntry, TextTemplateResolver textTemplateResolver) {
		player.getInventory().clear();
		resetCombatState(player);
		player.equipStack(EquipmentSlot.HEAD, createEquipment(definition.helmetItem()));
		player.equipStack(EquipmentSlot.CHEST, createEquipment(definition.chestItem()));
		player.equipStack(EquipmentSlot.LEGS, createEquipment(definition.legsItem()));
		player.equipStack(EquipmentSlot.FEET, createEquipment(definition.bootsItem()));
		player.equipStack(EquipmentSlot.MAINHAND, createEquipment(
			definition.mainHandItem(),
			unitEntry == null ? "" : unitEntry.mainHand.displayName,
			unitEntry == null ? List.of() : unitEntry.mainHand.loreLines,
			textTemplateResolver
		));
		player.equipStack(EquipmentSlot.OFFHAND, createEquipment(
			definition.offHandItem(),
			unitEntry == null ? "" : unitEntry.offHand.displayName,
			unitEntry == null ? List.of() : unitEntry.offHand.loreLines,
			textTemplateResolver
		));
		if (definition.abilityItem() != null) {
			player.getInventory().insertStack(createAbilityItem(definition, unitEntry, textTemplateResolver));
		}
		restockAmmo(player, definition);
		player.setHealth((float) definition.maxHealth());
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(20.0f);
	}

	public void applyBaseAttributes(ServerPlayerEntity player, UnitDefinition definition) {
		var maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.setBaseValue(definition.maxHealth());
		}
		var moveSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (moveSpeed != null) {
			moveSpeed.setBaseValue(0.1D * definition.moveSpeedScale());
		}
		player.setHealth((float) definition.maxHealth());
	}

	public void giveCaptainItems(ServerPlayerEntity player, FactionId factionId, TextTemplateResolver textTemplateResolver) {
		player.getInventory().clear();
		resetCombatState(player);
		player.getInventory().insertStack(createCaptainMenuItem(factionId, textTemplateResolver));
		player.getInventory().insertStack(createCaptainSkillItem(factionId, textTemplateResolver));
		player.getInventory().setSelectedSlot(0);
	}

	public void resetCombatState(ServerPlayerEntity player) {
		var maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.setBaseValue(20.0D);
		}
		var moveSpeed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
		if (moveSpeed != null) {
			moveSpeed.setBaseValue(0.1D);
		}
		player.clearStatusEffects();
		player.setHealth(20.0f);
	}

	public void maintainLoadout(ServerPlayerEntity player, UnitDefinition definition) {
		resetDurability(player.getMainHandStack());
		resetDurability(player.getOffHandStack());
		resetDurability(player.getEquippedStack(EquipmentSlot.HEAD));
		resetDurability(player.getEquippedStack(EquipmentSlot.CHEST));
		resetDurability(player.getEquippedStack(EquipmentSlot.LEGS));
		resetDurability(player.getEquippedStack(EquipmentSlot.FEET));
		restockAmmo(player, definition);
	}

	public boolean isCaptainMenuItem(ItemStack stack) {
		return hasKind(stack, KIND_CAPTAIN_MENU);
	}

	public boolean isCaptainSkillItem(ItemStack stack) {
		return hasKind(stack, KIND_CAPTAIN_SKILL);
	}

	public boolean isUnitAbilityItem(ItemStack stack, String unitId) {
		if (!hasKind(stack, KIND_UNIT_ABILITY)) {
			return false;
		}
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		return customData != null && unitId.equals(customData.copyNbt().getString(CUSTOM_DATA_UNIT_ID));
	}

	private void restockAmmo(ServerPlayerEntity player, UnitDefinition definition) {
		if (definition.ammoType() == UnitDefinition.AmmoType.ARROW && !player.getInventory().contains(new ItemStack(Items.ARROW))) {
			player.getInventory().insertStack(new ItemStack(Items.ARROW));
		}
		if (definition.ammoType() == UnitDefinition.AmmoType.FIREWORK && !player.getInventory().contains(new ItemStack(Items.FIREWORK_ROCKET))) {
			player.getInventory().insertStack(new ItemStack(Items.FIREWORK_ROCKET));
		}
	}

	private ItemStack createCaptainMenuItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.BELL, KIND_CAPTAIN_MENU, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format("&e유닛 소환"));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			textTemplateResolver.format("&7우클릭 또는 Shift+F로 소환 GUI 열기"),
			textTemplateResolver.format("&8현재 팩션: &f" + factionLabel(factionId))
		)));
		return stack;
	}

	private ItemStack createCaptainSkillItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.NETHER_STAR, KIND_CAPTAIN_SKILL, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format("&d사령관 스킬"));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			textTemplateResolver.format("&7우클릭으로 팩션 전용 임시 스킬 발동"),
			textTemplateResolver.format("&8현재 팩션: &f" + factionLabel(factionId))
		)));
		return stack;
	}

	private ItemStack createAbilityItem(UnitDefinition definition, FactionUnitEntry unitEntry, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(definition.abilityItem(), KIND_UNIT_ABILITY, definition.factionId(), definition.id());
		var abilityName = unitEntry != null && unitEntry.abilityItem != null && !unitEntry.abilityItem.displayName.isBlank()
			? unitEntry.abilityItem.displayName
			: "&b" + definition.abilityName();
		var loreLines = unitEntry != null && unitEntry.abilityItem != null && !unitEntry.abilityItem.loreLines.isEmpty()
			? unitEntry.abilityItem.loreLines
			: List.of("&7유닛 스킬 발동", "&8쿨다운: &f" + definition.abilityCooldownSeconds() + "초");
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(abilityName));
		stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines.stream().map(textTemplateResolver::format).toList()));
		return stack;
	}

	private ItemStack createEquipment(Item item) {
		return createEquipment(item, "", List.of(), null);
	}

	private ItemStack createEquipment(Item item, String displayName, List<String> loreLines, TextTemplateResolver textTemplateResolver) {
		if (item == null) {
			return ItemStack.EMPTY;
		}
		var stack = new ItemStack(item);
		markUnbreakable(stack);
		if (textTemplateResolver != null && displayName != null && !displayName.isBlank()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(displayName));
		}
		if (textTemplateResolver != null && loreLines != null && !loreLines.isEmpty()) {
			stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines.stream().map(textTemplateResolver::format).toList()));
		}
		return stack;
	}

	private ItemStack createTaggedStack(Item item, String kind, FactionId factionId, String unitId) {
		var stack = createEquipment(item);
		var nbt = new NbtCompound();
		nbt.putString(CUSTOM_DATA_KIND, kind);
		if (factionId != null) {
			nbt.putString(CUSTOM_DATA_FACTION_ID, factionId.name());
		}
		if (unitId != null) {
			nbt.putString(CUSTOM_DATA_UNIT_ID, unitId);
		}
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
		return stack;
	}

	private boolean hasKind(ItemStack stack, String kind) {
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		return customData != null && kind.equals(customData.copyNbt().getString(CUSTOM_DATA_KIND));
	}

	private void markUnbreakable(ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
		if (stack.isDamageable()) {
			stack.setDamage(0);
		}
	}

	private void resetDurability(ItemStack stack) {
		if (!stack.isEmpty() && stack.isDamageable() && stack.isDamaged()) {
			stack.setDamage(0);
		}
	}

	private String factionLabel(FactionId factionId) {
		if (factionId == null) {
			return "미정";
		}
		return switch (factionId) {
			case VILLAGER -> "주민";
			case MONSTER -> "몬스터";
			case NETHER -> "네더";
		};
	}
}
