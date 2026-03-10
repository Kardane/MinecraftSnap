package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

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
}
