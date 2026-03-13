package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
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
		applyBaseLoadout(player, definition, textTemplateResolver);
		applyBaseAttributes(player, definition);
	}

	public void applyBaseLoadout(ServerPlayerEntity player, UnitDefinition definition, TextTemplateResolver textTemplateResolver) {
		player.getInventory().clear();
		resetCombatState(player);
		var head = createEquipment(player, definition.helmet(), textTemplateResolver);
		var chest = createEquipment(player, definition.chest(), textTemplateResolver);
		var legs = createEquipment(player, definition.legs(), textTemplateResolver);
		var feet = createEquipment(player, definition.boots(), textTemplateResolver);
		var mainHand = createEquipment(player, definition.mainHand(), textTemplateResolver);
		var offHand = createEquipment(player, definition.offHand(), textTemplateResolver);
		var abilityTriggerTarget = determineAbilityTriggerTarget(definition);
		switch (abilityTriggerTarget) {
			case MAIN_HAND -> tagAbilityTrigger(mainHand, definition);
			case OFF_HAND -> tagAbilityTrigger(offHand, definition);
			case EXTRA_ITEM -> {
			}
			case NONE -> {
			}
		}
		player.equipStack(EquipmentSlot.HEAD, head);
		player.equipStack(EquipmentSlot.CHEST, chest);
		player.equipStack(EquipmentSlot.LEGS, legs);
		player.equipStack(EquipmentSlot.FEET, feet);
		player.equipStack(EquipmentSlot.MAINHAND, mainHand);
		player.equipStack(EquipmentSlot.OFFHAND, offHand);
		if (abilityTriggerTarget == AbilityTriggerTarget.EXTRA_ITEM) {
			player.getInventory().insertStack(createAbilityItem(player, definition, textTemplateResolver));
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

	public boolean matchesCaptainMenuTrigger(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return isCaptainMenuItem(stack) || matchesCaptainMenuTriggerItemId(Registries.ITEM.getId(stack.getItem()).toString());
	}

	public boolean matchesCaptainSkillTrigger(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		return isCaptainSkillItem(stack) || matchesCaptainSkillTriggerItemId(Registries.ITEM.getId(stack.getItem()).toString());
	}

	public boolean isUnitAbilityItem(ItemStack stack, String unitId) {
		if (!hasKind(stack, KIND_UNIT_ABILITY)) {
			return false;
		}
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		return customData != null && unitId.equals(customData.copyNbt().getString(CUSTOM_DATA_UNIT_ID));
	}

	public boolean matchesUnitAbilityTrigger(ItemStack stack, UnitDefinition definition) {
		if (stack == null || stack.isEmpty() || definition == null || !definition.hasActiveSkill()) {
			return false;
		}
		if (isUnitAbilityItem(stack, definition.id())) {
			return true;
		}
		return matchesUnitAbilityTriggerItemId(Registries.ITEM.getId(stack.getItem()).toString(), definition);
	}

	boolean matchesUnitAbilityTriggerItemId(String itemId, UnitDefinition definition) {
		if (itemId == null || itemId.isBlank() || definition == null || !definition.hasActiveSkill()) {
			return false;
		}
		return switch (determineAbilityTriggerTarget(definition)) {
			case MAIN_HAND -> itemIdMatchesSpec(itemId, definition.mainHand());
			case OFF_HAND -> itemIdMatchesSpec(itemId, definition.offHand());
			case EXTRA_ITEM -> itemIdMatchesSpec(itemId, definition.abilityItemSpec());
			case NONE -> false;
		};
	}

	boolean matchesCaptainMenuTriggerItemId(String itemId) {
		return "minecraft:bell".equals(itemId);
	}

	boolean matchesCaptainSkillTriggerItemId(String itemId) {
		return "minecraft:nether_star".equals(itemId);
	}

	private void restockAmmo(ServerPlayerEntity player, UnitDefinition definition) {
		if (definition.ammoType() == UnitDefinition.AmmoType.ARROW && !player.getInventory().contains(new ItemStack(Items.ARROW))) {
			player.getInventory().insertStack(new ItemStack(Items.ARROW));
		}
		if (definition.ammoType() == UnitDefinition.AmmoType.FIREWORK && !player.getInventory().contains(new ItemStack(Items.FIREWORK_ROCKET))) {
			player.getInventory().insertStack(new ItemStack(Items.FIREWORK_ROCKET));
		}
	}

	ItemStack createCaptainMenuItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.BELL, KIND_CAPTAIN_MENU, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(textConfig().captainMenuItemName));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			textTemplateResolver.format(textConfig().captainMenuItemUseLore),
			textTemplateResolver.format(textConfig().captainMenuItemFactionLoreTemplate.replace("{faction}", factionLabel(factionId)))
		)));
		return stack;
	}

	ItemStack createCaptainSkillItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.NETHER_STAR, KIND_CAPTAIN_SKILL, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(textConfig().captainSkillItemName));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			textTemplateResolver.format(textConfig().captainSkillItemUseLore),
			textTemplateResolver.format(textConfig().captainSkillItemFactionLoreTemplate.replace("{faction}", factionLabel(factionId)))
		)));
		return stack;
	}

	private ItemStack createAbilityItem(ServerPlayerEntity player, UnitDefinition definition, TextTemplateResolver textTemplateResolver) {
		var spec = definition.abilityItemSpec();
		var abilityName = spec != null && !spec.displayName.isBlank()
			? spec.displayName
			: "&b" + definition.abilityName();
		var loreLines = spec != null && !spec.loreLines.isEmpty()
			? spec.loreLines
			: List.of("&7유닛 스킬 발동", "&8쿨다운: &f" + definition.abilityCooldownSeconds() + "초");
		return createConfiguredStack(player, spec, KIND_UNIT_ABILITY, definition.factionId(), definition.id(), abilityName, loreLines, textTemplateResolver);
	}

	private ItemStack createEquipment(ServerPlayerEntity player, UnitItemEntry spec, TextTemplateResolver textTemplateResolver) {
		return createConfiguredStack(player, spec, null, null, null, "", List.of(), textTemplateResolver);
	}

	public ItemStack createShopItem(ServerPlayerEntity player, UnitItemEntry spec, TextTemplateResolver textTemplateResolver) {
		if (spec == null || spec.isEmpty()) {
			return ItemStack.EMPTY;
		}
		var stack = createBaseStack(player, spec);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (textTemplateResolver != null && spec.displayName != null && !spec.displayName.isBlank()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(spec.displayName));
		}
		if (textTemplateResolver != null && spec.loreLines != null && !spec.loreLines.isEmpty()) {
			stack.set(DataComponentTypes.LORE, new LoreComponent(spec.loreLines.stream().map(textTemplateResolver::format).toList()));
		}
		return stack;
	}

	AbilityTriggerTarget determineAbilityTriggerTarget(UnitDefinition definition) {
		if (definition == null || !definition.hasActiveSkill()) {
			return AbilityTriggerTarget.NONE;
		}
		var abilitySpec = definition.abilityItemSpec();
		if (abilitySpec == null || abilitySpec.isEmpty()) {
			if (definition.mainHand() != null && !definition.mainHand().isEmpty()) {
				return AbilityTriggerTarget.MAIN_HAND;
			}
			if (definition.offHand() != null && !definition.offHand().isEmpty()) {
				return AbilityTriggerTarget.OFF_HAND;
			}
			return AbilityTriggerTarget.NONE;
		}
		if (definition.mainHand() != null && abilitySpec.sameSpec(definition.mainHand())) {
			return AbilityTriggerTarget.MAIN_HAND;
		}
		if (definition.offHand() != null && abilitySpec.sameSpec(definition.offHand())) {
			return AbilityTriggerTarget.OFF_HAND;
		}
		return AbilityTriggerTarget.EXTRA_ITEM;
	}

	private ItemStack createTaggedStack(Item item, String kind, FactionId factionId, String unitId) {
		var stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
		markUnbreakable(stack);
		applyCustomData(stack, kind, factionId, unitId);
		return stack;
	}

	ItemStack createConfiguredStack(
		ServerPlayerEntity player,
		UnitItemEntry spec,
		String kind,
		FactionId factionId,
		String unitId,
		String fallbackDisplayName,
		List<String> fallbackLoreLines,
		TextTemplateResolver textTemplateResolver
	) {
		if (spec == null || spec.isEmpty()) {
			return ItemStack.EMPTY;
		}
		var stack = createBaseStack(player, spec);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		markUnbreakable(stack);
		var displayName = spec.displayName.isBlank() ? fallbackDisplayName : spec.displayName;
		var loreLines = spec.loreLines.isEmpty() ? fallbackLoreLines : spec.loreLines;
		if (textTemplateResolver != null && displayName != null && !displayName.isBlank()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.format(displayName));
		}
		if (textTemplateResolver != null && loreLines != null && !loreLines.isEmpty()) {
			stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines.stream().map(textTemplateResolver::format).toList()));
		}
		applyCustomData(stack, kind, factionId, unitId);
		return stack;
	}

	private ItemStack createBaseStack(ServerPlayerEntity player, UnitItemEntry spec) {
		try {
			if (!spec.stackNbt.isBlank()) {
				return ItemStack.CODEC.parse(registryOps(player), StringNbtReader.readCompound(spec.stackNbt))
					.getOrThrow(message -> new IllegalArgumentException("잘못된 ItemStack SNBT: " + message));
			}
			var item = spec.resolve();
			if (item == null) {
				return ItemStack.EMPTY;
			}
			var stack = new ItemStack(item, spec.count);
			if (!spec.componentsNbt.isBlank()) {
				var changes = ComponentChanges.CODEC.parse(registryOps(player), StringNbtReader.readCompound(spec.componentsNbt))
					.getOrThrow(message -> new IllegalArgumentException("잘못된 components SNBT: " + message));
				stack.applyChanges(changes);
			}
			return stack;
		} catch (Exception exception) {
			throw new IllegalArgumentException("유닛 아이템 구성 파싱 실패: " + spec.itemId, exception);
		}
	}

	private RegistryOps<net.minecraft.nbt.NbtElement> registryOps(ServerPlayerEntity player) {
		if (player == null || player.getServer() == null) {
			throw new IllegalArgumentException("아이템 SNBT 파싱에는 서버 컨텍스트가 필요함");
		}
		return RegistryOps.of(NbtOps.INSTANCE, player.getServer().getRegistryManager());
	}

	private void applyCustomData(ItemStack stack, String kind, FactionId factionId, String unitId) {
		if (stack.isEmpty() || kind == null || kind.isBlank()) {
			return;
		}
		var existing = stack.get(DataComponentTypes.CUSTOM_DATA);
		var nbt = existing == null ? new NbtCompound() : existing.copyNbt();
		nbt.putString(CUSTOM_DATA_KIND, kind);
		if (factionId != null) {
			nbt.putString(CUSTOM_DATA_FACTION_ID, factionId.name());
		}
		if (unitId != null) {
			nbt.putString(CUSTOM_DATA_UNIT_ID, unitId);
		}
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
	}

	private void tagAbilityTrigger(ItemStack stack, UnitDefinition definition) {
		if (stack.isEmpty() || definition == null) {
			return;
		}
		applyCustomData(stack, KIND_UNIT_ABILITY, definition.factionId(), definition.id());
	}

	private boolean itemIdMatchesSpec(String itemId, UnitItemEntry spec) {
		if (itemId == null || itemId.isBlank() || spec == null || spec.isEmpty()) {
			return false;
		}
		if (spec.itemId != null && !spec.itemId.isBlank()) {
			return itemId.equals(spec.itemId);
		}
		var item = spec.resolve();
		return item != null && itemId.equals(Registries.ITEM.getId(item).toString());
	}

	private boolean hasKind(ItemStack stack, String kind) {
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		return customData != null && kind.equals(customData.copyNbt().getString(CUSTOM_DATA_KIND));
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
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

	enum AbilityTriggerTarget {
		NONE,
		MAIN_HAND,
		OFF_HAND,
		EXTRA_ITEM
	}
}
