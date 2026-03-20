package karn.minecraftsnap.game;

import karn.minecraftsnap.config.AdvanceOptionEntry;
import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceServiceTest {
	@Test
	void matchingOptionBuildsTicksAndBecomesReady() {
		var service = new AdvanceService(monsterRegistry(option("husk", List.of("minecraft:desert"), List.of("clear"), 2)));
		var state = monsterState("zombie");

		service.updateProgress(state, "minecraft:desert", "clear");
		service.updateProgress(state, "minecraft:desert", "clear");

		assertEquals(2, state.getAdvanceOptionTicks("husk"));
		assertTrue(state.isAdvanceReady("husk", 2));
	}

	@Test
	void mismatchedOptionResetsOnlyThatOptionProgress() {
		var service = new AdvanceService(monsterRegistry(
			option("husk", List.of("minecraft:desert"), List.of("clear"), 2),
			option("zombie_alt", List.of("minecraft:plains"), List.of("clear"), 3)
		));
		var state = monsterState("zombie");

		service.updateProgress(state, "minecraft:desert", "clear");
		assertEquals(1, state.getAdvanceOptionTicks("husk"));
		assertEquals(0, state.getAdvanceOptionTicks("zombie_alt"));

		service.updateProgress(state, "minecraft:plains", "clear");
		assertEquals(0, state.getAdvanceOptionTicks("husk"));
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
		var service = new AdvanceService(monsterRegistry(option("giant_slime", List.of("minecraft:swamp"), List.of("rain"), 2)));
		var state = monsterState("zombie");
		state.setAdvanceOptionTicks("giant_slime", 2);

		var result = service.applyAdvance(state, "giant_slime");

		assertEquals("giant_slime", result.id());
		assertEquals("giant_slime", state.getCurrentUnitId());
		assertEquals(0, state.getAdvanceOptionTicks("giant_slime"));
	}

	private UnitRegistry monsterRegistry(AdvanceOptionEntry... options) {
		var registry = new UnitRegistry(false);
		registry.registerUnit(new UnitDefinition(
			"zombie",
			"좀비",
			FactionId.MONSTER,
			true,
			0,
			20.0,
			1.0,
			UnitItemEntry.create("minecraft:iron_shovel"),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			"",
			0,
			UnitDefinition.AmmoType.NONE,
			EntitySpecEntry.create("minecraft:zombie"),
			List.of(),
			List.of(options)
		));
		registry.registerUnit(targetUnit("husk", "허스크", "minecraft:iron_sword", "minecraft:husk"));
		registry.registerUnit(targetUnit("drowned", "드라운드", "minecraft:trident", "minecraft:drowned"));
		registry.registerUnit(targetUnit("zombie_alt", "대체 전직", "minecraft:stone_sword", "minecraft:zombie"));
		registry.registerUnit(targetUnit("charged_creeper", "대전된 크리퍼", "minecraft:tnt", "minecraft:creeper"));
		registry.registerUnit(targetUnit("creeper_alt", "크리퍼 대체형", "minecraft:tnt", "minecraft:creeper"));
		registry.registerUnit(targetUnit("giant_slime", "거대 슬라임", "minecraft:slime_ball", "minecraft:slime"));
		return registry;
	}

	private UnitDefinition targetUnit(String id, String displayName, String itemId, String disguiseId) {
		return new UnitDefinition(
			id,
			displayName,
			FactionId.MONSTER,
			false,
			0,
			20.0,
			1.0,
			UnitItemEntry.create(itemId),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			new UnitItemEntry(),
			"",
			0,
			UnitDefinition.AmmoType.NONE,
			EntitySpecEntry.create(disguiseId),
			List.of(),
			List.of()
		);
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
