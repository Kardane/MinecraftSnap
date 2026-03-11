package karn.minecraftsnap.biome;

import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.lane.LaneRuntime;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public record BiomeRuntimeContext(
	MinecraftServer server,
	ServerWorld world,
	LaneRuntime laneRuntime,
	BiomeEntry biomeEntry,
	TextTemplateResolver textTemplateResolver,
	long serverTicks,
	int elapsedGameSeconds
) {
	public List<ServerPlayerEntity> aliveUnitPlayers() {
		return laneRuntime.resolveAliveUnitPlayers(server);
	}

	public List<ServerPlayerEntity> aliveCaptureUnitPlayers() {
		return laneRuntime.resolveAliveCaptureUnitPlayers(server);
	}

	public String weather() {
		if (world == null) {
			return "clear";
		}
		if (world.isThundering()) {
			return "thunder";
		}
		if (world.isRaining()) {
			return "rain";
		}
		return "clear";
	}

	public int secondsSinceReveal() {
		return Math.max(0, elapsedGameSeconds - laneRuntime.revealElapsedSeconds());
	}

	public String laneLabel() {
		return switch (laneRuntime.laneId()) {
			case LANE_1 -> "1번";
			case LANE_2 -> "2번";
			case LANE_3 -> "3번";
		};
	}

	public void broadcast(List<String> lines) {
		if (server == null || lines == null || lines.isEmpty()) {
			return;
		}
		var placeholders = Map.of(
			"{lane}", laneLabel(),
			"{biome}", biomeEntry.displayName
		);
		for (var line : lines) {
			server.getPlayerManager().broadcast(textTemplateResolver.format(line, placeholders), false);
		}
	}

	public void playSound(String soundId) {
		if (server == null || soundId == null || soundId.isBlank()) {
			return;
		}
		var identifier = Identifier.tryParse(soundId);
		if (identifier == null) {
			return;
		}
		var soundEvent = SoundEvent.of(identifier);
		for (var player : server.getPlayerManager().getPlayerList()) {
			player.playSoundToPlayer(soundEvent, SoundCategory.MASTER, 1.0f, 1.0f);
		}
	}
}
