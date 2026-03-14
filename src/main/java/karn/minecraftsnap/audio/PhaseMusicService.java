package karn.minecraftsnap.audio;

import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

import java.util.Random;

public class PhaseMusicService {
	private final MatchManager matchManager;
	private final Random random;
	private MatchPhase lastPhase = null;
	private long lastLoopTick = Long.MIN_VALUE;
	private int activeLobbyTrackIndex = -1;
	private MinecraftSnapAudioCatalog.ManagedLoopSound activeLoopSound = null;

	public PhaseMusicService(MatchManager matchManager) {
		this(matchManager, new Random());
	}

	PhaseMusicService(MatchManager matchManager, Random random) {
		this.matchManager = matchManager;
		this.random = random;
	}

	public void tick(MinecraftServer server) {
		var currentPhase = matchManager.getPhase();
		if (currentPhase != lastPhase) {
			stopAllManagedSounds(server);
			if (shouldPlayStartCue(lastPhase, currentPhase)) {
				playCue(server, MinecraftSnapAudioCatalog.START);
			}
			if (shouldPlayEndCue(lastPhase, currentPhase)) {
				playCue(server, MinecraftSnapAudioCatalog.END);
			}
			lastLoopTick = Long.MIN_VALUE;
			activeLoopSound = null;
			if (phaseMode(currentPhase) != PhaseMode.LOBBY) {
				activeLobbyTrackIndex = -1;
			}
			lastPhase = currentPhase;
		}

		var mode = phaseMode(currentPhase);
		if (mode == PhaseMode.NONE || server.getPlayerManager().getPlayerList().isEmpty()) {
			return;
		}

		var currentTick = matchManager.getServerTicks();
		if (lastLoopTick != Long.MIN_VALUE && activeLoopSound != null && currentTick - lastLoopTick < activeLoopSound.durationTicks()) {
			return;
		}

		activeLoopSound = switch (mode) {
			case LOBBY -> {
				activeLobbyTrackIndex = nextLobbyTrackIndex(activeLobbyTrackIndex, MinecraftSnapAudioCatalog.LOBBY_TRACKS.size(), random);
				yield MinecraftSnapAudioCatalog.LOBBY_TRACKS.get(activeLobbyTrackIndex);
			}
			case READY -> MinecraftSnapAudioCatalog.READY;
			case GAME -> MinecraftSnapAudioCatalog.GAME;
			case NONE -> null;
		};
		if (activeLoopSound == null) {
			return;
		}
		playLoop(server, activeLoopSound);
		lastLoopTick = currentTick;
	}

	public void handleJoin(ServerPlayerEntity player) {
		var mode = phaseMode(matchManager.getPhase());
		if (mode == PhaseMode.NONE) {
			return;
		}
		var loopSound = currentLoopForJoin(mode);
		if (loopSound != null) {
			playSound(player, loopSound.id());
		}
	}

	static PhaseMode phaseMode(MatchPhase phase) {
		if (phase == MatchPhase.LOBBY) {
			return PhaseMode.LOBBY;
		}
		if (phase == MatchPhase.TEAM_SELECT || phase == MatchPhase.FACTION_SELECT) {
			return PhaseMode.READY;
		}
		if (phase == MatchPhase.GAME_RUNNING) {
			return PhaseMode.GAME;
		}
		return PhaseMode.NONE;
	}

	static boolean shouldPlayStartCue(MatchPhase previousPhase, MatchPhase currentPhase) {
		return previousPhase == MatchPhase.GAME_START && currentPhase == MatchPhase.GAME_RUNNING;
	}

	static boolean shouldPlayEndCue(MatchPhase previousPhase, MatchPhase currentPhase) {
		return previousPhase != MatchPhase.GAME_END && currentPhase == MatchPhase.GAME_END;
	}

	static int nextLobbyTrackIndex(int previousIndex, int trackCount, Random random) {
		if (trackCount <= 1) {
			return 0;
		}
		int next = random.nextInt(trackCount);
		if (next == previousIndex) {
			next = (next + 1 + random.nextInt(trackCount - 1)) % trackCount;
		}
		return next;
	}

	private MinecraftSnapAudioCatalog.ManagedLoopSound currentLoopForJoin(PhaseMode mode) {
		return switch (mode) {
			case LOBBY -> activeLoopSound != null ? activeLoopSound : MinecraftSnapAudioCatalog.LOBBY_TRACKS.getFirst();
			case READY -> MinecraftSnapAudioCatalog.READY;
			case GAME -> MinecraftSnapAudioCatalog.GAME;
			case NONE -> null;
		};
	}

	private void playLoop(MinecraftServer server, MinecraftSnapAudioCatalog.ManagedLoopSound sound) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			playSound(player, sound.id());
		}
	}

	private void playCue(MinecraftServer server, MinecraftSnapAudioCatalog.ManagedCueSound sound) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			playSound(player, sound.id());
		}
	}

	private void stopAllManagedSounds(MinecraftServer server) {
		for (var overlay : MinecraftSnapAudioCatalog.ALL_OVERLAY_SOUNDS) {
			stopSound(server, overlay.id());
		}
	}

	private void stopSound(MinecraftServer server, net.minecraft.util.Identifier soundId) {
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new StopSoundS2CPacket(soundId, SoundCategory.VOICE));
		}
	}

	private void playSound(ServerPlayerEntity player, net.minecraft.util.Identifier soundId) {
		player.playSoundToPlayer(net.minecraft.sound.SoundEvent.of(soundId), SoundCategory.VOICE, 0.5F, 1.0F);
	}

	enum PhaseMode {
		NONE,
		LOBBY,
		READY,
		GAME
	}
}
