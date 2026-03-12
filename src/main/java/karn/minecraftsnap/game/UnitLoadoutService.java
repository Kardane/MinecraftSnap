package karn.minecraftsnap.game;

import karn.minecraftsnap.config.FactionUnitEntry;
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
		applyBaseLoadout(player, definition, null, textTemplateResolver);
		applyBaseAttributes(player, definition);
	}

	public void applyBaseLoadout(ServerPlayerEntity player, UnitDefinition definition, FactionUnitEntry unitEntry, TextTemplateResolver textTemplateResolver) {
		player.getInventory().clear();
		resetCombatState(player);
		player.equipStack(EquipmentSlot.HEAD, createEquipment(player, unitEntry == null ? definition.helmet() : unitEntry.helmet, textTemplateResolver));
		player.equipStack(EquipmentSlot.CHEST, createEquipment(player, unitEntry == null ? definition.chest() : unitEntry.chest, textTemplateResolver));
		player.equipStack(EquipmentSlot.LEGS, createEquipment(player, unitEntry == null ? definition.legs() : unitEntry.legs, textTemplateResolver));
		player.equipStack(EquipmentSlot.FEET, createEquipment(player, unitEntry == null ? definition.boots() : unitEntry.boots, textTemplateResolver));
		player.equipStack(EquipmentSlot.MAINHAND, createEquipment(player, unitEntry == null ? definition.mainHand() : unitEntry.mainHand, textTemplateResolver));
		player.equipStack(EquipmentSlot.OFFHAND, createEquipment(player, unitEntry == null ? definition.offHand() : unitEntry.offHand, textTemplateResolver));
		if (definition.abilityItem() != null) {
			player.getInventory().insertStack(createAbilityItem(player, definition, unitEntry, textTemplateResolver));
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

	private ItemStack createAbilityItem(ServerPlayerEntity player, UnitDefinition definition, FactionUnitEntry unitEntry, TextTemplateResolver textTemplateResolver) {
		var spec = unitEntry == null ? definition.abilityItemSpec() : unitEntry.abilityItem;
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
