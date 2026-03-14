package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.TextConfigFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptainWeatherGuiServiceTest {
	@Test
	void currentWeatherLoreIncludesDescriptionAndRemainingTime() {
		var textConfig = new TextConfigFile();
		var lore = CaptainWeatherGuiService.optionLore(
			textConfig,
			CaptainWeatherGuiService.WeatherOption.THUNDER,
			42,
			CaptainWeatherGuiService.WeatherOption.THUNDER
		);

		assertEquals(textConfig.captainMonsterWeatherThunderLore, lore.getFirst());
		assertTrue(lore.stream().anyMatch(line -> line.contains("42초")));
	}

	@Test
	void inactiveWeatherLoreIncludesDescriptionAndClickHint() {
		var textConfig = new TextConfigFile();
		var lore = CaptainWeatherGuiService.optionLore(
			textConfig,
			CaptainWeatherGuiService.WeatherOption.CLEAR,
			0,
			CaptainWeatherGuiService.WeatherOption.RAIN
		);

		assertEquals(textConfig.captainMonsterWeatherRainLore, lore.getFirst());
		assertTrue(lore.stream().anyMatch(line -> line.contains("클릭해서 변경")));
	}
}
