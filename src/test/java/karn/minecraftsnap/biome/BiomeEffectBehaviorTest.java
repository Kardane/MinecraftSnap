package karn.minecraftsnap.biome;

import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeEffectBehaviorTest {
	@Test
	void badlandsAddsBonusTeamScoreOnCapture() {
		var matchManager = new MatchManager();
		matchManager.applyGameDuration(300);
		matchManager.setPhase(MatchPhase.GAME_RUNNING);
		var context = context(matchManager, new BadlandsBiomeEffect(), biome("badlands"));

		context.laneRuntime().biomeEffect().onCaptureScore(context, TeamId.RED);

		assertEquals(1, matchManager.getRedScore());
		assertEquals(0, matchManager.getBlueScore());
	}

	@Test
	void endRevealReducesRemainingTimeBySixtySeconds() {
		var matchManager = new MatchManager();
		matchManager.applyGameDuration(300);
		matchManager.setPhase(MatchPhase.GAME_START);
		var context = context(matchManager, new EndBiomeEffect(), biome("end"));

		context.laneRuntime().biomeEffect().onReveal(context);

		assertEquals(240, matchManager.getRemainingSeconds());
	}

	@Test
	void swampImmunityIncludesBoggedAndSlimes() {
		assertTrue(SwampBiomeEffect.isImmune("bogged"));
		assertTrue(SwampBiomeEffect.isImmune("slime"));
		assertTrue(SwampBiomeEffect.isImmune("giant_slime"));
	}

	private BiomeRuntimeContext context(MatchManager matchManager, BiomeEffect effect, BiomeEntry entry) {
		var runtime = new LaneRuntime(LaneId.LANE_1);
		runtime.revealBiome(entry, effect, 0);
		return new BiomeRuntimeContext(
			null,
			null,
			matchManager,
			runtime,
			entry,
			new TextTemplateResolver(),
			0L,
			0
		);
	}

	private BiomeEntry biome(String id) {
		var entry = new BiomeEntry();
		entry.id = id;
		entry.effectType = id;
		entry.displayName = id;
		entry.normalize();
		return entry;
	}
}
