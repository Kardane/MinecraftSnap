package karn.minecraftsnap.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McSnapCommandRegistrarTest {
	@Test
	void normalizeAdminGuiAcceptsOnlySupportedIds() {
		assertEquals("wiki", McSnapCommandRegistrar.normalizeAdminGui("wiki"));
		assertEquals("captain_spawn", McSnapCommandRegistrar.normalizeAdminGui("CAPTAIN_SPAWN"));
		assertEquals("advance", McSnapCommandRegistrar.normalizeAdminGui("advance"));
		assertNull(McSnapCommandRegistrar.normalizeAdminGui("unknown"));
	}
}
