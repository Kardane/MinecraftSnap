package karn.minecraftsnap.game;

import karn.minecraftsnap.unit.UnitClass;
import karn.minecraftsnap.unit.UnitContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitHookServiceTest {
	@Test
	void dispatchesAllConfiguredHooks() {
		var service = new UnitHookService(null, null, null, null, () -> null, null, null, null, null, null, null, null, null, null);
		var unitClass = new RecordingUnitClass();
		var context = new UnitContext(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

		service.dispatchOnTick(unitClass, context);
		service.dispatchOnSkillUse(unitClass, context);
		service.dispatchOnLaneEnter(unitClass, context, null, LaneId.LANE_1);
		service.dispatchOnCaptureTick(unitClass, context);
		service.dispatchOnCaptureScore(unitClass, context);
		service.dispatchOnDamaged(unitClass, context, null, 3.0f);
		service.dispatchOnDeath(unitClass, context, null);
		service.dispatchOnKill(unitClass, context, null);
		service.dispatchOnAttack(unitClass, context, null, 4.0f);

		assertEquals(1, unitClass.tickCalls);
		assertEquals(1, unitClass.skillCalls);
		assertEquals(1, unitClass.laneEnterCalls);
		assertEquals(1, unitClass.captureTickCalls);
		assertEquals(1, unitClass.captureScoreCalls);
		assertEquals(1, unitClass.damagedCalls);
		assertEquals(1, unitClass.deathCalls);
		assertEquals(1, unitClass.killCalls);
		assertEquals(1, unitClass.attackCalls);
	}

	@Test
	void unitActionsAllowedOutsideGameWhenAssigned() {
		assertTrue(UnitHookService.canUseUnitActions(RoleType.UNIT, "villager", false));
		assertFalse(UnitHookService.canUseUnitActions(RoleType.CAPTAIN, "villager", false));
		assertFalse(UnitHookService.canUseUnitActions(RoleType.UNIT, null, false));
		assertFalse(UnitHookService.canUseUnitActions(RoleType.UNIT, "villager", true));
	}

	private static final class RecordingUnitClass implements UnitClass {
		int tickCalls;
		int attackCalls;
		int damagedCalls;
		int deathCalls;
		int killCalls;
		int skillCalls;
		int laneEnterCalls;
		int captureTickCalls;
		int captureScoreCalls;

		@Override
		public void onTick(UnitContext context) {
			tickCalls++;
		}

		@Override
		public void onAttack(UnitContext context, net.minecraft.server.network.ServerPlayerEntity victim, float amount) {
			attackCalls++;
		}

		@Override
		public void onDamaged(UnitContext context, net.minecraft.entity.damage.DamageSource source, float amount) {
			damagedCalls++;
		}

		@Override
		public void onDeath(UnitContext context, net.minecraft.entity.damage.DamageSource source) {
			deathCalls++;
		}

		@Override
		public void onKill(UnitContext context, net.minecraft.server.network.ServerPlayerEntity victim) {
			killCalls++;
		}

		@Override
		public void onSkillUse(UnitContext context) {
			skillCalls++;
		}

		@Override
		public void onLaneEnter(UnitContext context, LaneId previousLaneId, LaneId currentLaneId) {
			laneEnterCalls++;
		}

		@Override
		public void onCaptureTick(UnitContext context) {
			captureTickCalls++;
		}

		@Override
		public void onCaptureScore(UnitContext context) {
			captureScoreCalls++;
		}
	}
}
