package karn.minecraftsnap.audio;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public class UiSoundService {
	public void playUiClick(ServerPlayerEntity player) {
		playToPlayer(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.0f);
	}

	public void playUiConfirm(ServerPlayerEntity player) {
		playToPlayer(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8f, 1.2f);
	}

	public void playUiDeny(ServerPlayerEntity player) {
		playToPlayer(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.9f, 0.8f);
	}

	public void playGlobalAnnouncement(MinecraftServer server) {
		playToAll(server, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.8f, 1.0f);
	}

	public void playCountdownTick(MinecraftServer server, boolean urgent) {
		playToAll(server, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8f, urgent ? 1.3f : 1.0f);
	}

	public void playEventSuccess(MinecraftServer server) {
		playToAll(server, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.9f, 1.15f);
	}

	public void playEventWarning(MinecraftServer server) {
		playToAll(server, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.9f, 0.85f);
	}

	public void playEventSuccess(ServerPlayerEntity player) {
		playToPlayer(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.9f, 1.15f);
	}

	public void playEventWarning(ServerPlayerEntity player) {
		playToPlayer(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.9f, 0.85f);
	}

	protected void playToAll(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
		if (server == null || sound == null) {
			return;
		}
		for (var player : server.getPlayerManager().getPlayerList()) {
			playToPlayer(player, sound, volume, pitch);
		}
	}

	protected void playToPlayer(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
		if (player == null || sound == null) {
			return;
		}
		player.playSoundToPlayer(sound, SoundCategory.PLAYERS, volume, pitch);
	}
}
