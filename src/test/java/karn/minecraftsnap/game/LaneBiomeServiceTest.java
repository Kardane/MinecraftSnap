package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaneBiomeServiceTest {
	@Test
	void restoreAllReplaysStoredBiomeCommands() {
		var commands = new ArrayList<String>();
		var service = new LaneBiomeService((server, command) -> {
			commands.add(command);
			return true;
		});
		service.rememberSnapshot(LaneId.LANE_1, List.of(
			new LaneBiomeService.BiomeCellSnapshot("minecraft:overworld", 0, 64, 0, 3, 67, 3, "minecraft:plains"),
			new LaneBiomeService.BiomeCellSnapshot("minecraft:overworld", 4, 64, 0, 7, 67, 3, "minecraft:forest")
		));

		service.restoreAll(null);

		assertEquals(2, commands.size());
		assertTrue(commands.get(0).contains("fillbiome 0 64 0 3 67 3 minecraft:plains"));
		assertTrue(service.getSnapshot(LaneId.LANE_1).isEmpty());
	}
}
