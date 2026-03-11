package karn.minecraftsnap.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class McSnapCommandRegistrarTest {
	@Test
	void openguiRegistersLiteralChildren() {
		var dispatcher = new CommandDispatcher<ServerCommandSource>();
		new McSnapCommandRegistrar(null).register(dispatcher, null, null);

		var openGui = dispatcher.getRoot().getChild("mcsnap").getChild("admin").getChild("opengui");
		assertNotNull(openGui.getChild("wiki"));
		assertNotNull(openGui.getChild("captain_spawn"));
		assertNotNull(openGui.getChild("advance"));
		assertNull(openGui.getChild("gui"));
		assertEquals(6, McSnapCommandRegistrar.ADMIN_GUI_IDS.size());
	}
}
