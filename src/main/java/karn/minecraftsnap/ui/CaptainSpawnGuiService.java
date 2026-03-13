package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
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
			lore.add("&7코스트: &b" + definition.cost());
			lore.add("&7생성 쿨다운: &e" + definition.spawnCooldownSeconds() + "초");
			lore.add("&7체력: &c" + (int) definition.maxHealth());
			lore.addAll(definition.descriptionLines());
			lore.add(blocked ? "&c현재 소환 불가" : "&a클릭해서 소환");
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
}
