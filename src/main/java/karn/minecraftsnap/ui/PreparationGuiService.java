package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class PreparationGuiService {
	private final TextTemplateResolver textTemplateResolver;

	public PreparationGuiService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&6게임 준비"));

		var units = unitsFor(state.getFactionId());
		for (int i = 0; i < units.size(); i++) {
			var unit = units.get(i);
			boolean selected = unit.id().equals(state.getPreferredUnitId());
			var builder = new GuiElementBuilder(unit.item())
				.setName(textTemplateResolver.format(unit.displayName()))
				.setLore(List.of(
					textTemplateResolver.format(state.getRoleType() == RoleType.UNIT ? "&7클릭해서 우선 배정 토글" : "&7사령관은 읽기 전용"),
					textTemplateResolver.format(selected ? "&a현재 우선 배정됨" : "&8미선택")
				));
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

	private List<UnitOption> unitsFor(FactionId factionId) {
		if (factionId == FactionId.MONSTER) {
			return List.of(
				new UnitOption("zombie", "&2좀비", Items.IRON_SHOVEL),
				new UnitOption("skeleton", "&f스켈레톤", Items.BOW),
				new UnitOption("slime", "&a슬라임", Items.SLIME_BALL),
				new UnitOption("creeper", "&2크리퍼", Items.TNT)
			);
		}
		if (factionId == FactionId.NETHER) {
			return List.of(
				new UnitOption("piglin", "&6피글린", Items.GOLDEN_SWORD),
				new UnitOption("zombified_piglin", "&6좀비 피글린", Items.ROTTEN_FLESH),
				new UnitOption("blaze", "&e블레이즈", Items.BLAZE_ROD),
				new UnitOption("piglin_brute", "&6피글린 브루트", Items.GOLDEN_AXE)
			);
		}
		return List.of(
			new UnitOption("villager", "&a주민", Items.BREAD),
			new UnitOption("armorer_villager", "&7대장장이 주민", Items.SHIELD),
			new UnitOption("vindicator", "&f변명자", Items.IRON_AXE),
			new UnitOption("pillager", "&f약탈자", Items.CROSSBOW)
		);
	}

	private record UnitOption(String id, String displayName, net.minecraft.item.Item item) {
	}
}
