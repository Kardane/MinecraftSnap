package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.ShopConfigFile;
import karn.minecraftsnap.config.ShopEntry;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.game.VillagerEnchantService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class TradeGuiService {
	private static final List<Integer> MERCHANDISE_SLOTS = java.util.stream.IntStream.range(0, 27).boxed().toList();

	private final TextTemplateResolver textTemplateResolver;
	private final UnitLoadoutService unitLoadoutService;
	private final Function<FactionId, ShopConfigFile> shopConfigProvider;
	private final Supplier<StatsRepository> statsRepositorySupplier;
	private final UiSoundService uiSoundService;
	private final VillagerEnchantService villagerEnchantService;

	public TradeGuiService(
		TextTemplateResolver textTemplateResolver,
		UnitLoadoutService unitLoadoutService,
		Function<FactionId, ShopConfigFile> shopConfigProvider,
		Supplier<StatsRepository> statsRepositorySupplier
	) {
		this(textTemplateResolver, unitLoadoutService, shopConfigProvider, statsRepositorySupplier, null, new VillagerEnchantService());
	}

	public TradeGuiService(
		TextTemplateResolver textTemplateResolver,
		UnitLoadoutService unitLoadoutService,
		Function<FactionId, ShopConfigFile> shopConfigProvider,
		Supplier<StatsRepository> statsRepositorySupplier,
		UiSoundService uiSoundService
	) {
		this(textTemplateResolver, unitLoadoutService, shopConfigProvider, statsRepositorySupplier, uiSoundService, new VillagerEnchantService());
	}

	public TradeGuiService(
		TextTemplateResolver textTemplateResolver,
		UnitLoadoutService unitLoadoutService,
		Function<FactionId, ShopConfigFile> shopConfigProvider,
		Supplier<StatsRepository> statsRepositorySupplier,
		UiSoundService uiSoundService,
		VillagerEnchantService villagerEnchantService
	) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitLoadoutService = unitLoadoutService;
		this.shopConfigProvider = shopConfigProvider;
		this.statsRepositorySupplier = statsRepositorySupplier;
		this.uiSoundService = uiSoundService;
		this.villagerEnchantService = villagerEnchantService;
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state) {
		openInternal(player, state, resolveSupportedFaction(state == null ? null : state.getFactionId()));
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		openInternal(player, state, resolveSupportedFaction(factionId));
	}

	public void openVillagerShop(ServerPlayerEntity player, PlayerMatchState state) {
		open(player, state, FactionId.VILLAGER);
	}

	public void openNetherShop(ServerPlayerEntity player, PlayerMatchState state) {
		open(player, state, FactionId.NETHER);
	}

	static List<Integer> merchandiseSlots() {
		return MERCHANDISE_SLOTS;
	}

	static FactionId resolveSupportedFaction(FactionId factionId) {
		if (factionId == null) {
			return null;
		}
		return switch (factionId) {
			case VILLAGER, NETHER -> factionId;
			default -> null;
		};
	}

	PurchaseResult completePurchase(
		FactionId factionId,
		PlayerMatchState state,
		UUID playerId,
		String playerName,
		int price,
		int inventoryCurrencyAvailable,
		Runnable inventoryCurrencySpendAction,
		BooleanSupplier rewardGrantAction
	) {
		var inventoryCurrencySpent = Math.min(price, Math.max(0, inventoryCurrencyAvailable));
		if (inventoryCurrencySpent < price) {
			return PurchaseResult.INSUFFICIENT_FUNDS;
		}
		if (!rewardGrantAction.getAsBoolean()) {
			return PurchaseResult.INVENTORY_FULL;
		}
		if (inventoryCurrencySpendAction != null && inventoryCurrencySpent > 0) {
			inventoryCurrencySpendAction.run();
		}
		switch (factionId) {
			case VILLAGER, NETHER -> {
			}
			default -> {
				return PurchaseResult.UNSUPPORTED_FACTION;
			}
		}
		return PurchaseResult.SUCCESS;
	}

	private void openInternal(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		if (player == null || state == null || factionId == null) {
			return;
		}
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.formatUi(resolveTitle(factionId)));
		renderMerchandise(gui, player, state, factionId);
		gui.open();
	}

	private void renderMerchandise(SimpleGui gui, ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		var config = shopConfigProvider == null ? null : shopConfigProvider.apply(factionId);
		var entries = config == null
			? List.<ShopEntry>of()
			: config.entries.stream().filter(entry -> supportsEntryType(factionId, entry)).toList();
		for (int index = 0; index < Math.min(entries.size(), MERCHANDISE_SLOTS.size()); index++) {
			var entry = entries.get(index);
			var displayStack = buildDisplayStack(player, entry);
			if (displayStack.isEmpty()) {
				continue;
			}
			mergeShopLore(displayStack, factionId, state, entry);
			var builder = new GuiElementBuilder(displayStack)
				.setCallback((slotIndex, clickType, action, slotGui) -> {
					if (uiSoundService != null) {
						uiSoundService.playUiClick(player);
					}
					var result = purchase(player, state, factionId, entry);
					switch (result) {
						case SUCCESS -> {
							if (uiSoundService != null) {
								uiSoundService.playUiConfirm(player);
							}
							if (villagerEnchantService.isEnchantEntry(entry)) {
								playEnchantUpgradeFeedback(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradePurchaseSuccessMessage), false);
						}
						case INSUFFICIENT_FUNDS -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeInsufficientFundsMessage), false);
						}
						case INVENTORY_FULL -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeInventoryFullMessage), false);
						}
						case INVALID_ENTRY -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeInvalidEntryMessage), false);
						}
						case MAX_LEVEL -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeMaxLevelMessage), false);
						}
						case NO_ELIGIBLE_EQUIPMENT -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeNoEligibleEquipmentMessage), false);
						}
						case UNSUPPORTED_FACTION -> {
							if (uiSoundService != null) {
								uiSoundService.playUiDeny(player);
							}
							player.sendMessage(textTemplateResolver.format(textConfig().tradeUnsupportedFactionMessage), false);
						}
					}
				});
			gui.setSlot(MERCHANDISE_SLOTS.get(index), builder.build());
		}
	}

	private void mergeShopLore(ItemStack displayStack, FactionId factionId, PlayerMatchState state, ShopEntry entry) {
		var mergedLore = new ArrayList<net.minecraft.text.Text>();
		var existingLore = displayStack.get(net.minecraft.component.DataComponentTypes.LORE);
		if (existingLore != null) {
			mergedLore.addAll(existingLore.lines());
		}
		if (villagerEnchantService.isEnchantEntry(entry)) {
			var currentLevel = villagerEnchantService.currentLevel(state, entry);
			mergedLore.add(textTemplateResolver.formatUi(textConfig().tradeEnchantTargetLoreTemplate
				.replace("{target}", villagerEnchantService.targetLabel(entry))));
			mergedLore.add(textTemplateResolver.formatUi(textConfig().tradeEnchantCurrentLoreTemplate
				.replace("{label}", villagerEnchantService.enchantLabel(entry))
				.replace("{level}", villagerEnchantService.levelText(currentLevel))));
			if (villagerEnchantService.isMaxLevel(state, entry)) {
				mergedLore.add(textTemplateResolver.formatUi(textConfig().tradeEnchantMaxLore));
			} else {
				mergedLore.add(textTemplateResolver.formatUi(textConfig().tradeEnchantNextLoreTemplate
					.replace("{label}", villagerEnchantService.enchantLabel(entry))
					.replace("{level}", villagerEnchantService.levelText(currentLevel + 1))));
				mergedLore.add(textTemplateResolver.formatUi(textConfig().tradePriceLoreTemplate
					.replace("{color}", currencyColor(factionId))
					.replace("{price}", Integer.toString(villagerEnchantService.priceForNextLevel(state, entry)))));
			}
		} else {
			mergedLore.add(textTemplateResolver.formatUi(textConfig().tradePriceLoreTemplate
				.replace("{color}", currencyColor(factionId))
				.replace("{price}", Integer.toString(entry.price))));
		}
		mergedLore.add(textTemplateResolver.formatUi(textConfig().tradeClickLore));
		displayStack.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(mergedLore));
	}

	private PurchaseResult purchase(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId, ShopEntry entry) {
		if (!supportsEntryType(factionId, entry)) {
			return PurchaseResult.INVALID_ENTRY;
		}
		if (villagerEnchantService.isEnchantEntry(entry)) {
			return purchaseEnchant(player, state, factionId, entry);
		}
		if (entry == null || entry.item == null || entry.item.isEmpty()) {
			return PurchaseResult.INVALID_ENTRY;
		}
		ItemStack reward;
		try {
			reward = unitLoadoutService.createShopItem(player, entry.item, textTemplateResolver);
		} catch (IllegalArgumentException exception) {
			return PurchaseResult.INVALID_ENTRY;
		}
		if (reward.isEmpty()) {
			return PurchaseResult.INVALID_ENTRY;
		}
		var inventoryCurrencyAvailable = countInventoryCurrency(factionId, inventoryStacks(player));
		var inventoryCurrencySpent = Math.min(entry.price, inventoryCurrencyAvailable);
		return completePurchase(
			factionId,
			state,
			player.getUuid(),
			player.getName().getString(),
			entry.price,
			inventoryCurrencyAvailable,
			() -> consumeInventoryCurrency(factionId, inventoryStacks(player), inventoryCurrencySpent),
			() -> tryInsertReward(player, reward)
		);
	}

	private PurchaseResult purchaseEnchant(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId, ShopEntry entry) {
		if (factionId != FactionId.VILLAGER) {
			return PurchaseResult.UNSUPPORTED_FACTION;
		}
		if (!villagerEnchantService.supportsConfiguredEnchant(entry) || player == null || state == null) {
			return PurchaseResult.INVALID_ENTRY;
		}
		if (villagerEnchantService.isMaxLevel(state, entry)) {
			return PurchaseResult.MAX_LEVEL;
		}
		if (!villagerEnchantService.hasEligibleEquipment(player, state, entry)) {
			return PurchaseResult.NO_ELIGIBLE_EQUIPMENT;
		}
		var price = villagerEnchantService.priceForNextLevel(state, entry);
		if (price <= 0) {
			return PurchaseResult.INVALID_ENTRY;
		}
		var stacks = inventoryStacks(player);
		var inventoryCurrencyAvailable = countInventoryCurrency(factionId, stacks);
		if (inventoryCurrencyAvailable < price) {
			return PurchaseResult.INSUFFICIENT_FUNDS;
		}
		int previousLevel = villagerEnchantService.currentLevel(state, entry);
		if (!villagerEnchantService.tryUpgrade(state, entry)) {
			return PurchaseResult.MAX_LEVEL;
		}
		if (!villagerEnchantService.applyCurrentLevel(player, state, entry)) {
			villagerEnchantService.restoreLevel(state, entry, previousLevel);
			return PurchaseResult.INVALID_ENTRY;
		}
		consumeInventoryCurrency(factionId, stacks, price);
		return PurchaseResult.SUCCESS;
	}

	boolean supportsEntryType(FactionId factionId, ShopEntry entry) {
		if (factionId == null || entry == null) {
			return false;
		}
		return switch (factionId) {
			case VILLAGER -> villagerEnchantService.isEnchantEntry(entry);
			case NETHER -> "item".equalsIgnoreCase(entry.type)
				&& entry.item != null
				&& !entry.item.isEmpty();
			default -> false;
		};
	}

	private ItemStack buildDisplayStack(ServerPlayerEntity player, ShopEntry entry) {
		if (entry == null || entry.item == null || entry.item.isEmpty()) {
			return ItemStack.EMPTY;
		}
		try {
			return unitLoadoutService.createShopItem(player, entry.item, textTemplateResolver);
		} catch (IllegalArgumentException exception) {
			return ItemStack.EMPTY;
		}
	}

	private void playEnchantUpgradeFeedback(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.9f, 1.1f);
		if (player.getWorld() instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getBodyY(0.75D), player.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.0D);
		}
	}

	private boolean tryInsertReward(ServerPlayerEntity player, ItemStack reward) {
		if (!canFit(player, reward)) {
			return false;
		}
		return player.getInventory().insertStack(reward.copy());
	}

	private boolean canFit(ServerPlayerEntity player, ItemStack reward) {
		int remaining = reward.getCount();
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.size(); slot++) {
			var existing = inventory.getStack(slot);
			if (existing.isEmpty()) {
				remaining -= reward.getMaxCount();
			} else if (ItemStack.areItemsAndComponentsEqual(existing, reward)) {
				remaining -= Math.max(0, existing.getMaxCount() - existing.getCount());
			}
			if (remaining <= 0) {
				return true;
			}
		}
		return false;
	}

	static int countInventoryCurrency(FactionId factionId, List<ItemStack> stacks) {
		var entries = stacks.stream()
			.map(stack -> new CurrencyEntry(stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).toString(), stack.getCount()))
			.toList();
		return countCurrencyEntries(factionId, entries);
	}

	static int countCurrencyEntries(FactionId factionId, List<CurrencyEntry> entries) {
		if (entries == null || factionId == null) {
			return 0;
		}
		var currencyItemId = currencyItemIdOf(factionId);
		if (currencyItemId == null) {
			return 0;
		}
		return entries.stream()
			.filter(entry -> currencyItemId.equals(entry.itemId()))
			.mapToInt(CurrencyEntry::count)
			.sum();
	}

	static int consumeInventoryCurrency(FactionId factionId, List<ItemStack> stacks, int amount) {
		if (stacks == null || amount <= 0 || factionId == null) {
			return amount;
		}
		var remaining = amount;
		var currencyItem = currencyItemOf(factionId);
		if (currencyItem == null) {
			return remaining;
		}
		for (var stack : stacks) {
			if (remaining <= 0) {
				break;
			}
			if (stack.isEmpty() || stack.getItem() != currencyItem) {
				continue;
			}
			var consumed = Math.min(stack.getCount(), remaining);
			stack.decrement(consumed);
			remaining -= consumed;
		}
		return remaining;
	}

	private static net.minecraft.item.Item currencyItemOf(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> Items.EMERALD;
			case NETHER -> Items.GOLD_INGOT;
			default -> null;
		};
	}

	private static String currencyItemIdOf(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> "minecraft:emerald";
			case NETHER -> "minecraft:gold_ingot";
			default -> null;
		};
	}

	private List<ItemStack> inventoryStacks(ServerPlayerEntity player) {
		if (player == null) {
			return List.of();
		}
		var inventory = player.getInventory();
		var stacks = new ArrayList<ItemStack>(inventory.size());
		for (int slot = 0; slot < inventory.size(); slot++) {
			stacks.add(inventory.getStack(slot));
		}
		return stacks;
	}

	private String currencyColor(FactionId factionId) {
		return factionId == FactionId.NETHER ? "&6" : "&a";
	}

	private String resolveTitle(FactionId factionId) {
		return switch (factionId) {
			case NETHER -> textConfig().tradeNetherTitle;
			case VILLAGER -> textConfig().tradeVillagerEnchantTitle;
			default -> textConfig().tradeVillagerTitle;
		};
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	enum PurchaseResult {
		SUCCESS,
		INSUFFICIENT_FUNDS,
		INVENTORY_FULL,
		INVALID_ENTRY,
		MAX_LEVEL,
		NO_ELIGIBLE_EQUIPMENT,
		UNSUPPORTED_FACTION
	}

	record CurrencyEntry(String itemId, int count) {
	}
}
