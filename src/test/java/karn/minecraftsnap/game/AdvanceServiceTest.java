package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.FactionConfigFile;
import karn.minecraftsnap.config.FactionUnitEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceServiceTest {
	@Test
	void matchingOptionBuildsTicksAndBecomesReady() {
		var service = new AdvanceService(monsterRegistry(option("zombie_veteran", List.of("minecraft:swamp"), List.of("rain"), 2)));
		var state = monsterState("zombie");

		service.updateProgress(state, "minecraft:swamp", "rain");
		service.updateProgress(state, "minecraft:swamp", "rain");

		assertEquals(2, state.getAdvanceOptionTicks("zombie_veteran"));
		assertTrue(state.isAdvanceReady("zombie_veteran", 2));
	}

	@Test
	void mismatchedOptionResetsOnlyThatOptionProgress() {
		var service = new AdvanceService(monsterRegistry(
			option("zombie_veteran", List.of("minecraft:swamp"), List.of("rain"), 2),
			option("zombie_alt", List.of("minecraft:plains"), List.of("clear"), 3)
		));
		var state = monsterState("zombie");

		service.updateProgress(state, "minecraft:swamp", "rain");
		assertEquals(1, state.getAdvanceOptionTicks("zombie_veteran"));
		assertEquals(0, state.getAdvanceOptionTicks("zombie_alt"));

		service.updateProgress(state, "minecraft:plains", "clear");
		assertEquals(0, state.getAdvanceOptionTicks("zombie_veteran"));
		assertEquals(1, state.getAdvanceOptionTicks("zombie_alt"));
	}

	@Test
	void forceAdvancePrimesFirstOptionOnly() {
		var service = new AdvanceService(monsterRegistry(
			option("charged_creeper", List.of("minecraft:plains"), List.of("thunder"), 10),
			option("creeper_alt", List.of("minecraft:forest"), List.of("rain"), 5)
		));
		var state = monsterState("zombie");

		assertTrue(service.forceAdvance(state));
		assertTrue(state.isAdvanceReady("charged_creeper", 10));
		assertEquals(0, state.getAdvanceOptionTicks("creeper_alt"));
	}

	@Test
	void applyAdvanceReplacesCurrentUnitAndClearsOptionTicks() {
		var service = new AdvanceService(monsterRegistry(option("slime_brute", List.of("minecraft:swamp"), List.of("rain"), 2)));
		var state = monsterState("zombie");
		state.setAdvanceOptionTicks("slime_brute", 2);

		var result = service.applyAdvance(state, "slime_brute");

		assertEquals("slime_brute", result.id());
		assertEquals("slime_brute", state.getCurrentUnitId());
		assertEquals(0, state.getAdvanceOptionTicks("slime_brute"));
	}

	private UnitRegistry monsterRegistry(AdvanceOptionEntry... options) {
		var zombie = new FactionUnitEntry();
		zombie.id = "zombie";
		zombie.displayName = "좀비";
		zombie.mainHand = UnitItemEntry.create("minecraft:iron_shovel");
		zombie.disguise = EntitySpecEntry.create("minecraft:zombie");
		zombie.advanceOptions = new java.util.ArrayList<>(List.of(options));
		zombie.normalize();

		var veteran = targetUnit("zombie_veteran", "강화 좀비", "minecraft:iron_shovel", "minecraft:husk");
		var alt = targetUnit("zombie_alt", "대체 전직", "minecraft:stone_sword", "minecraft:zombie");
		var charged = targetUnit("charged_creeper", "대전된 크리퍼", "minecraft:tnt", "minecraft:creeper");
		var creeperAlt = targetUnit("creeper_alt", "크리퍼 대체형", "minecraft:tnt", "minecraft:creeper");
		var slimeBrute = targetUnit("slime_brute", "강화 슬라임", "minecraft:slime_ball", "minecraft:slime");

		var config = new FactionConfigFile();
		config.units = List.of(zombie, veteran, alt, charged, creeperAlt, slimeBrute);
		config.normalize();

		var registry = new UnitRegistry(false);
		registry.loadFromFactionConfigs(Map.of(FactionId.MONSTER, config));
		return registry;
	}

	private FactionUnitEntry targetUnit(String id, String displayName, String itemId, String disguiseId) {
		var unit = new FactionUnitEntry();
		unit.id = id;
		unit.displayName = displayName;
		unit.captainSpawnable = false;
		unit.mainHand = UnitItemEntry.create(itemId);
		unit.disguise = EntitySpecEntry.create(disguiseId);
		unit.normalize();
		return unit;
	}

	private AdvanceOptionEntry option(String resultUnitId, List<String> biomes, List<String> weathers, int requiredTicks) {
		var option = new AdvanceOptionEntry();
		option.resultUnitId = resultUnitId;
		option.displayName = resultUnitId;
		option.descriptionLines = List.of("&7테스트 전직");
		option.biomes = biomes;
		option.weathers = weathers;
		option.requiredTicks = requiredTicks;
		option.normalize();
		return option;
	}

	private PlayerMatchState monsterState(String unitId) {
		var state = new PlayerMatchState();
		state.setTeam(TeamId.RED, RoleType.UNIT);
		state.setFactionId(FactionId.MONSTER);
		state.setCurrentUnitId(unitId);
		return state;
	}
}
