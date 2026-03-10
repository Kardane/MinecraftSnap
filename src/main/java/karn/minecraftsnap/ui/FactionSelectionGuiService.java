package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.FactionConfigFile;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class FactionSelectionGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UnitRegistry unitRegistry;

	public FactionSelectionGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
	}

	public void open(ServerPlayerEntity player, TeamId teamId, FactionId selectedFaction, Consumer<FactionId> onSelect) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(teamId == TeamId.RED ? "&c레드 팩션 선택" : "&9블루 팩션 선택"));
		gui.setSlot(11, buildFactionSlot(gui, FactionId.VILLAGER, Items.EMERALD, selectedFaction, onSelect));
		gui.setSlot(13, buildFactionSlot(gui, FactionId.MONSTER, Items.IRON_SWORD, selectedFaction, onSelect));
		gui.setSlot(15, buildFactionSlot(gui, FactionId.NETHER, Items.BLAZE_ROD, selectedFaction, onSelect));
		gui.open();
	}

	private GuiElementInterface buildFactionSlot(
		SimpleGui gui,
		FactionId factionId,
		net.minecraft.item.Item item,
		FactionId selectedFaction,
		Consumer<FactionId> onSelect
	) {
		var config = unitRegistry.getFactionConfig(factionId);
		var name = config == null ? factionId.name() : config.displayName;
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.format("&f" + name))
			.setLore(buildLore(config, selectedFaction == factionId))
			.setCallback((index, clickType, action, slotGui) -> {
				onSelect.accept(factionId);
				gui.close();
			});

		if (selectedFaction == factionId) {
			builder.glow();
		}

		return builder.build();
	}

	private List<net.minecraft.text.Text> buildLore(FactionConfigFile config, boolean selected) {
		var lines = new java.util.ArrayList<String>();
		if (config != null) {
			lines.addAll(config.summaryLines);
			if (config.captainSkill != null && config.captainSkill.name != null && !config.captainSkill.name.isBlank()) {
				lines.add("&8사령관 스킬: &d" + config.captainSkill.name);
			}
		}
		lines.add(selected ? "&a현재 선택됨" : "&e클릭해서 선택");
		return lines(lines.toArray(String[]::new));
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}
}
