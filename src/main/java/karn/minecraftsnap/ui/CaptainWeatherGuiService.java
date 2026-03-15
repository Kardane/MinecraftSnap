package karn.minecraftsnap.ui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.function.Consumer;

public class CaptainWeatherGuiService {
	private final TextTemplateResolver textTemplateResolver;
	private final UiSoundService uiSoundService;

	public CaptainWeatherGuiService(TextTemplateResolver textTemplateResolver, UiSoundService uiSoundService) {
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
	}

	public void open(
		ServerPlayerEntity player,
		TextConfigFile textConfig,
		WeatherOption current,
		int remainingSeconds,
		Consumer<WeatherOption> onSelect
	) {
		var gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false);
		gui.setTitle(textTemplateResolver.format(textConfig.captainMonsterWeatherGuiTitle));
		gui.setSlot(0, option(Items.FEATHER, textConfig.captainMonsterWeatherClearName, textConfig, current, remainingSeconds, WeatherOption.CLEAR, onSelect));
		gui.setSlot(2, option(Items.WATER_BUCKET, textConfig.captainMonsterWeatherRainName, textConfig, current, remainingSeconds, WeatherOption.RAIN, onSelect));
		gui.setSlot(4, option(Items.TRIDENT, textConfig.captainMonsterWeatherThunderName, textConfig, current, remainingSeconds, WeatherOption.THUNDER, onSelect));
		gui.open();
	}

	private eu.pb4.sgui.api.elements.GuiElementInterface option(
		net.minecraft.item.Item item,
		String name,
		TextConfigFile textConfig,
		WeatherOption current,
		int remainingSeconds,
		WeatherOption target,
		Consumer<WeatherOption> onSelect
	) {
		var lore = optionLore(textConfig, current, remainingSeconds, target);
		return new GuiElementBuilder(item)
			.setName(textTemplateResolver.format(name))
			.setLore(lore.stream().map(textTemplateResolver::format).toList())
			.glow(current == target)
			.setCallback((index, clickType, actionType, gui) -> {
				if (uiSoundService != null && gui.getPlayer() instanceof ServerPlayerEntity player) {
					uiSoundService.playUiClick(player);
				}
				onSelect.accept(target);
			})
			.build();
	}

	static List<String> optionLore(TextConfigFile textConfig, WeatherOption current, int remainingSeconds, WeatherOption target) {
		var lore = new java.util.ArrayList<String>();
		lore.add(description(textConfig, target));
		if (current == target) {
			lore.add(textConfig.captainMonsterWeatherActiveLore);
			lore.add(remainingSeconds > 0
				? textConfig.captainMonsterWeatherRemainingLoreTemplate.replace("{seconds}", Integer.toString(remainingSeconds))
				: textConfig.captainMonsterWeatherPersistentLore);
		} else {
			lore.add(textConfig.captainMonsterWeatherClickLore);
		}
		return lore;
	}

	private static String description(TextConfigFile textConfig, WeatherOption target) {
		return switch (target) {
			case CLEAR -> textConfig.captainMonsterWeatherClearLore;
			case RAIN -> textConfig.captainMonsterWeatherRainLore;
			case THUNDER -> textConfig.captainMonsterWeatherThunderLore;
		};
	}

	public enum WeatherOption {
		CLEAR,
		RAIN,
		THUNDER
	}
}
