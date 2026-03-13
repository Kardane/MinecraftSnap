package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.config.TextConfigFile;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.CaptainState;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class CaptainSpawnGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UnitRegistry unitRegistry;
	private final UiSoundService uiSoundService;

	public CaptainSpawnGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry) {
		this(textTemplateResolver, unitRegistry, null);
	}

	public CaptainSpawnGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry, UiSoundService uiSoundService) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
		this.uiSoundService = uiSoundService;
	}

	public void open(ServerPlayerEntity player, FactionId factionId, CaptainState captainState, String title, Consumer<UnitDefinition> onSelect) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(title));
		int slot = 10;
		for (var definition : unitRegistry.byFaction(factionId)) {
			var blocked = captainState.getCurrentMana() < definition.cost() || captainState.getSpawnCooldownSeconds() > 0;
			var lore = new java.util.ArrayList<String>();
			lore.add(textConfig().captainSpawnCostLoreTemplate.replace("{cost}", Integer.toString(definition.cost())));
			lore.add(textConfig().captainSpawnCooldownLoreTemplate.replace("{seconds}", Integer.toString(definition.spawnCooldownSeconds())));
			lore.add(textConfig().captainSpawnHealthLoreTemplate.replace("{health}", Integer.toString((int) definition.maxHealth())));
			lore.addAll(definition.descriptionLines());
			lore.add(blocked ? textConfig().captainSpawnBlockedLore : textConfig().captainSpawnReadyLore);
			var builder = new GuiElementBuilder(definition.mainHandItem())
				.setName(textTemplateResolver.format("&f" + definition.displayName()))
				.setLore(lore.stream().map(textTemplateResolver::format).toList());
			if (!blocked) {
				builder.glow();
				builder.setCallback((index, clickType, action, slotGui) -> {
					if (uiSoundService != null) {
						uiSoundService.playUiConfirm(gui.getPlayer());
					}
					onSelect.accept(definition);
					gui.close();
				});
			} else {
				builder.setCallback((index, clickType, action, slotGui) -> {
					if (uiSoundService != null) {
						uiSoundService.playUiDeny(gui.getPlayer());
					}
				});
			}
			gui.setSlot(slot++, builder.build());
		}
		gui.open();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
