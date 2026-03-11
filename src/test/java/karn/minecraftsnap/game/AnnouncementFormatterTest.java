package karn.minecraftsnap.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnouncementFormatterTest {
	@Test
	void phaseMessageReturnsExpectedText() {
		var config = new karn.minecraftsnap.config.SystemConfig.AnnouncementConfig();
		assertEquals("&6팩션 선택 시작", AnnouncementFormatter.phaseMessage(MatchPhase.FACTION_SELECT, config));
		assertEquals("&e로비로 복귀", AnnouncementFormatter.phaseMessage(MatchPhase.LOBBY, config));
	}

	@Test
	void factionSelectionMessageIncludesTeamAndFaction() {
		var config = new karn.minecraftsnap.config.SystemConfig.AnnouncementConfig();
		assertEquals("&f레드 팀 사령관이 &6주민 &f팩션 선택", AnnouncementFormatter.factionSelectionMessage(TeamId.RED, FactionId.VILLAGER, config));
		assertEquals("&f블루 팀 사령관이 &6네더 &f팩션 선택", AnnouncementFormatter.factionSelectionMessage(TeamId.BLUE, FactionId.NETHER, config));
	}

	@Test
	void announcementTextCanBeConfigured() {
		var config = new karn.minecraftsnap.config.SystemConfig.AnnouncementConfig();
		config.factionSelectionMessage = "{team}:{faction}";
		config.monsterFactionName = "적대";

		assertEquals("블루:적대", AnnouncementFormatter.factionSelectionMessage(TeamId.BLUE, FactionId.MONSTER, config));
	}
}
