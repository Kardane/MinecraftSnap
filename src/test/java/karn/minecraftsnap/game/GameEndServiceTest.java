package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.util.TextTemplateResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameEndServiceTest {
	@Test
	void gameEndAppliesGlowTickRateAndLobbyReturn() {
		var manager = new MatchManager();
		var events = new ArrayList<String>();
		var service = new GameEndService(
			manager,
			new TextTemplateResolver(),
			message -> events.add("title:" + message),
			(team, seconds) -> events.add("glow:" + team + ":" + seconds),
			() -> events.add("clear"),
			rate -> events.add("tick:" + rate),
			() -> events.add("reward"),
			() -> events.add("lobby"),
			() -> events.add("restore")
		);
		var config = new SystemConfig();

		manager.setPhase(MatchPhase.GAME_RUNNING);
		manager.recordAllPointsHeld(TeamId.RED, 1);

		service.tick(config);
		assertEquals("title:&6승리: &f레드", events.get(0));
		assertEquals("reward", events.get(1));
		assertEquals("glow:RED:5", events.get(2));
		assertEquals("tick:10", events.get(3));

		for (int i = 0; i < config.gameEnd.returnToLobbyDelaySeconds * 20; i++) {
			manager.tick();
		}
		service.tick(config);

		assertEquals("tick:20", events.get(4));
		assertEquals("clear", events.get(5));
		assertEquals("restore", events.get(6));
		assertEquals("lobby", events.get(7));
	}
}
