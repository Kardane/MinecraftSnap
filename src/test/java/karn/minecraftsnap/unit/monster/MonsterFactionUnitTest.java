package karn.minecraftsnap.unit.monster;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterFactionUnitTest {
	@Test
	void zombieMatchesDocumentSpec() {
		var unit = new ZombieUnit();
		var definition = unit.definition();

		assertEquals("좀비", definition.displayName());
		assertEquals(1, definition.cost());
		assertEquals(7, definition.spawnCooldownSeconds());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(0.8, definition.moveSpeedScale());
		assertEquals("minecraft:iron_shovel", definition.mainHand().itemId);
		assertEquals("minecraft:leather_helmet", definition.helmet().itemId);
		assertEquals(1, unit.captainManaRestoreOnDeath());
		assertEquals(Set.of("husk", "drowned"), definition.advanceOptions().stream().map(option -> option.resultUnitId).collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void huskMatchesDocumentSpec() {
		var unit = new HuskUnit();
		var definition = unit.definition();

		assertEquals("허스크", definition.displayName());
		assertEquals(3, definition.cost());
		assertEquals(10, definition.spawnCooldownSeconds());
		assertEquals(20.0, definition.maxHealth());
		assertEquals(0.9, definition.moveSpeedScale());
		assertEquals("minecraft:iron_sword", definition.mainHand().itemId);
		assertEquals(1, unit.captainManaRestoreOnDeath());
		assertEquals(60, unit.statusDurationTicks());
	}

	@Test
	void drownedMatchesDocumentSpec() {
		var unit = new DrownedUnit();
		var definition = unit.definition();

		assertEquals("드라운드", definition.displayName());
		assertEquals(3, definition.cost());
		assertEquals(10, definition.spawnCooldownSeconds());
		assertEquals(18.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:trident", definition.mainHand().itemId);
		assertEquals(1, unit.captainManaRestoreOnDeath());
	}

	@Test
	void slimeMatchesDocumentSpec() {
		var unit = new SlimeUnit();
		var definition = unit.definition();

		assertEquals("슬라임", definition.displayName());
		assertEquals(2, definition.cost());
		assertEquals(8, definition.spawnCooldownSeconds());
		assertEquals(18.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals("minecraft:slime_ball", definition.mainHand().itemId);
		assertEquals(3.0D, unit.weaponAttackDamage());
		assertEquals(2.0D, unit.weaponAttackSpeed());
		assertEquals(0.8D, unit.jumpStrengthValue());
		assertEquals(4, unit.spawnedSlimeCount());
		assertEquals(2, unit.spawnedSlimeSize());
		assertEquals(300, definition.advanceOptions().getFirst().requiredTicks);
	}

	@Test
	void giantSlimeMatchesDocumentSpec() {
		var unit = new GiantSlimeUnit();
		var definition = unit.definition();

		assertEquals("거대 슬라임", definition.displayName());
		assertEquals(30.0, definition.maxHealth());
		assertEquals(1.0, definition.moveSpeedScale());
		assertEquals(3, unit.spawnedSlimeCount());
		assertEquals(5, unit.spawnedSlimeSize());
		assertTrue(definition.disguise().entityNbt.contains("Size:5"));
	}

	@Test
	void skeletonMatchesDocumentSpec() {
		var unit = new SkeletonUnit();
		var definition = unit.definition();

		assertEquals("스켈레톤", definition.displayName());
		assertEquals(3, definition.cost());
		assertEquals(12, definition.spawnCooldownSeconds());
		assertEquals(16.0, definition.maxHealth());
		assertEquals(0.9, definition.moveSpeedScale());
		assertEquals("minecraft:bow", definition.mainHand().itemId);
		assertEquals("minecraft:bone", definition.abilityItemSpec().itemId);
		assertEquals(12, definition.abilityCooldownSeconds());
		assertEquals(4.0D, unit.boneBlastRadius());
		assertEquals(5.0f, unit.boneBlastDamage());
		assertEquals(Set.of("stray", "bogged", "wither_skeleton"), definition.advanceOptions().stream().map(option -> option.resultUnitId).collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void strayMatchesDocumentSpec() {
		var unit = new StrayUnit();
		var definition = unit.definition();

		assertEquals("스트레이", definition.displayName());
		assertEquals(4, definition.cost());
		assertEquals(15, definition.spawnCooldownSeconds());
		assertEquals(16.0, definition.maxHealth());
		assertEquals(0.8, definition.moveSpeedScale());
		assertEquals("minecraft:bow", definition.mainHand().itemId);
		assertEquals(40, unit.statusDurationTicks());
		assertEquals(1, unit.effectAmplifier());
	}

	@Test
	void boggedMatchesDocumentSpec() {
		var unit = new BoggedUnit();
		var definition = unit.definition();

		assertEquals("보그드", definition.displayName());
		assertEquals(4, definition.cost());
		assertEquals(15, definition.spawnCooldownSeconds());
		assertEquals(16.0, definition.maxHealth());
		assertEquals(0.8, definition.moveSpeedScale());
		assertEquals("minecraft:bow", definition.mainHand().itemId);
		assertEquals(100, unit.statusDurationTicks());
		assertEquals(1, unit.effectAmplifier());
	}

	@Test
	void witherSkeletonMatchesDocumentSpec() {
		var unit = new WitherSkeletonUnit();
		var definition = unit.definition();

		assertEquals("위더 스켈레톤", definition.displayName());
		assertEquals(4, definition.cost());
		assertEquals(25, definition.spawnCooldownSeconds());
		assertEquals(24.0, definition.maxHealth());
		assertEquals(1.1, definition.moveSpeedScale());
		assertEquals("minecraft:stone_sword", definition.mainHand().itemId);
		assertEquals("minecraft:stone_sword", definition.abilityItemSpec().itemId);
		assertEquals(8, definition.abilityCooldownSeconds());
		assertEquals(60, unit.statusDurationTicks());
		assertEquals(1, unit.effectAmplifier());
		assertEquals(40, unit.fireResistanceDurationTicks());
	}
}
