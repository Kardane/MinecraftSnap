package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class AdvanceGuiService {
	private final TextTemplateResolver textTemplateResolver;

	public AdvanceGuiService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void open(
		ServerPlayerEntity player,
		PlayerMatchState state,
		String biomeId,
		String weather,
		int requiredSeconds,
		UnitDefinition targetDefinition,
		Runnable onAdvance
	) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&c전직"));
		gui.setSlot(11, new GuiElementBuilder(Items.COMPASS)
			.setName(textTemplateResolver.format("&e현재 진행도"))
			.setLore(lines(
				"&f진행 시간: &b" + state.getAdvanceProgressSeconds() + "&7/&f" + requiredSeconds + "초",
				"&f현재 바이옴: &a" + biomeId,
				"&f현재 날씨: &b" + weather
			))
			.build());
		gui.setSlot(15, new GuiElementBuilder(targetDefinition == null ? Items.BARRIER : targetDefinition.mainHandItem())
			.setName(textTemplateResolver.format(targetDefinition == null ? "&c전직 대상 없음" : "&a전직 결과: &f" + targetDefinition.displayName()))
			.setLore(lines(
				state.isAdvanceAvailable() ? "&a클릭해서 전직" : "&c아직 전직할 수 없음",
				targetDefinition == null ? "&8컨픽 조건을 확인" : "&7준비되면 현재 유닛이 교체됨"
			))
			.setCallback((index, clickType, action, slotGui) -> {
				if (state.isAdvanceAvailable() && targetDefinition != null) {
					onAdvance.run();
					gui.close();
				}
			})
			.build());
		gui.open();
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}
}
