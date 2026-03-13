package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.ShopConfigFile;
import karn.minecraftsnap.config.ShopEntry;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.UnitLoadoutService;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeGuiServiceTest {
	@Test
	void merchandiseSlotsExcludeReservedSlots() {
		assertEquals(27, TradeGuiService.merchandiseSlots().size());
		assertTrue(TradeGuiService.merchandiseSlots().contains(11));
		assertEquals(0, TradeGuiService.merchandiseSlots().getFirst());
	}

	@Test
	void countInventoryCurrencyIgnoresNbtAndComponents() {
		var total = TradeGuiService.countCurrencyEntries(FactionId.VILLAGER, List.of(
			new TradeGuiService.CurrencyEntry("minecraft:emerald", 2),
			new TradeGuiService.CurrencyEntry("minecraft:emerald", 3),
			new TradeGuiService.CurrencyEntry("minecraft:diamond", 10)
		));

		assertEquals(5, total);
	}

	@Test
	void countInventoryCurrencyUsesOnlyMatchingItemId() {
		var total = TradeGuiService.countCurrencyEntries(FactionId.NETHER, List.of(
			new TradeGuiService.CurrencyEntry("minecraft:gold_ingot", 2),
			new TradeGuiService.CurrencyEntry("minecraft:gold_ingot", 2),
			new TradeGuiService.CurrencyEntry("minecraft:gold_nugget", 99)
		));

		assertEquals(4, total);
	}

	@Test
	void purchaseConsumesInventoryEmeraldsOnly(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addEmeralds(playerId, "tester", 5);
		var state = new PlayerMatchState();
		state.setEmeralds(5);
		var spent = new int[1];
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 3, 3, () -> spent[0] = 3, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.SUCCESS, result);
		assertEquals(5, state.getEmeralds());
		assertEquals(5, repository.getOrCreate(playerId, "tester").emeralds);
		assertEquals(3, spent[0]);
	}

	@Test
	void purchaseConsumesInventoryGoldIngotsOnly(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addGoldIngots(playerId, "tester", 4);
		var state = new PlayerMatchState();
		state.setGoldIngots(4);
		var spent = new int[1];
		var service = createService(repository);

		var result = service.completePurchase(FactionId.NETHER, state, playerId, "tester", 2, 2, () -> spent[0] = 2, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.SUCCESS, result);
		assertEquals(4, state.getGoldIngots());
		assertEquals(4, repository.getOrCreate(playerId, "tester").goldIngots);
		assertEquals(2, spent[0]);
	}

	@Test
	void purchaseFailsWithoutInventoryCurrencyEvenIfStateBalanceExists(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addEmeralds(playerId, "tester", 10);
		var state = new PlayerMatchState();
		state.setEmeralds(10);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 2, 0, () -> {
		}, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.INSUFFICIENT_FUNDS, result);
		assertEquals(10, state.getEmeralds());
		assertEquals(10, repository.getOrCreate(playerId, "tester").emeralds);
	}

	@Test
	void purchaseFailsWhenRewardCannotBeInserted(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addGoldIngots(playerId, "tester", 5);
		var state = new PlayerMatchState();
		state.setGoldIngots(5);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.NETHER, state, playerId, "tester", 3, 3, () -> {
		}, () -> false);

		assertEquals(TradeGuiService.PurchaseResult.INVENTORY_FULL, result);
		assertEquals(5, state.getGoldIngots());
		assertEquals(5, repository.getOrCreate(playerId, "tester").goldIngots);
	}

	@Test
	void shopConfigNormalizesEntries() {
		var config = new ShopConfigFile();
		var entry = new ShopEntry();
		entry.id = "bread";
		entry.price = -3;
		entry.item = UnitItemEntry.create("minecraft:bread");
		config.entries.add(entry);

		config.normalize();

		assertEquals(1, config.entries.size());
		assertEquals(1, config.entries.getFirst().price);
		assertTrue(config.entries.getFirst().item.count >= 1);
	}

	@Test
	void purchaseCanUseInventoryCurrencyWithoutTouchingStateBalance(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		var state = new PlayerMatchState();
		var spent = new int[1];
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 3, 3, () -> spent[0] = 3, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.SUCCESS, result);
		assertEquals(0, state.getEmeralds());
		assertEquals(3, spent[0]);
		assertEquals(0, repository.getOrCreate(playerId, "tester").emeralds);
	}

	@Test
	void purchaseFailsWhenInventoryCurrencyIsShortEvenIfPartiallyAvailable(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addEmeralds(playerId, "tester", 99);
		var state = new PlayerMatchState();
		state.setEmeralds(99);
		var spent = new int[1];
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 3, 2, () -> spent[0] = 2, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.INSUFFICIENT_FUNDS, result);
		assertEquals(99, state.getEmeralds());
		assertEquals(99, repository.getOrCreate(playerId, "tester").emeralds);
		assertEquals(0, spent[0]);
	}

	private TradeGuiService createService(StatsRepository repository) {
		return new TradeGuiService(
			new TextTemplateResolver(),
			new UnitLoadoutService(),
			factionId -> new ShopConfigFile(),
			() -> repository
		);
	}

	private StatsRepository createRepository(Path tempDir) {
		var repository = new StatsRepository(tempDir.resolve("stats.json"), LoggerFactory.getLogger("test"));
		repository.load();
		return repository;
	}
}
