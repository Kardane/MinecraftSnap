package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import karn.minecraftsnap.config.SystemConfig;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnitSpawnServiceTest {
	@Test
	void preferredUnitPlayerSelectedFirst() {
		var service = new UnitSpawnService();
		var preferred = new UnitSpawnService.SpawnCandidate(UUID.randomUUID(), "villager", true);
		var fallback = new UnitSpawnService.SpawnCandidate(UUID.randomUUID(), null, true);

		var result = service.selectSpawnCandidate("villager", List.of(fallback, preferred));

		assertEquals(preferred.playerId(), result.playerId());
	}

	@Test
	void fallsBackToFirstSpectatorWhenNoPreferenceMatch() {
		var service = new UnitSpawnService();
		var first = new UnitSpawnService.SpawnCandidate(UUID.randomUUID(), null, true);
		var second = new UnitSpawnService.SpawnCandidate(UUID.randomUUID(), "zombie", true);

		var result = service.selectSpawnCandidate("villager", List.of(first, second));

		assertEquals(first.playerId(), result.playerId());
	}

	@Test
	void returnsNullWhenNoSpectatorCandidateExists() {
		var service = new UnitSpawnService();

		var result = service.selectSpawnCandidate("villager", List.of(
			new UnitSpawnService.SpawnCandidate(UUID.randomUUID(), "villager", false)
		));

		assertNull(result);
	}

	@Test
	void invalidUnitSpawnFallsBackToCaptainSpawn() {
		var config = new SystemConfig();
		config.gameStart.redLane1UnitSpawn.y = -59.0;

		assertEquals(config.gameStart.redCaptainSpawn, UnitSpawnService.safeUnitSpawn(config, TeamId.RED, LaneId.LANE_1));
		assertEquals(config.gameStart.blueLane1UnitSpawn, UnitSpawnService.safeUnitSpawn(config, TeamId.BLUE, LaneId.LANE_1));
	}

	@Test
	void nearestLaneUsesCaptainPosition() {
		var config = new SystemConfig();
		config.inGame.lane1Region = SystemConfig.LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		config.inGame.lane2Region = SystemConfig.LaneRegionConfig.create(20.0, 0.0, -8.0, 30.0, 320.0, 8.0);
		config.inGame.lane3Region = SystemConfig.LaneRegionConfig.create(40.0, 0.0, -8.0, 50.0, 320.0, 8.0);

		assertEquals(LaneId.LANE_2, UnitSpawnService.nearestLaneForPosition(23.0, 0.0, config));
	}
}
