package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceServiceTest {
	@Test
	void matchingBiomeAndWeatherBuildsAdvanceExp() {
		var service = new AdvanceService(new UnitRegistry(false));
		var config = new SystemConfig();
		config.normalize();
		var state = new PlayerMatchState();
		state.setTeam(TeamId.RED, RoleType.UNIT);
		state.setFactionId(FactionId.MONSTER);
		state.setCurrentUnitId("zombie");

		for (int second = 0; second < 15; second++) {
			service.updateProgress(state, "minecraft:swamp", "rain", config.advance);
		}

		assertTrue(state.isAdvanceAvailable());
		assertEquals("zombie_veteran", state.getAdvanceTargetUnitId());
		assertEquals(15, state.getAdvanceExp());
	}

	@Test
	void mismatchResetsAdvanceExp() {
		var service = new AdvanceService(new UnitRegistry(false));
		var config = new SystemConfig();
		config.normalize();
		var state = new PlayerMatchState();
		state.setTeam(TeamId.RED, RoleType.UNIT);
		state.setFactionId(FactionId.MONSTER);
		state.setCurrentUnitId("skeleton");

		service.updateProgress(state, "minecraft:snowy_plains", "clear", config.advance);
		service.updateProgress(state, "minecraft:plains", "clear", config.advance);

		assertFalse(state.isAdvanceAvailable());
		assertEquals(0, state.getAdvanceExp());
		assertEquals(null, state.getAdvanceTargetUnitId());
	}

	@Test
	void forceAdvanceUsesConfiguredResult() {
		var service = new AdvanceService(new UnitRegistry(false));
		var config = new SystemConfig();
		config.normalize();
		var state = new PlayerMatchState();
		state.setTeam(TeamId.BLUE, RoleType.UNIT);
		state.setFactionId(FactionId.MONSTER);
		state.setCurrentUnitId("creeper");

		assertTrue(service.forceAdvance(state, config.advance));
		assertTrue(state.isAdvanceAvailable());
		assertEquals("charged_creeper", state.getAdvanceTargetUnitId());
		assertEquals(config.advance.conditions.stream()
			.filter(condition -> "creeper".equals(condition.unitId))
			.findFirst()
			.orElseThrow()
			.requiredExp, state.getAdvanceExp());
	}

	@Test
	void applyAdvanceReplacesCurrentUnitAndClearsProgress() {
		var registry = new UnitRegistry(false);
		registry.registerUnit(new UnitDefinition(
			"slime_brute",
			"강화 슬라임",
			FactionId.MONSTER,
			false,
			0,
			0,
			18.0,
			1.2,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			"",
			0,
			UnitDefinition.UnitAbilityType.NONE,
			UnitDefinition.UnitPassiveType.NONE,
			UnitDefinition.AmmoType.NONE,
			"slime",
			java.util.List.of()
		));
		var service = new AdvanceService(registry);
		var state = new PlayerMatchState();
		state.setTeam(TeamId.RED, RoleType.UNIT);
		state.setFactionId(FactionId.MONSTER);
		state.setCurrentUnitId("slime");
		state.setAdvanceAvailable(true);
		state.setAdvanceTargetUnitId("slime_brute");
		state.setAdvanceExp(12);

		var result = service.applyAdvance(state);

		assertEquals("slime_brute", result.id());
		assertEquals("slime_brute", state.getCurrentUnitId());
		assertFalse(state.isAdvanceAvailable());
		assertEquals(0, state.getAdvanceExp());
		assertEquals(null, state.getAdvanceTargetUnitId());
	}
}
