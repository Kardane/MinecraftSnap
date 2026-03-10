package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InGameRuleServiceTest {
	@TempDir
	Path tempDir;

	@Test
	void blocksPlayerDamageOutsideGameRunning() {
		var manager = new MatchManager();
		var service = createService(manager);
		var victim = new PlayerMatchState();
		victim.setTeam(TeamId.RED, RoleType.UNIT);
		var attacker = new PlayerMatchState();
		attacker.setTeam(TeamId.BLUE, RoleType.UNIT);

		assertFalse(service.isDamageAllowed(victim, attacker, MatchPhase.LOBBY));
	}

	@Test
	void blocksFriendlyFireInGameRunning() {
		var manager = new MatchManager();
		var service = createService(manager);
		var victim = new PlayerMatchState();
		victim.setTeam(TeamId.RED, RoleType.UNIT);
		var attacker = new PlayerMatchState();
		attacker.setTeam(TeamId.RED, RoleType.CAPTAIN);

		assertFalse(service.isDamageAllowed(victim, attacker, MatchPhase.GAME_RUNNING));
	}

	@Test
	void allowsEnemyDamageInGameRunning() {
		var manager = new MatchManager();
		var service = createService(manager);
		var victim = new PlayerMatchState();
		victim.setTeam(TeamId.RED, RoleType.UNIT);
		var attacker = new PlayerMatchState();
		attacker.setTeam(TeamId.BLUE, RoleType.CAPTAIN);

		assertTrue(service.isDamageAllowed(victim, attacker, MatchPhase.GAME_RUNNING));
	}

	@Test
	void blocksClosedLaneForUnitOnly() {
		var manager = new MatchManager();
		var service = createService(manager);
		var unit = new PlayerMatchState();
		unit.setTeam(TeamId.RED, RoleType.UNIT);
		var captain = new PlayerMatchState();
		captain.setTeam(TeamId.RED, RoleType.CAPTAIN);

		assertTrue(service.shouldBlockClosedLane(unit, LaneId.LANE_2));
		assertFalse(service.shouldBlockClosedLane(captain, LaneId.LANE_2));

		manager.revealLane(LaneId.LANE_2);
		assertFalse(service.shouldBlockClosedLane(unit, LaneId.LANE_2));
	}

	@Test
	void recordsKillAndDeathAndMarksPendingSpectator() {
		var manager = new MatchManager();
		var service = createService(manager);
		var victimId = UUID.randomUUID();
		var killerId = UUID.randomUUID();
		manager.setRole(victimId, TeamId.RED, RoleType.UNIT);
		manager.setRole(killerId, TeamId.BLUE, RoleType.UNIT);

		service.recordKillAndDeath(victimId, "victim", killerId, "killer");

		var repository = service.getStatsRepository();
		assertEquals(1, repository.getOrCreate(victimId, "victim").deaths);
		assertEquals(1, repository.getOrCreate(killerId, "killer").kills);
		assertEquals(303, repository.getLadder(killerId, "killer"));
		assertTrue(service.isPendingSpectator(victimId));
	}

	@Test
	void findsContainingLaneByConfiguredRegion() {
		var manager = new MatchManager();
		var service = createService(manager);
		var config = new SystemConfig();

		assertEquals(LaneId.LANE_1, service.findContainingLane("minecraft:overworld", 0.0, 64.0, 0.0, config.inGame));
		assertEquals(LaneId.LANE_2, service.findContainingLane("minecraft:overworld", 20.0, 64.0, 0.0, config.inGame));
		assertNull(service.findContainingLane("minecraft:the_nether", 0.0, 64.0, 0.0, config.inGame));
	}

	private InGameRuleService createService(MatchManager manager) {
		var repository = new StatsRepository(tempDir.resolve("stats.json"), LoggerFactory.getLogger("test"));
		repository.load();
		return new InGameRuleService(manager, repository, new TextTemplateResolver());
	}
}
