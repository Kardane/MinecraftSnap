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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
				.setLore(buildOptionLoreTexts(option))
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
		var conditionLine = buildConditionLineString(option, textConfig);
		if (conditionLine != null && !conditionLine.isBlank()) {
			lines.add(conditionLine);
		}
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

	private List<Text> buildOptionLoreTexts(AdvanceOptionView option) {
		var textConfig = textConfig();
		var lines = new ArrayList<Text>();
		var conditionLine = buildConditionLineText(option, textConfig);
		if (conditionLine != null) {
			lines.add(conditionLine);
		}
		lines.add(textTemplateResolver.format(textConfig.advanceProgressLoreTemplate
			.replace("{current}", Integer.toString(option.currentTicks()))
			.replace("{required}", Integer.toString(option.requiredTicks()))));
		if (!option.conditionsMet()) {
			lines.add(textTemplateResolver.format(textConfig.advanceConditionUnmetLore));
		} else if (!option.ready()) {
			lines.add(textTemplateResolver.format(textConfig.advanceWaitingLore));
		} else {
			lines.add(textTemplateResolver.format(textConfig.advanceClickLore));
		}
		return lines;
	}

	private static String buildConditionLineString(AdvanceOptionView option, TextConfigFile textConfig) {
		var seconds = Integer.toString(Math.max(1, option.requiredTicks() / 20));
		var biomes = formatBiomeNames(option.biomes());
		var weather = formatWeatherNames(option.weathers());
		if (!biomes.isBlank() && !weather.isBlank()) {
			return textConfig.advanceConditionBiomeWeatherDurationLoreTemplate
				.replace("{biomes}", biomes)
				.replace("{weather}", weather)
				.replace("{seconds}", seconds);
		}
		if (!biomes.isBlank()) {
			return textConfig.advanceConditionBiomeDurationLoreTemplate
				.replace("{biomes}", biomes)
				.replace("{seconds}", seconds);
		}
		if (!weather.isBlank()) {
			return textConfig.advanceConditionWeatherDurationLoreTemplate
				.replace("{weather}", weather)
				.replace("{seconds}", seconds);
		}
		return null;
	}

	private Text buildConditionLineText(AdvanceOptionView option, TextConfigFile textConfig) {
		var seconds = Integer.toString(Math.max(1, option.requiredTicks() / 20));
		var weather = formatWeatherNames(option.weathers());
		var biomeTexts = biomeTexts(option.biomes());
		if (!biomeTexts.isEmpty() && !weather.isBlank()) {
			return textTemplateResolver.format(textConfig.advanceConditionPrefix)
				.copy()
				.append(joinBiomeTexts(biomeTexts))
				.append(textTemplateResolver.format(textConfig.advanceConditionBiomeWeatherDurationSuffixTemplate
					.replace("{weather}", weather)
					.replace("{seconds}", seconds)));
		}
		if (!biomeTexts.isEmpty()) {
			return textTemplateResolver.format(textConfig.advanceConditionPrefix)
				.copy()
				.append(joinBiomeTexts(biomeTexts))
				.append(textTemplateResolver.format(textConfig.advanceConditionBiomeDurationSuffixTemplate
					.replace("{seconds}", seconds)));
		}
		if (!weather.isBlank()) {
			return textTemplateResolver.format(textConfig.advanceConditionWeatherDurationLoreTemplate
				.replace("{weather}", weather)
				.replace("{seconds}", seconds));
		}
		return null;
	}

	private static String formatBiomeNames(List<String> biomes) {
		if (biomes == null || biomes.isEmpty()) {
			return "";
		}
		return biomes.stream()
			.map(AdvanceGuiService::formatResourceName)
			.distinct()
			.reduce((left, right) -> left + ", " + right)
			.orElse("");
	}

	private List<Text> biomeTexts(List<String> biomes) {
		if (biomes == null || biomes.isEmpty()) {
			return List.of();
		}
		var texts = new ArrayList<Text>();
		for (int index = 0; index < biomes.size(); index++) {
			var biome = biomes.get(index);
			if (biome == null || biome.isBlank()) {
				continue;
			}
			if (!texts.isEmpty()) {
				texts.add(Text.literal(", "));
			}
			texts.add(biomeText(biome));
		}
		return texts;
	}

	private Text joinBiomeTexts(List<Text> biomeTexts) {
		var result = Text.empty();
		for (var biomeText : biomeTexts) {
			result.append(biomeText);
		}
		return result;
	}

	private static Text biomeText(String biomeId) {
		try {
			var id = Identifier.of(biomeId);
			return Text.translatable("biome." + id.getNamespace() + "." + id.getPath());
		} catch (Exception ignored) {
			return Text.literal(formatResourceName(biomeId));
		}
	}

	private static String formatWeatherNames(List<String> weathers) {
		if (weathers == null || weathers.isEmpty()) {
			return "";
		}
		return weathers.stream()
			.map(AdvanceGuiService::formatWeatherName)
			.distinct()
			.reduce((left, right) -> left + ", " + right)
			.orElse("");
	}

	private static String formatResourceName(String id) {
		if (id == null || id.isBlank()) {
			return "";
		}
		var value = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		var builder = new StringBuilder();
		for (var part : value.split("_")) {
			if (part.isBlank()) {
				continue;
			}
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1));
			}
		}
		return builder.toString();
	}

	private static String formatWeatherName(String weather) {
		return switch (weather) {
			case "clear" -> textConfigStatic().captainMonsterWeatherClearName;
			case "rain" -> textConfigStatic().captainMonsterWeatherRainName;
			case "thunder" -> textConfigStatic().captainMonsterWeatherThunderName;
			default -> formatResourceName(weather);
		};
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
