package karn.minecraftsnap.game;

import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.biome.ReverseIcicleBiomeEffect;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePointServiceTest {
	@Test
	void containsReturnsTrueOnlyInsideCuboidCaptureRegion() {
		var region = SystemConfig.CaptureRegionConfig.create("test", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);

		assertTrue(CapturePointService.contains(region, new Vec3d(0.0, 64.0, 0.0)));
		assertFalse(CapturePointService.contains(region, new Vec3d(5.0, 64.0, 0.0)));
	}

	@Test
	void captureRegionMustStayInsideLaneRegion() {
		var lane = SystemConfig.LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		var inside = SystemConfig.CaptureRegionConfig.create("inside", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);
		var outside = SystemConfig.CaptureRegionConfig.create("outside", -20.0, 60.0, -4.0, 4.0, 68.0, 4.0);

		assertTrue(CapturePointService.contains(lane, inside));
		assertFalse(CapturePointService.contains(lane, outside));
	}

	@Test
	void onlyAssignedUnitsCountAsCaptureParticipant() {
		var captain = new PlayerMatchState();
		captain.setTeam(TeamId.RED, RoleType.CAPTAIN);
		var unit = new PlayerMatchState();
		unit.setTeam(TeamId.RED, RoleType.UNIT);
		unit.setCurrentUnitId("villager");
		var unassignedUnit = new PlayerMatchState();
		unassignedUnit.setTeam(TeamId.RED, RoleType.UNIT);
		var spectator = new PlayerMatchState();
		spectator.setTeam(TeamId.RED, RoleType.SPECTATOR);

		assertFalse(CapturePointService.countsForCapture(captain, false));
		assertTrue(CapturePointService.countsForCapture(unit, false));
		assertFalse(CapturePointService.countsForCapture(unassignedUnit, false));
		assertFalse(CapturePointService.countsForCapture(spectator, false));
		assertFalse(CapturePointService.countsForCapture(captain, true));
	}

	@Test
	void neutralOwnerUsesWhiteParticleColor() {
		assertEquals(0xFFFFFF, CapturePointService.color(CaptureOwner.NEUTRAL));
	}

	@Test
	void inactiveLaneStillRendersParticlesWhenRegionIsValid() {
		var lane = SystemConfig.LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		var capture = SystemConfig.CaptureRegionConfig.create("inside", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);
		capture.enabled = true;

		assertTrue(CapturePointService.shouldRenderParticles(false, lane, capture));
		assertTrue(CapturePointService.shouldRenderParticles(true, lane, capture));
	}

	@Test
	void particleBeamUsesConfiguredDefaults() {
		var capture = SystemConfig.CaptureRegionConfig.create("inside", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);

		assertEquals(0.0D, CapturePointService.particleCenterX(capture));
		assertEquals(0.0D, CapturePointService.particleCenterZ(capture));
		assertEquals(30.0D, CapturePointService.particleBeamTopY());
		assertEquals(0.5D, CapturePointService.particleSpacing());
		assertEquals(3.0F, CapturePointService.particleSize());
	}

	@Test
	void particleBeamStepLoopsAcrossFullColumn() {
		assertEquals(0, CapturePointService.particleBeamStep(0L, 81));
		assertEquals(1, CapturePointService.particleBeamStep(2L, 81));
		assertEquals(80, CapturePointService.particleBeamStep(160L, 81));
		assertEquals(0, CapturePointService.particleBeamStep(162L, 81));
	}

	@Test
	void fireworkLaunchPositionsUseAllCaptureCorners() {
		var capture = SystemConfig.CaptureRegionConfig.create("inside", -4.0, 60.0, -8.0, 4.0, 68.0, 8.0);

		assertEquals(List.of(
			new Vec3d(-4.0, 68.5, -8.0),
			new Vec3d(-4.0, 68.5, 8.0),
			new Vec3d(4.0, 68.5, -8.0),
			new Vec3d(4.0, 68.5, 8.0)
		), CapturePointService.fireworkLaunchPositions(capture));
	}

	@Test
	void reverseIcicleMakesCaptureScoreAmountZero() {
		var service = new CapturePointService(new MatchManager(), null, null, new TextTemplateResolver(), null, null);
		var entry = new BiomeEntry();
		entry.id = "reverse_icicle";
		entry.effectType = "reverse_icicle";
		entry.displayName = "역고드름";
		entry.normalize();
		var runtime = new LaneRuntime(LaneId.LANE_1);
		runtime.revealBiome(entry, new ReverseIcicleBiomeEffect(), 0);
		var context = new BiomeRuntimeContext(null, null, new MatchManager(), runtime, entry, new TextTemplateResolver(), 0L, 0);

		assertEquals(0, service.captureScoreAmount(context, TeamId.RED));
	}
}
