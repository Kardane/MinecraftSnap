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

	@Test
	void preferRegistersRoleUnitArgumentTree() {
		var dispatcher = new CommandDispatcher<ServerCommandSource>();
		new McSnapCommandRegistrar(null).register(dispatcher, null, null);

		var prefer = dispatcher.getRoot().getChild("mcsnap").getChild("prefer");
		assertNotNull(prefer.getChild("captain"));
		assertNotNull(prefer.getChild("none"));
		assertNotNull(prefer.getChild("role"));
		assertEquals("unit", prefer.getChild("role").getChildren().iterator().next().getName());
		assertNull(prefer.getChild("unit"));
	}

	@Test
	void adminRegistersUnitCommandTree() {
		var dispatcher = new CommandDispatcher<ServerCommandSource>();
		new McSnapCommandRegistrar(null).register(dispatcher, null, null);

		var admin = dispatcher.getRoot().getChild("mcsnap").getChild("admin");
		assertNotNull(admin.getChild("unit"));
		assertEquals("player", admin.getChild("unit").getChildren().iterator().next().getName());
	}

	@Test
	void adminRegistersBiomeStructureCommandTree() {
		var dispatcher = new CommandDispatcher<ServerCommandSource>();
		new McSnapCommandRegistrar(null).register(dispatcher, null, null);

		var biomestructure = dispatcher.getRoot().getChild("mcsnap").getChild("admin").getChild("biomestructure");
		assertNotNull(biomestructure.getChild("place_nearest"));
		assertNotNull(biomestructure.getChild("place_nearest").getChild("structure_id"));
		assertNotNull(biomestructure.getChild("reset_all"));
	}

	@Test
	void adminRegistersBotsCommandTree() {
		var dispatcher = new CommandDispatcher<ServerCommandSource>();
		new McSnapCommandRegistrar(null).register(dispatcher, null, null);

		var bots = dispatcher.getRoot().getChild("mcsnap").getChild("admin").getChild("bots");
		assertNotNull(bots);
		assertNotNull(bots.getChild("count"));
	}
}
