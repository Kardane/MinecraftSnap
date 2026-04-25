package karn.minecraftsnap.audio;

import karn.minecraftsnap.MinecraftSnap;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftSnapAudioCatalog {
	public static final ManagedLoopSound LOBBY = loop("music.lobby", fallback("minecraft:music.menu"), 2182);
	public static final ManagedLoopSound LOBBY2 = loop("music.lobby2", fallback("minecraft:music.menu"), 2182);
	public static final ManagedLoopSound READY = loop("music.ready", fallback("minecraft:music.game"), 4237);
	public static final ManagedLoopSound GAME = loop("music.game", fallback("minecraft:music.dragon"), 4561);
	public static final ManagedCueSound START = cue("music.start", fallback("minecraft:ui.toast.challenge_complete"));
	public static final ManagedCueSound END = cue("music.end", fallback("minecraft:entity.player.levelup"));
	public static final ManagedCueSound SNAP = cue("music.snap", fallback("minecraft:block.note_block.bell"));

	public static final List<ManagedLoopSound> LOBBY_TRACKS = List.of(LOBBY, LOBBY2);
	public static final List<OverlaySound> ALL_OVERLAY_SOUNDS;

	static {
		List<OverlaySound> all = new ArrayList<>();
		all.addAll(LOBBY_TRACKS);
		all.add(READY);
		all.add(GAME);
		all.add(START);
		all.add(END);
		all.add(SNAP);
		ALL_OVERLAY_SOUNDS = List.copyOf(all);
	}

	private MinecraftSnapAudioCatalog() {
	}

	private static ManagedLoopSound loop(String path, SoundEvent fallback, int durationTicks) {
		var id = Identifier.of(MinecraftSnap.MOD_ID, path);
		return new ManagedLoopSound(path, id, fallback, durationTicks);
	}

	private static ManagedCueSound cue(String path, SoundEvent fallback) {
		var id = Identifier.of(MinecraftSnap.MOD_ID, path);
		return new ManagedCueSound(path, id, fallback);
	}

	private static SoundEvent fallback(String id) {
		return SoundEvent.of(Identifier.of(id));
	}

	public sealed interface OverlaySound permits ManagedLoopSound, ManagedCueSound {
		String path();
		Identifier id();
		SoundEvent fallback();
	}

	public record ManagedLoopSound(String path, Identifier id, SoundEvent fallback, int durationTicks) implements OverlaySound {
	}

	public record ManagedCueSound(String path, Identifier id, SoundEvent fallback) implements OverlaySound {
	}
}
