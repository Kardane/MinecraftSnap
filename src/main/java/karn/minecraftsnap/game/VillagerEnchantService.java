package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.ShopEntry;
import karn.minecraftsnap.unit.UnitSpecSupport;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VillagerEnchantService {
	private static final String ENCHANT_STATE_PREFIX = "villager_enchant_";

	public boolean isEnchantEntry(ShopEntry entry) {
		return entry != null
			&& "enchant".equalsIgnoreCase(entry.type)
			&& entry.enchantmentId != null
			&& !entry.enchantmentId.isBlank()
			&& entry.target != null
			&& !entry.target.isBlank();
	}

	public boolean supportsConfiguredEnchant(ShopEntry entry) {
		return isEnchantEntry(entry) && resolveEnchantmentKey(entry.enchantmentId) != null;
	}

	public int currentLevel(PlayerMatchState state, ShopEntry entry) {
		if (state == null || !isEnchantEntry(entry)) {
			return 0;
		}
		return Math.max(0, state.getUnitRuntimeInt(stateKey(entry)) == null ? 0 : state.getUnitRuntimeInt(stateKey(entry)));
	}

	public boolean tryUpgrade(PlayerMatchState state, ShopEntry entry) {
		if (state == null || !isEnchantEntry(entry) || isMaxLevel(state, entry)) {
			return false;
		}
		state.setUnitRuntimeInt(stateKey(entry), currentLevel(state, entry) + 1);
		return true;
	}

	public void restoreLevel(PlayerMatchState state, ShopEntry entry, int level) {
		if (state == null || !isEnchantEntry(entry)) {
			return;
		}
		if (level <= 0) {
			state.removeUnitRuntimeInt(stateKey(entry));
			return;
		}
		state.setUnitRuntimeInt(stateKey(entry), level);
	}

	public boolean isMaxLevel(PlayerMatchState state, ShopEntry entry) {
		return currentLevel(state, entry) >= effectiveMaxLevel(entry);
	}

	public int priceForNextLevel(PlayerMatchState state, ShopEntry entry) {
		if (!isEnchantEntry(entry) || isMaxLevel(state, entry)) {
			return -1;
		}
		var prices = entry.prices == null || entry.prices.isEmpty() ? List.of(entry.price) : entry.prices;
		int index = Math.min(currentLevel(state, entry), prices.size() - 1);
		return prices.get(index);
	}

	boolean hasEligibleEquipment(
		UnitDefinition definition,
		ShopEntry entry,
		ItemStack mainHand,
		ItemStack helmet,
		ItemStack chest,
		ItemStack legs,
		ItemStack boots
	) {
		return !collectTargetStacks(definition, entry, mainHand, helmet, chest, legs, boots).isEmpty();
	}

	int eligibleEquipmentCount(
		UnitDefinition definition,
		ShopEntry entry,
		boolean hasMainHand,
		boolean hasHelmet,
		boolean hasChest,
		boolean hasLegs,
		boolean hasBoots
	) {
		if (!supportsConfiguredEnchant(entry) || definition == null) {
			return 0;
		}
		return switch (normalizeTarget(entry.target)) {
			case "weapon" -> hasConfiguredMainHand(definition) && hasMainHand ? 1 : 0;
			case "armor" -> (UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.HEAD) && hasHelmet ? 1 : 0)
				+ (UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.CHEST) && hasChest ? 1 : 0)
				+ (UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.LEGS) && hasLegs ? 1 : 0)
				+ (UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.FEET) && hasBoots ? 1 : 0);
			default -> 0;
		};
	}

	public boolean hasEligibleEquipment(ServerPlayerEntity player, PlayerMatchState state, ShopEntry entry) {
		if (player == null || state == null) {
			return false;
		}
		return hasEligibleEquipment(
			resolveUnitDefinition(state),
			entry,
			player.getMainHandStack(),
			player.getEquippedStack(EquipmentSlot.HEAD),
			player.getEquippedStack(EquipmentSlot.CHEST),
			player.getEquippedStack(EquipmentSlot.LEGS),
			player.getEquippedStack(EquipmentSlot.FEET)
		);
	}

	List<ItemStack> collectTargetStacks(
		UnitDefinition definition,
		ShopEntry entry,
		ItemStack mainHand,
		ItemStack helmet,
		ItemStack chest,
		ItemStack legs,
		ItemStack boots
	) {
		var targets = new ArrayList<ItemStack>();
		if (!supportsConfiguredEnchant(entry) || definition == null) {
			return targets;
		}
		return switch (normalizeTarget(entry.target)) {
			case "weapon" -> {
				if (hasConfiguredMainHand(definition) && mainHand != null && !mainHand.isEmpty()) {
					targets.add(mainHand);
				}
				yield targets;
			}
			case "armor" -> {
				addIfPresent(targets, helmet, UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.HEAD));
				addIfPresent(targets, chest, UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.CHEST));
				addIfPresent(targets, legs, UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.LEGS));
				addIfPresent(targets, boots, UnitLoadoutService.hasEffectiveArmorSlot(definition, EquipmentSlot.FEET));
				yield targets;
			}
			default -> targets;
		};
	}

	public boolean applyCurrentLevel(ServerPlayerEntity player, PlayerMatchState state, ShopEntry entry) {
		if (player == null || state == null || !supportsConfiguredEnchant(entry)) {
			return false;
		}
		int level = currentLevel(state, entry);
		if (level <= 0) {
			return false;
		}
		return applyLevel(player, resolveUnitDefinition(state), entry, level);
	}

	public void reapplyCurrentEnchantments(ServerPlayerEntity player, PlayerMatchState state, List<ShopEntry> entries) {
		if (player == null || state == null || entries == null || entries.isEmpty()) {
			return;
		}
		for (var entry : entries) {
			if (!supportsConfiguredEnchant(entry) || currentLevel(state, entry) <= 0) {
				continue;
			}
			applyCurrentLevel(player, state, entry);
		}
	}

	public String enchantLabel(ShopEntry entry) {
		if (entry == null || entry.enchantmentId == null) {
			return "강화";
		}
		return switch (entry.enchantmentId) {
			case "minecraft:sharpness" -> "날카로움";
			case "minecraft:protection" -> "보호";
			default -> entry.enchantmentId;
		};
	}

	public String targetLabel(ShopEntry entry) {
		if (entry == null) {
			return "장비";
		}
		return switch (normalizeTarget(entry.target)) {
			case "weapon" -> "무기";
			case "armor" -> "방어구";
			default -> "장비";
		};
	}

	public String levelText(int level) {
		if (level <= 0) {
			return "없음";
		}
		return switch (level) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> Integer.toString(level);
		};
	}

	private boolean applyLevel(ServerPlayerEntity player, UnitDefinition definition, ShopEntry entry, int level) {
		var enchantmentKey = resolveEnchantmentKey(entry.enchantmentId);
		if (enchantmentKey == null || definition == null) {
			return false;
		}
		var targets = collectTargetStacks(
			definition,
			entry,
			player.getMainHandStack(),
			player.getEquippedStack(EquipmentSlot.HEAD),
			player.getEquippedStack(EquipmentSlot.CHEST),
			player.getEquippedStack(EquipmentSlot.LEGS),
			player.getEquippedStack(EquipmentSlot.FEET)
		);
		if (targets.isEmpty()) {
			return false;
		}
		var world = player.getWorld() instanceof ServerWorld serverWorld ? serverWorld : null;
		if (world == null) {
			return false;
		}
		for (var stack : targets) {
			UnitSpecSupport.applyEnchantment(world, stack, enchantmentKey, level);
		}
		return true;
	}

	private RegistryKey<Enchantment> resolveEnchantmentKey(String enchantmentId) {
		if (enchantmentId == null || enchantmentId.isBlank()) {
			return null;
		}
		var identifier = Identifier.tryParse(enchantmentId);
		return identifier == null ? null : RegistryKey.of(RegistryKeys.ENCHANTMENT, identifier);
	}

	private int effectiveMaxLevel(ShopEntry entry) {
		if (entry == null || entry.maxLevel <= 0) {
			return 1;
		}
		return entry.maxLevel;
	}

	private String stateKey(ShopEntry entry) {
		if (entry != null && entry.id != null && !entry.id.isBlank()) {
			return ENCHANT_STATE_PREFIX + entry.id;
		}
		var raw = entry == null || entry.enchantmentId == null ? "unknown" : entry.enchantmentId;
		return ENCHANT_STATE_PREFIX + raw.replace(':', '_');
	}

	private String normalizeTarget(String target) {
		return target == null ? "" : target.toLowerCase(Locale.ROOT);
	}

	private boolean hasConfiguredMainHand(UnitDefinition definition) {
		return definition != null && definition.mainHand() != null && !definition.mainHand().isEmpty();
	}

	private UnitDefinition resolveUnitDefinition(PlayerMatchState state) {
		if (state == null || state.getCurrentUnitId() == null || state.getCurrentUnitId().isBlank()) {
			return null;
		}
		var mod = MinecraftSnap.getInstance();
		return mod == null ? null : mod.getUnitRegistry().get(state.getCurrentUnitId());
	}

	private void addIfPresent(List<ItemStack> targets, ItemStack stack, boolean eligibleSlot) {
		if (eligibleSlot && stack != null && !stack.isEmpty()) {
			targets.add(stack);
		}
	}
}
