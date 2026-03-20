package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.EntitySpecEntry;
import karn.minecraftsnap.config.UnitItemEntry;
import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvanceGuiServiceTest {
	@Test
	void buildOptionLoreAddsConditionFailureLine() {
		var option = new AdvanceGuiService.AdvanceOptionView(
			"husk",
			"허스크",
			List.of("&7늪과 비를 버티면 강화"),
			List.of("minecraft:swamp"),
			List.of("rain"),
			3,
			10,
			false,
			false,
			new UnitDefinition(
				"husk",
				"허스크",
				FactionId.MONSTER,
				false,
				0,
				20.0,
				1.0,
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				new UnitItemEntry(),
				"",
				0,
				UnitDefinition.AmmoType.NONE,
				new EntitySpecEntry(),
				List.of(),
				List.of()
			)
		);

		var lore = AdvanceGuiService.buildOptionLore(option);

		assertTrue(lore.stream().anyMatch(line -> line.contains("조건:")));
		assertTrue(lore.stream().anyMatch(line -> line.contains("Swamp")));
		assertTrue(lore.stream().anyMatch(line -> line.contains("비")));
		assertTrue(lore.stream().anyMatch(line -> line.contains("조건이 충족되지 않음")));
		assertFalse(lore.stream().anyMatch(line -> line.contains("요구 바이옴")));
		assertFalse(lore.stream().anyMatch(line -> line.contains("버티면 적응")));
	}
}
