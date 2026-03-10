package karn.minecraftsnap.game;

import karn.minecraftsnap.config.MinecraftSnapConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UnitRegistryTest {
	@Test
	void loadsUnitsFromFactionConfigs(@TempDir Path tempDir) {
		var manager = new MinecraftSnapConfigManager(tempDir, LoggerFactory.getLogger("test"));
		manager.load();
		var registry = new UnitRegistry(false);
		registry.loadFromFactionConfigs(manager.getFactionConfigs(), ignored -> null);

		assertEquals(16, registry.all().size());
		assertEquals(4, registry.byFaction(FactionId.VILLAGER).size());
		assertEquals(8, registry.allByFaction(FactionId.MONSTER).size());
		assertEquals(4, registry.byFaction(FactionId.NETHER).size());
		assertFalse(registry.get("villager").descriptionLines().isEmpty());
	}
}
