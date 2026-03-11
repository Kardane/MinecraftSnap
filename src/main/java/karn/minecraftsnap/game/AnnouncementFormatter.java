package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;

public final class AnnouncementFormatter {
	private AnnouncementFormatter() {
	}

	public static String phaseMessage(MatchPhase phase, SystemConfig.AnnouncementConfig config) {
		return switch (phase) {
			case LOBBY -> config.lobbyPhaseMessage;
			case TEAM_SELECT -> config.teamSelectPhaseMessage;
			case FACTION_SELECT -> config.factionSelectPhaseMessage;
			case GAME_START -> config.gameStartPhaseMessage;
			case GAME_RUNNING -> config.gameRunningPhaseMessage;
			case GAME_END -> config.gameEndPhaseMessage;
		};
	}

	public static String factionSelectionMessage(TeamId teamId, FactionId factionId, SystemConfig.AnnouncementConfig config) {
		return config.factionSelectionMessage
			.replace("{team}", teamId.getDisplayName())
			.replace("{faction}", factionName(factionId, config));
	}

	private static String factionName(FactionId factionId, SystemConfig.AnnouncementConfig config) {
		if (factionId == null) {
			return "미정";
		}
		return switch (factionId) {
			case VILLAGER -> config.villagerFactionName;
			case MONSTER -> config.monsterFactionName;
			case NETHER -> config.netherFactionName;
		};
	}
}
