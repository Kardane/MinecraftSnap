package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class AdminToolsGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UiSoundService uiSoundService;
	private final Consumer<ServerPlayerEntity> mainMenuOpener;
	private final Consumer<ServerPlayerEntity> assignTeamsAction;
	private final Consumer<ServerPlayerEntity> clearTeamsAction;
	private final Consumer<ServerPlayerEntity> factionSelectAction;
	private final Consumer<ServerPlayerEntity> gameEndAction;

	public AdminToolsGuiService(
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Consumer<ServerPlayerEntity> mainMenuOpener,
		Consumer<ServerPlayerEntity> assignTeamsAction,
		Consumer<ServerPlayerEntity> clearTeamsAction,
		Consumer<ServerPlayerEntity> factionSelectAction,
		Consumer<ServerPlayerEntity> gameEndAction
	) {
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
		this.mainMenuOpener = mainMenuOpener;
		this.assignTeamsAction = assignTeamsAction;
		this.clearTeamsAction = clearTeamsAction;
		this.factionSelectAction = factionSelectAction;
		this.gameEndAction = gameEndAction;
	}

	public void open(ServerPlayerEntity player) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X1, player, false);
		gui.setTitle(textTemplateResolver.formatUi(textConfig().adminToolsGuiTitle));
		gui.setSlot(0, button(gui, Items.PURPLE_BANNER, textConfig().adminToolsAssignTeamsName, List.of(), () -> assignTeamsAction.accept(player)));
		gui.setSlot(1, button(gui, Items.WHITE_BANNER, textConfig().adminToolsClearTeamsName, List.of(), () -> clearTeamsAction.accept(player)));
		gui.setSlot(2, button(gui, Items.NETHER_STAR, textConfig().adminToolsFactionSelectName, List.of(), () -> factionSelectAction.accept(player)));
		gui.setSlot(3, button(gui, Items.REDSTONE, textConfig().adminToolsGameEndName, List.of(), () -> gameEndAction.accept(player)));
		gui.setSlot(8, button(gui, Items.BARRIER, textConfig().adminToolsCloseName, List.of(), () -> mainMenuOpener.accept(player)));
		gui.open();
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface button(SimpleGui gui, net.minecraft.item.Item item, String name, List<String> lore, Runnable callback) {
		return new GuiElementBuilder(item)
			.setName(textTemplateResolver.formatUi(name))
			.setLore(lore.stream().map(textTemplateResolver::formatUi).toList())
			.setCallback((index, clickType, actionType, slotGui) -> {
				if (uiSoundService != null) {
					uiSoundService.playUiClick(gui.getPlayer());
				}
				callback.run();
			})
			.build();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
