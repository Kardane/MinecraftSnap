package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class FactionSelectionGuiService {
	private final TextTemplateResolver textTemplateResolver;

	public FactionSelectionGuiService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void open(ServerPlayerEntity player, TeamId teamId, FactionId selectedFaction, Consumer<FactionId> onSelect) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format(teamId == TeamId.RED ? "&c레드 팩션 선택" : "&9블루 팩션 선택"));
		gui.setSlot(11, buildFactionSlot(gui, FactionId.VILLAGER, Items.EMERALD, "&a주민", "&7균형 잡힌 기본 팩션", selectedFaction, onSelect));
		gui.setSlot(13, buildFactionSlot(gui, FactionId.MONSTER, Items.IRON_SWORD, "&c몬스터", "&7공격적인 운영용 팩션", selectedFaction, onSelect));
		gui.setSlot(15, buildFactionSlot(gui, FactionId.NETHER, Items.BLAZE_ROD, "&6네더", "&7특수 기믹 중심 팩션", selectedFaction, onSelect));
		gui.open();
	}

	private GuiElementInterface buildFactionSlot(
		SimpleGui gui,
		FactionId factionId,
		net.minecraft.item.Item item,
		String name,
		String lore,
		FactionId selectedFaction,
		Consumer<FactionId> onSelect
	) {
		var builder = new GuiElementBuilder(item)
			.setName(textTemplateResolver.format(name))
			.setLore(lines(lore, selectedFaction == factionId ? "&a현재 선택됨" : "&e클릭해서 선택"))
			.setCallback((index, clickType, action, slotGui) -> {
				onSelect.accept(factionId);
				gui.close();
			});

		if (selectedFaction == factionId) {
			builder.glow();
		}

		return builder.build();
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}
}
