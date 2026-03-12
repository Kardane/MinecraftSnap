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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeGuiServiceTest {
	@Test
	void merchandiseSlotsExcludeReservedSlots() {
		assertEquals(24, TradeGuiService.merchandiseSlots().size());
		assertFalse(TradeGuiService.merchandiseSlots().contains(11));
		assertFalse(TradeGuiService.merchandiseSlots().contains(13));
		assertFalse(TradeGuiService.merchandiseSlots().contains(15));
		assertEquals(0, TradeGuiService.merchandiseSlots().getFirst());
	}

	@Test
	void purchaseConsumesEmeraldsAndUpdatesStats(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addEmeralds(playerId, "tester", 5);
		var state = new PlayerMatchState();
		state.setEmeralds(5);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 3, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.SUCCESS, result);
		assertEquals(2, state.getEmeralds());
		assertEquals(2, repository.getOrCreate(playerId, "tester").emeralds);
	}

	@Test
	void purchaseConsumesGoldIngotsAndUpdatesStats(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addGoldIngots(playerId, "tester", 4);
		var state = new PlayerMatchState();
		state.setGoldIngots(4);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.NETHER, state, playerId, "tester", 2, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.SUCCESS, result);
		assertEquals(2, state.getGoldIngots());
		assertEquals(2, repository.getOrCreate(playerId, "tester").goldIngots);
	}

	@Test
	void purchaseFailsWithoutEnoughCurrency(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addEmeralds(playerId, "tester", 1);
		var state = new PlayerMatchState();
		state.setEmeralds(1);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.VILLAGER, state, playerId, "tester", 2, () -> true);

		assertEquals(TradeGuiService.PurchaseResult.INSUFFICIENT_FUNDS, result);
		assertEquals(1, state.getEmeralds());
		assertEquals(1, repository.getOrCreate(playerId, "tester").emeralds);
	}

	@Test
	void purchaseFailsWhenRewardCannotBeInserted(@TempDir Path tempDir) {
		var repository = createRepository(tempDir);
		var playerId = UUID.randomUUID();
		repository.addGoldIngots(playerId, "tester", 5);
		var state = new PlayerMatchState();
		state.setGoldIngots(5);
		var service = createService(repository);

		var result = service.completePurchase(FactionId.NETHER, state, playerId, "tester", 3, () -> false);

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
