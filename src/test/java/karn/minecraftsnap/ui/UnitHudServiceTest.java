package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitHudServiceTest {
	@Test
	void formatsReadyActionBar() {
		var definition = new UnitDefinition(
			"villager",
			"주민",
			FactionId.VILLAGER,
			true,
			1,
			5,
			20.0,
			1.0,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			"상점 열기",
			8,
			UnitDefinition.AmmoType.NONE,
			null,
			List.of(),
			List.of()
		);

		var systemConfig = new SystemConfig();
		assertEquals("&f주민 &8| &b상점 열기 &8| &a준비 완료", UnitHudService.formatActionBar(definition, 0, systemConfig));
		assertEquals("&f주민 &8| &b상점 열기 &8| &e3초", UnitHudService.formatActionBar(definition, 3, systemConfig));
	}
}
