package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.UnitRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class PreparationGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UnitRegistry unitRegistry;

	public PreparationGuiService(TextTemplateResolver textTemplateResolver, UnitRegistry unitRegistry) {
		this.textTemplateResolver = textTemplateResolver;
		this.unitRegistry = unitRegistry;
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&6게임 준비"));

		var units = unitsFor(state.getFactionId());
		for (int i = 0; i < units.size(); i++) {
			var unit = units.get(i);
			boolean selected = unit.id().equals(state.getPreferredUnitId());
			var lore = new java.util.ArrayList<String>();
			lore.add(state.getRoleType() == RoleType.UNIT ? "&7클릭해서 우선 배정 토글" : "&7사령관은 읽기 전용");
			lore.add("&7코스트: &b" + unit.cost());
			lore.addAll(unit.descriptionLines());
			lore.add(selected ? "&a현재 우선 배정됨" : "&8미선택");
			var builder = new GuiElementBuilder(unit.mainHandItem())
				.setName(textTemplateResolver.format("&f" + unit.displayName()))
				.setLore(lore.stream().map(textTemplateResolver::format).toList());
			if (selected) {
				builder.glow();
			}
			if (state.getRoleType() == RoleType.UNIT) {
				builder.setCallback((index, clickType, action, slotGui) -> {
					state.setPreferredUnitId(selected ? null : unit.id());
					gui.close();
				});
			}
			gui.setSlot(10 + i, builder.build());
		}

		gui.open();
	}

	private List<karn.minecraftsnap.game.UnitDefinition> unitsFor(FactionId factionId) {
		if (factionId == null) {
			return List.of();
		}
		return unitRegistry.byFaction(factionId).stream().toList();
	}
}
