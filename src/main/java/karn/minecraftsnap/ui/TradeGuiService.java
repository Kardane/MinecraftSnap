package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.PlayerMatchState;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class TradeGuiService {
	private final TextTemplateResolver textTemplateResolver;

	public TradeGuiService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void open(ServerPlayerEntity player, PlayerMatchState state) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&6거래창"));
		gui.setSlot(11, new GuiElementBuilder(Items.EMERALD)
			.setName(textTemplateResolver.format("&a에메랄드"))
			.setLore(lines(
				"&f보유량: &a" + state.getEmeralds(),
				"&7주민 팩션 화폐"
			))
			.build());
		gui.setSlot(15, new GuiElementBuilder(Items.GOLD_INGOT)
			.setName(textTemplateResolver.format("&6금괴"))
			.setLore(lines(
				"&f보유량: &6" + state.getGoldIngots(),
				"&7네더 팩션 화폐"
			))
			.build());
		gui.setSlot(13, new GuiElementBuilder(state.getFactionId() == FactionId.NETHER ? Items.BARREL : Items.CHEST)
			.setName(textTemplateResolver.format("&e상점 준비중"))
			.setLore(lines(
				"&7구매 기능은 다음 단계에서 연결",
				"&7이번 단계는 화폐 획득/표시만 지원"
			))
			.build());
		gui.open();
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}
}
