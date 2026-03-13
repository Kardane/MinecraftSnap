package karn.minecraftsnap.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossBarServiceTest {
	@Test
	void factionSelectHelpersRenderCountdownAndPercent() {
		assertEquals("&6팩션 선택 남은 시간 &f00:15", BossBarService.factionSelectText(15, "&6팩션 선택 남은 시간 &f{time}"));
		assertEquals(15, BossBarService.factionSelectRemainingSeconds(0L, 15));
		assertEquals(8.0f / 15.0f, BossBarService.factionSelectPercent(150L, 15));
		assertEquals(0.0f, BossBarService.factionSelectPercent(400L, 15));
	}

	@Test
	void gameRunningColorFollowsLeadingTeamAndUsesYellowOnTie() {
		assertEquals(net.minecraft.entity.boss.BossBar.Color.RED, BossBarService.gameRunningColor(5, 3));
		assertEquals(net.minecraft.entity.boss.BossBar.Color.BLUE, BossBarService.gameRunningColor(3, 5));
		assertEquals(net.minecraft.entity.boss.BossBar.Color.YELLOW, BossBarService.gameRunningColor(4, 4));
	}
}
