package karn.minecraftsnap.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MinecraftSnapAudioCatalogTest {
	@Test
	void catalogMatchesConfiguredTracks() {
		assertEquals(2, MinecraftSnapAudioCatalog.LOBBY_TRACKS.size());
		assertEquals("minecraftsnap:music.lobby", MinecraftSnapAudioCatalog.LOBBY.id().toString());
		assertEquals("minecraftsnap:music.lobby2", MinecraftSnapAudioCatalog.LOBBY2.id().toString());
		assertEquals("minecraftsnap:music.ready", MinecraftSnapAudioCatalog.READY.id().toString());
		assertEquals("minecraftsnap:music.game", MinecraftSnapAudioCatalog.GAME.id().toString());
		assertEquals("minecraftsnap:music.start", MinecraftSnapAudioCatalog.START.id().toString());
		assertEquals("minecraftsnap:music.end", MinecraftSnapAudioCatalog.END.id().toString());
	}

	@Test
	void loopDurationsMatchConvertedAudioLengths() {
		assertEquals(2182, MinecraftSnapAudioCatalog.LOBBY.durationTicks());
		assertEquals(2182, MinecraftSnapAudioCatalog.LOBBY2.durationTicks());
		assertEquals(4237, MinecraftSnapAudioCatalog.READY.durationTicks());
		assertEquals(4561, MinecraftSnapAudioCatalog.GAME.durationTicks());
	}

	@Test
	void cueTracksAreNotMarkedAsStreaming() throws Exception {
		try (var stream = getClass().getResourceAsStream("/assets/minecraftsnap/sounds.json")) {
			assertNotNull(stream);
			JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			assertFalse(root.getAsJsonObject("music.start").getAsJsonArray("sounds").get(0).getAsJsonObject().has("stream"));
			assertFalse(root.getAsJsonObject("music.end").getAsJsonArray("sounds").get(0).getAsJsonObject().has("stream"));
		}
	}
}
