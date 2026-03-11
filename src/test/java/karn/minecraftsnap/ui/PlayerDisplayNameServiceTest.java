package karn.minecraftsnap.ui;

import karn.minecraftsnap.game.RoleType;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDisplayNameServiceTest {
	@Test
	void buildDisplayNameAddsLadderAndCaptainStar() {
		var captain = PlayerDisplayNameService.buildDisplayName(Text.literal("Karned"), 300, RoleType.CAPTAIN);
		var unit = PlayerDisplayNameService.buildDisplayName(Text.literal("Karned"), 300, RoleType.UNIT);

		assertEquals("[300] ★ Karned", captain.getString());
		assertEquals("[300] Karned", unit.getString());
	}

	@Test
	void lobbyTabListShowsOnlyLadderWithoutCaptainStar() {
		var config = new karn.minecraftsnap.config.SystemConfig.DisplayConfig();
		var captainLobby = PlayerDisplayNameService.buildDisplayName(Text.literal("Karned"), 300, RoleType.CAPTAIN, MatchPhase.LOBBY, config);

		assertEquals("[300] Karned", captainLobby.getString());
		assertFalse(PlayerDisplayNameService.useStyledDisplayName(MatchPhase.LOBBY));
		assertTrue(PlayerDisplayNameService.useStyledDisplayName(MatchPhase.GAME_RUNNING));
	}

	@Test
	void displayTemplateCanBeConfigured() {
		var config = new karn.minecraftsnap.config.SystemConfig.DisplayConfig();
		config.ladderPrefixFormat = "<{ladder}> ";
		config.captainStar = "대장 ";

		var captain = PlayerDisplayNameService.buildDisplayName(Text.literal("Karned"), 450, RoleType.CAPTAIN, MatchPhase.GAME_RUNNING, config);

		assertEquals("<450> 대장 Karned", captain.getString());
	}

	@Test
	void teamColorIsAppliedToPlayerNameInTabList() {
		var config = new karn.minecraftsnap.config.SystemConfig.DisplayConfig();
		var captain = PlayerDisplayNameService.buildDisplayName(Text.literal("Karned"), 450, RoleType.CAPTAIN, TeamId.RED, MatchPhase.GAME_RUNNING, config);

		assertEquals(Formatting.RED.getColorValue(), captain.getSiblings().get(2).getStyle().getColor().getRgb());
	}
}
