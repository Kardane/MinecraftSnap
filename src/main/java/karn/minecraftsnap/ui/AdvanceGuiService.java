package karn.minecraftsnap.ui;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.MinecraftSnap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.game.AdvanceService;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AdvanceGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UiSoundService uiSoundService;

	public AdvanceGuiService(TextTemplateResolver textTemplateResolver) {
		this(textTemplateResolver, null);
	}

	public AdvanceGuiService(TextTemplateResolver textTemplateResolver, UiSoundService uiSoundService) {
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
	}

	public void open(
		ServerPlayerEntity player,
		List<AdvanceOptionView> options,
		Consumer<String> onAdvance
	) {
		var gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig().advanceGuiTitle));
		if (options == null || options.isEmpty()) {
			gui.setSlot(0, new GuiElementBuilder(Items.BARRIER)
				.setName(textTemplateResolver.format(textConfig().advanceNoOptionName))
				.setLore(lines(textConfig().advanceNoOptionLore))
				.build());
			gui.open();
			return;
		}
		for (int slot = 0; slot < Math.min(5, options.size()); slot++) {
			var option = options.get(slot);
			var definition = option.definition();
			var item = definition == null || definition.mainHandItem() == null ? Items.BARRIER : definition.mainHandItem();
			gui.setSlot(slot, new GuiElementBuilder(item)
				.setName(textTemplateResolver.format("&f" + option.displayName()))
				.setLore(lines(buildOptionLore(option).toArray(String[]::new)))
				.setCallback((index, clickType, action, slotGui) -> {
					if (option.ready()) {
						if (uiSoundService != null) {
							uiSoundService.playUiConfirm(gui.getPlayer());
						}
						onAdvance.accept(option.resultUnitId());
						gui.close();
					} else if (uiSoundService != null) {
						uiSoundService.playUiDeny(gui.getPlayer());
					}
				})
				.build());
		}
		gui.open();
	}

	static List<String> buildOptionLore(AdvanceOptionView option) {
		var textConfig = textConfigStatic();
		var lines = new ArrayList<String>();
		lines.addAll(option.descriptionLines());
		lines.add(textConfig.advanceRequiredBiomeLoreTemplate.replace("{biomes}", String.join(", ", option.biomes())));
		lines.add(textConfig.advanceRequiredWeatherLoreTemplate.replace("{weathers}", String.join(", ", option.weathers())));
		lines.add(textConfig.advanceProgressLoreTemplate
			.replace("{current}", Integer.toString(option.currentTicks()))
			.replace("{required}", Integer.toString(option.requiredTicks())));
		if (!option.conditionsMet()) {
			lines.add(textConfig.advanceConditionUnmetLore);
		} else if (!option.ready()) {
			lines.add(textConfig.advanceWaitingLore);
		} else {
			lines.add(textConfig.advanceClickLore);
		}
		return lines;
	}

	private List<net.minecraft.text.Text> lines(String... values) {
		return java.util.Arrays.stream(values)
			.map(textTemplateResolver::format)
			.toList();
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	private static TextConfigFile textConfigStatic() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	public record AdvanceOptionView(
		String resultUnitId,
		String displayName,
		List<String> descriptionLines,
		List<String> biomes,
		List<String> weathers,
		int currentTicks,
		int requiredTicks,
		boolean conditionsMet,
		boolean ready,
		UnitDefinition definition
	) {
		public static AdvanceOptionView from(AdvanceService.AdvanceOptionState state) {
			var option = state.option();
			return new AdvanceOptionView(
				option.resultUnitId,
				option.displayName == null || option.displayName.isBlank()
					? (state.definition() == null ? option.resultUnitId : state.definition().displayName())
					: option.displayName,
				option.descriptionLines,
				option.biomes,
				option.weathers,
				state.currentTicks(),
				option.requiredTicks,
				state.conditionsMet(),
				state.ready(),
				state.definition()
			);
		}
	}
}
