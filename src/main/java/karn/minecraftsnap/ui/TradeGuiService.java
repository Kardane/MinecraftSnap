package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.ShopConfigFile;
import karn.minecraftsnap.config.ShopEntry;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class TradeGuiService {
	private static final List<Integer> MERCHANDISE_SLOTS = java.util.stream.IntStream.range(0, 27)
		.filter(slot -> slot != 11 && slot != 13 && slot != 15)
		.boxed()
		.toList();

	private final TextTemplateResolver textTemplateResolver;
	private final UnitLoadoutService unitLoadoutService;
	private final Function<FactionId, ShopConfigFile> shopConfigProvider;
	private final Supplier<StatsRepository> statsRepositorySupplier;

	public TradeGuiService(
		TextTemplateResolver textTemplateResolver,
		UnitLoadoutService unitLoadoutService,
		Function<FactionId, ShopConfigFile> shopConfigProvider,
		Supplier<StatsRepository> statsRepositorySupplier
	) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitLoadoutService = unitLoadoutService;
		this.shopConfigProvider = shopConfigProvider;
		this.statsRepositorySupplier = statsRepositorySupplier;
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state) {
		openInternal(player, state, state.getFactionId() == FactionId.NETHER ? FactionId.NETHER : FactionId.VILLAGER);
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		openInternal(player, state, factionId == FactionId.NETHER ? FactionId.NETHER : FactionId.VILLAGER);
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

	PurchaseResult completePurchase(
		FactionId factionId,
		PlayerMatchState state,
		UUID playerId,
		String playerName,
		int price,
		BooleanSupplier rewardGrantAction
	) {
		if (balanceOf(factionId, state) < price) {
			return PurchaseResult.INSUFFICIENT_FUNDS;
		}
		if (!rewardGrantAction.getAsBoolean()) {
			return PurchaseResult.INVENTORY_FULL;
		}
		switch (factionId) {
			case VILLAGER -> state.addEmeralds(-price);
			case NETHER -> state.addGoldIngots(-price);
			default -> {
				return PurchaseResult.UNSUPPORTED_FACTION;
			}
		}
		var repository = statsRepositorySupplier == null ? null : statsRepositorySupplier.get();
		if (repository != null) {
			switch (factionId) {
				case VILLAGER -> repository.addEmeralds(playerId, playerName, -price);
				case NETHER -> repository.addGoldIngots(playerId, playerName, -price);
				default -> {
				}
			}
		}
		return PurchaseResult.SUCCESS;
	}

	private void openInternal(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(factionId == FactionId.NETHER ? "&6네더 상점" : "&a주민 상점"));
		renderStaticSlots(gui, state, factionId);
		renderMerchandise(gui, player, state, factionId);
		gui.open();
	}

	private void renderStaticSlots(SimpleGui gui, PlayerMatchState state, FactionId factionId) {
		gui.setSlot(11, new GuiElementBuilder(factionId == FactionId.NETHER ? Items.GOLD_INGOT : Items.EMERALD)
			.setName(textTemplateResolver.format(factionId == FactionId.NETHER ? "&6금괴 잔액" : "&a에메랄드 잔액"))
			.setLore(lines("&f보유량: " + currencyColor(factionId) + balanceOf(factionId, state)))
			.build());
		gui.setSlot(13, new GuiElementBuilder(factionId == FactionId.NETHER ? Items.BARREL : Items.CHEST)
			.setName(textTemplateResolver.format("&e소모품 상점"))
			.setLore(lines(
				"&7아이템 클릭으로 즉시 구매",
				"&7디스플레이 아이템은 사라지지 않음"
			))
			.build());
		gui.setSlot(15, new GuiElementBuilder(Items.PAPER)
			.setName(textTemplateResolver.format("&f구매 안내"))
			.setLore(lines(
				"&7재화 부족 시 구매 실패",
				"&7인벤토리가 가득 차면 지급 안 됨"
			))
			.build());
	}

	private void renderMerchandise(SimpleGui gui, ServerPlayerEntity player, PlayerMatchState state, FactionId factionId) {
		var config = shopConfigProvider == null ? null : shopConfigProvider.apply(factionId);
		var entries = config == null ? List.<ShopEntry>of() : config.entries;
		for (int index = 0; index < Math.min(entries.size(), MERCHANDISE_SLOTS.size()); index++) {
			var entry = entries.get(index);
			var displayStack = buildDisplayStack(player, entry);
			if (displayStack.isEmpty()) {
				continue;
			}
			mergeShopLore(displayStack, factionId, entry.price);
			var builder = new GuiElementBuilder(displayStack)
				.setCallback((slotIndex, clickType, action, slotGui) -> {
					var result = purchase(player, state, factionId, entry);
					switch (result) {
						case SUCCESS -> player.sendMessage(textTemplateResolver.format("&a구매 완료"), false);
						case INSUFFICIENT_FUNDS -> player.sendMessage(textTemplateResolver.format("&c재화가 부족함"), false);
						case INVENTORY_FULL -> player.sendMessage(textTemplateResolver.format("&c인벤토리 공간 부족"), false);
						case INVALID_ENTRY -> player.sendMessage(textTemplateResolver.format("&c상점 품목 설정 오류"), false);
						case UNSUPPORTED_FACTION -> player.sendMessage(textTemplateResolver.format("&c이 팩션은 상점을 사용할 수 없음"), false);
					}
					renderStaticSlots(gui, state, factionId);
				});
			gui.setSlot(MERCHANDISE_SLOTS.get(index), builder.build());
		}
	}

	private void mergeShopLore(ItemStack displayStack, FactionId factionId, int price) {
		var mergedLore = new ArrayList<net.minecraft.text.Text>();
		var existingLore = displayStack.get(net.minecraft.component.DataComponentTypes.LORE);
		if (existingLore != null) {
			mergedLore.addAll(existingLore.lines());
		}
		mergedLore.add(textTemplateResolver.format("&f가격: " + currencyColor(factionId) + price));
		mergedLore.add(textTemplateResolver.format("&7클릭해서 구매"));
		displayStack.set(net.minecraft.component.DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(mergedLore));
	}

	private PurchaseResult purchase(ServerPlayerEntity player, PlayerMatchState state, FactionId factionId, ShopEntry entry) {
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
		return completePurchase(
			factionId,
			state,
			player.getUuid(),
			player.getName().getString(),
			entry.price,
			() -> tryInsertReward(player, reward)
		);
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

	private int balanceOf(FactionId factionId, PlayerMatchState state) {
		return switch (factionId) {
			case VILLAGER -> state.getEmeralds();
			case NETHER -> state.getGoldIngots();
			default -> 0;
		};
	}

	private String currencyColor(FactionId factionId) {
		return factionId == FactionId.NETHER ? "&6" : "&a";
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}

	enum PurchaseResult {
		SUCCESS,
		INSUFFICIENT_FUNDS,
		INVENTORY_FULL,
		INVALID_ENTRY,
		UNSUPPORTED_FACTION
	}
}
