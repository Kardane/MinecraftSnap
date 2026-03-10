package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class WikiGuiService {
	private final TextTemplateResolver textTemplateResolver;

	public WikiGuiService(TextTemplateResolver textTemplateResolver) {
		this.textTemplateResolver = textTemplateResolver;
	}

	public void open(ServerPlayerEntity player, MatchPhase phase) {
		var gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
		gui.setTitle(textTemplateResolver.format("&6MCsnap 안내"));
		gui.setSlot(10, new GuiElementBuilder(Items.BOOK)
			.setName(textTemplateResolver.format("&e현재 페이즈"))
			.setLore(lines(
				"&f지금 상태: &a" + phase.getDisplayName(),
				"&7로비 흐름을 확인하는 안내 창"
			))
			.build());
		gui.setSlot(13, new GuiElementBuilder(Items.CLOCK)
			.setName(textTemplateResolver.format("&e기본 흐름"))
			.setLore(lines(
				"&f1. 로비 대기",
				"&f2. 팀 자동 배정",
				"&f3. 사령관 팩션 선택",
				"&f4. 경기 시작"
			))
			.build());
		gui.setSlot(16, new GuiElementBuilder(Items.NAME_TAG)
			.setName(textTemplateResolver.format("&e단축 입력"))
			.setLore(lines(
				"&f쉬프트 + F",
				"&7현재 페이즈에 맞는 GUI 열기",
				"&7팩션 선택 중이면 사령관 GUI 우선"
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
