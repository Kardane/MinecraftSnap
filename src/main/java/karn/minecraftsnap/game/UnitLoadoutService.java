package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
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
import net.minecraft.potion.Potions;

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
		var head = createEquipment(player, definition, EquipmentSlot.HEAD, definition.helmet(), textTemplateResolver);
		var chest = createEquipment(player, definition, EquipmentSlot.CHEST, definition.chest(), textTemplateResolver);
		var legs = createEquipment(player, definition, EquipmentSlot.LEGS, definition.legs(), textTemplateResolver);
		var feet = createEquipment(player, definition, EquipmentSlot.FEET, definition.boots(), textTemplateResolver);
		var mainHand = createEquipment(player, definition, EquipmentSlot.MAINHAND, definition.mainHand(), textTemplateResolver);
		var offHand = createEquipment(player, definition, EquipmentSlot.OFFHAND, definition.offHand(), textTemplateResolver);
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
		resetPlayerAttributes(player);
		setBaseValue(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), definition.maxHealth());
		setBaseValue(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), 0.1D * definition.moveSpeedScale());
		setBaseValue(
			player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE),
			definition.extraAttributes().attackRangeOrDefault(defaultAttackRange())
		);
		setBaseValue(
			player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE),
			definition.extraAttributes().knockbackResistanceOrDefault(defaultKnockbackResistance())
		);
		setBaseValue(
			player.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE),
			definition.extraAttributes().safeFallDistanceOrDefault(defaultSafeFallDistance())
		);
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
		resetPlayerAttributes(player);
		player.clearStatusEffects();
		player.setNoGravity(false);
		player.setHealth((float) defaultMaxHealth());
	}

	public void resetPlayerAttributes(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		setBaseValue(player.getAttributeInstance(EntityAttributes.MAX_HEALTH), defaultMaxHealth());
		setBaseValue(player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED), defaultMoveSpeed());
		setBaseValue(player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE), defaultAttackDamage());
		setBaseValue(player.getAttributeInstance(EntityAttributes.ATTACK_SPEED), defaultAttackSpeed());
		setBaseValue(player.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE), defaultAttackRange());
		setBaseValue(player.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE), defaultKnockbackResistance());
		setBaseValue(player.getAttributeInstance(EntityAttributes.SAFE_FALL_DISTANCE), defaultSafeFallDistance());
		setBaseValue(player.getAttributeInstance(EntityAttributes.SCALE), defaultScale());
		setBaseValue(player.getAttributeInstance(EntityAttributes.JUMP_STRENGTH), defaultJumpStrength());
		setBaseValue(player.getAttributeInstance(EntityAttributes.STEP_HEIGHT), defaultStepHeight());
	}

	public static double defaultMaxHealth() {
		return 20.0D;
	}

	public static double defaultMoveSpeed() {
		return 0.1D;
	}

	public static double defaultAttackDamage() {
		return 1.0D;
	}

	public static double defaultAttackSpeed() {
		return 4.0D;
	}

	public static double defaultAttackRange() {
		return 3.0D;
	}

	public static double defaultKnockbackResistance() {
		return 0.0D;
	}

	public static double defaultSafeFallDistance() {
		return 3.0D;
	}

	public static double defaultScale() {
		return 1.0D;
	}

	public static double defaultJumpStrength() {
		return 0.42D;
	}

	public static double defaultStepHeight() {
		return 0.6D;
	}

	public void maintainLoadout(ServerPlayerEntity player, UnitDefinition definition) {
		resetDurability(player.getMainHandStack());
		resetDurability(player.getOffHandStack());
		resetDurability(player.getEquippedStack(EquipmentSlot.HEAD));
		resetDurability(player.getEquippedStack(EquipmentSlot.CHEST));
		resetDurability(player.getEquippedStack(EquipmentSlot.LEGS));
		resetDurability(player.getEquippedStack(EquipmentSlot.FEET));
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
		var ammoStack = ammoStack(definition.ammoType());
		if (!ammoStack.isEmpty() && !player.getInventory().contains(ammoStack)) {
			player.getInventory().insertStack(ammoStack);
		}
	}

	ItemStack ammoStack(UnitDefinition.AmmoType ammoType) {
		if (ammoType == null) {
			return ItemStack.EMPTY;
		}
		return switch (ammoType) {
			case NONE -> ItemStack.EMPTY;
			case ARROW -> new ItemStack(Items.ARROW);
			case SLOWNESS_ARROW -> PotionContentsComponent.createStack(Items.TIPPED_ARROW, Potions.SLOWNESS);
			case POISON_ARROW -> PotionContentsComponent.createStack(Items.TIPPED_ARROW, Potions.POISON);
			case FIREWORK -> new ItemStack(Items.FIREWORK_ROCKET);
		};
	}

	ItemStack createCaptainMenuItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.BELL, KIND_CAPTAIN_MENU, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(textConfig().captainMenuItemName));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			textTemplateResolver.formatUi(textConfig().captainMenuItemUseLore),
			textTemplateResolver.formatUi(textConfig().captainMenuItemFactionLoreTemplate.replace("{faction}", factionLabel(factionId)))
		)));
		return stack;
	}

	ItemStack createCaptainSkillItem(FactionId factionId, TextTemplateResolver textTemplateResolver) {
		var stack = createTaggedStack(Items.NETHER_STAR, KIND_CAPTAIN_SKILL, factionId, null);
		stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(textConfig().captainSkillItemName));
		var lore = new java.util.ArrayList<net.minecraft.text.Text>();
		lore.add(textTemplateResolver.formatUi(textConfig().captainSkillItemUseLore));
		lore.add(textTemplateResolver.formatUi(textConfig().captainSkillItemFactionLoreTemplate.replace("{faction}", factionLabel(factionId))));
		var skillSpec = FactionSpecs.get(factionId);
		if (skillSpec != null) {
			lore.addAll(skillSpec.captainSkillDescriptionLines().stream()
				.limit(2)
				.map(textTemplateResolver::formatUi)
				.toList());
		}
		stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
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

	private ItemStack createEquipment(
		ServerPlayerEntity player,
		UnitDefinition definition,
		EquipmentSlot slot,
		UnitItemEntry spec,
		TextTemplateResolver textTemplateResolver
	) {
		var fallbackSpec = fallbackVillagerArmorSpec(definition, slot);
		var effectiveSpec = fallbackSpec.isEmpty() ? spec : fallbackSpec;
		var stack = createConfiguredStack(player, effectiveSpec, null, null, null, "", List.of(), textTemplateResolver);
		if (!fallbackSpec.isEmpty()) {
			applyFallbackVillagerArmorProfile(stack);
		}
		return stack;
	}

	public ItemStack createShopItem(ServerPlayerEntity player, UnitItemEntry spec, TextTemplateResolver textTemplateResolver) {
		if (spec == null || spec.isEmpty()) {
			return ItemStack.EMPTY;
		}
		var stack = createBaseStack(player, spec);
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		markUnbreakable(stack);
		if (textTemplateResolver != null && spec.displayName != null && !spec.displayName.isBlank()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(spec.displayName));
		}
		if (textTemplateResolver != null && spec.loreLines != null && !spec.loreLines.isEmpty()) {
			stack.set(DataComponentTypes.LORE, new LoreComponent(spec.loreLines.stream().map(textTemplateResolver::formatUi).toList()));
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
		if (sameTriggerItem(abilitySpec, definition.mainHand())) {
			return AbilityTriggerTarget.MAIN_HAND;
		}
		if (sameTriggerItem(abilitySpec, definition.offHand())) {
			return AbilityTriggerTarget.OFF_HAND;
		}
		return AbilityTriggerTarget.EXTRA_ITEM;
	}

	private boolean sameTriggerItem(UnitItemEntry left, UnitItemEntry right) {
		if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
			return false;
		}
		return left.sameSpec(right);
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
		var name = spec.displayName;
		if ((name == null || name.isBlank()) && fallbackDisplayName != null && !fallbackDisplayName.isBlank()) {
			// SNBT에 이미 이름이 포함되어 있는지 확인
			if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) {
				stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(fallbackDisplayName));
			}
		} else if (name != null && !name.isBlank()) {
			stack.set(DataComponentTypes.CUSTOM_NAME, textTemplateResolver.formatUi(name));
		}

		var loreLines = spec.loreLines;
		if ((loreLines == null || loreLines.isEmpty()) && fallbackLoreLines != null && !fallbackLoreLines.isEmpty()) {
			// SNBT에 이미 설명이 포함되어 있는지 확인
			if (!stack.contains(DataComponentTypes.LORE)) {
				stack.set(DataComponentTypes.LORE, new LoreComponent(fallbackLoreLines.stream().map(textTemplateResolver::formatUi).toList()));
			}
		} else if (loreLines != null && !loreLines.isEmpty()) {
			stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines.stream().map(textTemplateResolver::formatUi).toList()));
		}
		applyCustomData(stack, kind, factionId, unitId);
		return stack;
	}

	private ItemStack createBaseStack(ServerPlayerEntity player, UnitItemEntry spec) {
		try {
			if (!spec.stackNbt.isBlank()) {
				return ItemStack.CODEC.parse(registryOps(player), StringNbtReader.readCompound(normalizeLegacySnbt(spec.stackNbt)))
					.getOrThrow(message -> new IllegalArgumentException("잘못된 ItemStack SNBT: " + message));
			}
			var item = spec.resolve();
			if (item == null) {
				return ItemStack.EMPTY;
			}
			var stack = new ItemStack(item, spec.count);
			if (!spec.componentsNbt.isBlank()) {
				var changes = ComponentChanges.CODEC.parse(registryOps(player), StringNbtReader.readCompound(normalizeLegacySnbt(spec.componentsNbt)))
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

	static String normalizeLegacySnbt(String raw) {
		if (raw == null || raw.isBlank()) {
			return raw == null ? "" : raw;
		}
		var source = raw.strip();
		if (source.startsWith("[") && source.endsWith("]")) {
			source = "{" + source.substring(1, source.length() - 1) + "}";
		}
		if (source.indexOf('=') < 0) {
			return source;
		}
		var builder = new StringBuilder(source.length());
		boolean inString = false;
		boolean escaped = false;
		for (int index = 0; index < source.length(); index++) {
			char current = source.charAt(index);
			if (escaped) {
				builder.append(current);
				escaped = false;
				continue;
			}
			if (current == '\\' && inString) {
				builder.append(current);
				escaped = true;
				continue;
			}
			if (current == '"') {
				builder.append(current);
				inString = !inString;
				continue;
			}
			builder.append(current == '=' && !inString ? ':' : current);
		}
		return builder.toString().replaceAll("(?<=[\\{,])\\s*([A-Za-z0-9_:.+-]+)\\s*:", "\"$1\":");
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
		stack.set(DataComponentTypes.TOOLTIP_DISPLAY, hideTooltipComponent(stack.get(DataComponentTypes.TOOLTIP_DISPLAY), DataComponentTypes.UNBREAKABLE));
		if (stack.isDamageable()) {
			stack.setDamage(0);
		}
	}

	static UnitItemEntry fallbackVillagerArmorSpec(UnitDefinition definition, EquipmentSlot slot) {
		if (definition == null || definition.factionId() != FactionId.VILLAGER || slot == null) {
			return new UnitItemEntry();
		}
		return switch (slot) {
			case HEAD -> emptyOrFallback(definition.helmet(), "minecraft:leather_helmet");
			case CHEST -> emptyOrFallback(definition.chest(), "minecraft:leather_chestplate");
			case LEGS -> emptyOrFallback(definition.legs(), "minecraft:leather_leggings");
			case FEET -> emptyOrFallback(definition.boots(), "minecraft:leather_boots");
			default -> new UnitItemEntry();
		};
	}

	static boolean hasEffectiveArmorSlot(UnitDefinition definition, EquipmentSlot slot) {
		if (definition == null || slot == null) {
			return false;
		}
		return switch (slot) {
			case HEAD -> (definition.helmet() != null && !definition.helmet().isEmpty()) || !fallbackVillagerArmorSpec(definition, slot).isEmpty();
			case CHEST -> (definition.chest() != null && !definition.chest().isEmpty()) || !fallbackVillagerArmorSpec(definition, slot).isEmpty();
			case LEGS -> (definition.legs() != null && !definition.legs().isEmpty()) || !fallbackVillagerArmorSpec(definition, slot).isEmpty();
			case FEET -> (definition.boots() != null && !definition.boots().isEmpty()) || !fallbackVillagerArmorSpec(definition, slot).isEmpty();
			default -> false;
		};
	}

	private static UnitItemEntry emptyOrFallback(UnitItemEntry configured, String fallbackItemId) {
		if (configured != null && !configured.isEmpty()) {
			return new UnitItemEntry();
		}
		return UnitItemEntry.create(fallbackItemId);
	}

	private void applyFallbackVillagerArmorProfile(ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.builder().build());
		stack.set(DataComponentTypes.TOOLTIP_DISPLAY, hideTooltipComponent(stack.get(DataComponentTypes.TOOLTIP_DISPLAY), DataComponentTypes.ATTRIBUTE_MODIFIERS));
	}

	static TooltipDisplayComponent hideTooltipComponent(TooltipDisplayComponent tooltipDisplay, net.minecraft.component.ComponentType<?> componentType) {
		var effectiveDisplay = tooltipDisplay == null ? TooltipDisplayComponent.DEFAULT : tooltipDisplay;
		return effectiveDisplay.with(componentType, true);
	}

	private void resetDurability(ItemStack stack) {
		if (!stack.isEmpty() && stack.isDamageable() && stack.isDamaged()) {
			stack.setDamage(0);
		}
	}

	private void setBaseValue(EntityAttributeInstance attribute, double value) {
		if (attribute != null) {
			attribute.setBaseValue(value);
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
