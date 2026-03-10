package karn.minecraftsnap.command;

public final class TeamChatParser {
	private TeamChatParser() {
	}

	public static String extractContent(String message) {
		if (message == null || !message.startsWith("!")) {
			return null;
		}

		var content = message.substring(1).trim();
		return content.isEmpty() ? "" : content;
	}
}
