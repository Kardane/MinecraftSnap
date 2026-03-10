package karn.minecraftsnap.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeamChatParserTest {
	@Test
	void teamChatPrefixDetected() {
		assertEquals("안녕하세요", TeamChatParser.extractContent("!안녕하세요"));
	}

	@Test
	void leadingWhitespaceRemoved() {
		assertEquals("반갑다", TeamChatParser.extractContent("!   반갑다"));
	}

	@Test
	void globalMessageIgnored() {
		assertNull(TeamChatParser.extractContent("안녕하세요"));
	}
}
