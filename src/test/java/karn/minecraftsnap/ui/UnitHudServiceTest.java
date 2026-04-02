package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitHudServiceTest {
	@Test
	void actionBarIsOnlySentWhenContentChanges() {
		assertTrue(UnitHudService.shouldSendActionBar(null, "&f주민"));
		assertFalse(UnitHudService.shouldSendActionBar("&f주민", "&f주민"));
		assertTrue(UnitHudService.shouldSendActionBar("&f주민", "&f주민 &8| &e3초"));
	}

	@Test
	void blankActionBarClearsOnlyWhenPreviousContentExisted() {
		assertTrue(UnitHudService.shouldSendActionBar("&f주민", ""));
		assertFalse(UnitHudService.shouldSendActionBar("", ""));
		assertFalse(UnitHudService.shouldSendActionBar(null, ""));
	}

	@Test
	void persistentHudAlwaysRefreshesEvenWhenTextMatches() {
		assertTrue(UnitHudService.shouldRenderPersistentActionBar("&f동일", "&f동일"));
		assertTrue(UnitHudService.shouldRenderPersistentActionBar(null, "&f사령관"));
		assertTrue(UnitHudService.shouldRenderPersistentActionBar("&f사령관", ""));
	}

	@Test
	void formatsReadyActionBar() {
		var definition = new UnitDefinition(
			"villager",
			"주민",
			FactionId.VILLAGER,
			true,
			1,
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

	@Test
	void usesTextConfigCooldownTemplate() {
		var definition = new UnitDefinition(
			"villager",
			"주민",
			FactionId.VILLAGER,
			true,
			1,
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
		assertEquals("&f주민 &8| &b상점 열기 &8| &e7초", UnitHudService.formatActionBar(definition, 7, systemConfig));
	}

	@Test
	void hidesSkillSectionForUnitsWithoutActiveSkill() {
		var definition = new UnitDefinition(
			"armorer_villager",
			"갑옷 대장장이 주민",
			FactionId.VILLAGER,
			true,
			2,
			20.0,
			0.8,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			"",
			0,
			UnitDefinition.AmmoType.NONE,
			null,
			List.of(),
			List.of()
		);

		assertEquals("갑옷 대장장이 주민", UnitHudService.formatActionBar(definition, 0, new SystemConfig()));
	}
	@Test
	void formatsCaptainActionBar() {
		var systemConfig = new SystemConfig();
		systemConfig.display.captainHudTemplate = "&b{current_mana}&7/&f{max_mana} &8({mana_cooldown}초) &8| &e{lane} &8| &d{player} &8| &c{skill_cooldown}초";

		assertEquals(
			"&b2&7/&f5 &8(7초) &8| &e2라인 &8| &dAlex &8| &c15초",
			UnitHudService.formatCaptainActionBar(2, 5, 7, "2라인", "Alex", 15, systemConfig)
		);
		assertEquals(
			"&b3&7/&f3 &8(10초) &8| &e1라인 &8| &d대기 중 없음 &8| &c0초",
			UnitHudService.formatCaptainActionBar(3, 3, 10, "1라인", "", 0, systemConfig)
		);
	}

	@Test
	void formatsCaptainActionBarFromCustomTemplate() {
		var systemConfig = new SystemConfig();
		systemConfig.display.captainHudTemplate = "&f마나 {current_mana}/{max_mana} | {mana_cooldown} | {lane} | {player} | {skill_cooldown}";

		assertEquals(
			"&f마나 4/6 | 9 | 3라인 | Steve | 12",
			UnitHudService.formatCaptainActionBar(4, 6, 9, "3라인", "Steve", 12, systemConfig)
		);
	}

	@Test
	void formatsSpectatorQueueActionBar() {
		var systemConfig = new SystemConfig();

		assertEquals("&7소환 대기열: &f3번", UnitHudService.formatSpectatorQueueActionBar(3, systemConfig));
		assertEquals("", UnitHudService.formatSpectatorQueueActionBar(0, systemConfig));
	}
}
