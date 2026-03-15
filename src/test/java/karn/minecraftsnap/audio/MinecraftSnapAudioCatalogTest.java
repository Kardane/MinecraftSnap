package karn.minecraftsnap.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
