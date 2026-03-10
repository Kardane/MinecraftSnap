package karn.minecraftsnap.audio;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class PhaseMusicService {
	private final MatchManager matchManager;
	private MatchPhase lastPhase = null;
	private long lastLoopTick = Long.MIN_VALUE;

	public PhaseMusicService(MatchManager matchManager) {
		this.matchManager = matchManager;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		var currentPhase = matchManager.getPhase();
		if (currentPhase != lastPhase) {
			stopPreviousTrack(server, systemConfig);
			lastLoopTick = Long.MIN_VALUE;
			lastPhase = currentPhase;
		}

		var config = getConfigForPhase(systemConfig, currentPhase);
		if (config == null || !config.enabled) {
			return;
		}

		var currentTick = matchManager.getServerTicks();
		if (lastLoopTick == Long.MIN_VALUE || currentTick - lastLoopTick >= config.intervalTicks) {
			playTrack(server, config);
			lastLoopTick = currentTick;
		}
	}

	public void handleJoin(ServerPlayerEntity player, SystemConfig systemConfig) {
		var config = getConfigForPhase(systemConfig, matchManager.getPhase());
		if (config == null || !config.enabled) {
			return;
		}

		playTrack(player, config);
	}

	private void stopPreviousTrack(MinecraftServer server, SystemConfig systemConfig) {
		stopTrack(server, systemConfig.lobbyMusic);
		stopTrack(server, systemConfig.gameMusic);
	}

	private void stopTrack(MinecraftServer server, SystemConfig.MusicLoopConfig config) {
		var soundId = Identifier.tryParse(config.soundId);
		if (soundId == null) {
			return;
		}

		var category = parseCategory(config.category);
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.networkHandler.sendPacket(new StopSoundS2CPacket(soundId, category));
		}
	}

	private void playTrack(MinecraftServer server, SystemConfig.MusicLoopConfig config) {
		var soundId = Identifier.tryParse(config.soundId);
		if (soundId == null) {
			return;
		}

		for (var player : server.getPlayerManager().getPlayerList()) {
			playTrack(player, config);
		}
	}

	private void playTrack(ServerPlayerEntity player, SystemConfig.MusicLoopConfig config) {
		var soundId = Identifier.tryParse(config.soundId);
		if (soundId == null) {
			return;
		}

		var soundEvent = SoundEvent.of(soundId);
		var category = parseCategory(config.category);
		player.playSoundToPlayer(soundEvent, category, config.volume, config.pitch);
	}

	private SystemConfig.MusicLoopConfig getConfigForPhase(SystemConfig systemConfig, MatchPhase phase) {
		if (phase == MatchPhase.LOBBY || phase == MatchPhase.TEAM_SELECT || phase == MatchPhase.FACTION_SELECT) {
			return systemConfig.lobbyMusic;
		}

		if (phase == MatchPhase.GAME_RUNNING) {
			return systemConfig.gameMusic;
		}

		return null;
	}

	private SoundCategory parseCategory(String category) {
		try {
			return SoundCategory.valueOf(category.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return SoundCategory.MUSIC;
		}
	}
}
