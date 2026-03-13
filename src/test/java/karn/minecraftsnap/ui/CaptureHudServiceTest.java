package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.CaptureOwner;
import karn.minecraftsnap.game.CapturePointState;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.entity.boss.BossBar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptureHudServiceTest {
	private final SystemConfig.CaptureConfig captureConfig = new SystemConfig.CaptureConfig();

	@Test
	void contestedStateUsesCollisionText() {
		var state = new CapturePointState(LaneId.LANE_1);
		state.getProgress().setContested();

		assertEquals("&e격돌중!", CaptureHudService.formatText(state, captureConfig));
		assertEquals(1.0f, CaptureHudService.progressPercent(state.getProgress(), 5));
		assertEquals(BossBar.Color.YELLOW, CaptureHudService.colorOf(state));
	}

	@Test
	void captureProgressUsesSecondsAndTeamColor() {
		var state = new CapturePointState(LaneId.LANE_1);
		state.getProgress().start(TeamId.RED);
		for (int i = 0; i < 59; i++) {
			state.getProgress().increment();
		}

		assertEquals("&f중립 점령지 &8| &e3&7/&f5", CaptureHudService.formatText(state, captureConfig));
		assertEquals(0.6f, CaptureHudService.progressPercent(state.getProgress(), 5));
		assertEquals(BossBar.Color.RED, CaptureHudService.colorOf(state));
	}

	@Test
	void idleOwnerShowsOwnerOnly() {
		var state = new CapturePointState(LaneId.LANE_1);
		for (int i = 0; i < 20; i++) {
			state.update(TeamId.BLUE, false, 1);
		}

		assertEquals(CaptureOwner.BLUE, state.getOwner());
		assertEquals("&f블루 점령지", CaptureHudService.formatText(state, captureConfig));
		assertEquals(BossBar.Color.BLUE, CaptureHudService.colorOf(state));
	}
}
