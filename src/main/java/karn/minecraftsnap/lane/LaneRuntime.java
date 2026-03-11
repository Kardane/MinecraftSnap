package karn.minecraftsnap.lane;

import karn.minecraftsnap.biome.BiomeEffect;
import karn.minecraftsnap.config.BiomeEntry;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.CaptureOwner;
import karn.minecraftsnap.game.LaneId;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LaneRuntime {
	private final LaneId laneId;
	private final List<UUID> aliveUnitPlayerIds = new ArrayList<>();
	private final List<UUID> aliveCaptureUnitPlayerIds = new ArrayList<>();
	private SystemConfig.LaneRegionConfig laneRegion;
	private SystemConfig.CaptureRegionConfig captureRegion;
	private BiomeEntry biomeEntry;
	private BiomeEffect biomeEffect;
	private CaptureOwner captureOwner = CaptureOwner.NEUTRAL;
	private LaneCaptureStatus captureStatus = LaneCaptureStatus.IDLE;
	private int redCaptureScore;
	private int blueCaptureScore;
	private int revealElapsedSeconds = -1;

	public LaneRuntime(LaneId laneId) {
		this.laneId = laneId;
	}

	public LaneId laneId() {
		return laneId;
	}

	public SystemConfig.LaneRegionConfig laneRegion() {
		return laneRegion;
	}

	public SystemConfig.CaptureRegionConfig captureRegion() {
		return captureRegion;
	}

	public List<UUID> aliveUnitPlayerIds() {
		return List.copyOf(aliveUnitPlayerIds);
	}

	public List<UUID> aliveCaptureUnitPlayerIds() {
		return List.copyOf(aliveCaptureUnitPlayerIds);
	}

	public BiomeEntry biomeEntry() {
		return biomeEntry;
	}

	public BiomeEffect biomeEffect() {
		return biomeEffect;
	}

	public CaptureOwner captureOwner() {
		return captureOwner;
	}

	public LaneCaptureStatus captureStatus() {
		return captureStatus;
	}

	public int redCaptureScore() {
		return redCaptureScore;
	}

	public int blueCaptureScore() {
		return blueCaptureScore;
	}

	public int revealElapsedSeconds() {
		return revealElapsedSeconds;
	}

	public List<ServerPlayerEntity> resolveAliveUnitPlayers(MinecraftServer server) {
		return resolvePlayers(server, aliveUnitPlayerIds);
	}

	public List<ServerPlayerEntity> resolveAliveCaptureUnitPlayers(MinecraftServer server) {
		return resolvePlayers(server, aliveCaptureUnitPlayerIds);
	}

	public boolean hasActiveBiome() {
		return biomeEntry != null && biomeEffect != null;
	}

	public void updateRegions(SystemConfig.LaneRegionConfig laneRegion, SystemConfig.CaptureRegionConfig captureRegion) {
		this.laneRegion = laneRegion;
		this.captureRegion = captureRegion;
	}

	public void updateOccupants(List<UUID> laneUnits, List<UUID> captureUnits) {
		aliveUnitPlayerIds.clear();
		aliveUnitPlayerIds.addAll(laneUnits);
		aliveCaptureUnitPlayerIds.clear();
		aliveCaptureUnitPlayerIds.addAll(captureUnits);
	}

	public void updateCapture(CaptureOwner captureOwner, LaneCaptureStatus captureStatus, int redCaptureScore, int blueCaptureScore) {
		this.captureOwner = captureOwner;
		this.captureStatus = captureStatus;
		this.redCaptureScore = redCaptureScore;
		this.blueCaptureScore = blueCaptureScore;
	}

	public void revealBiome(BiomeEntry biomeEntry, BiomeEffect biomeEffect, int revealElapsedSeconds) {
		this.biomeEntry = biomeEntry;
		this.biomeEffect = biomeEffect;
		this.revealElapsedSeconds = revealElapsedSeconds;
	}

	public void clearBiome() {
		biomeEntry = null;
		biomeEffect = null;
		revealElapsedSeconds = -1;
	}

	private List<ServerPlayerEntity> resolvePlayers(MinecraftServer server, List<UUID> playerIds) {
		if (server == null || playerIds.isEmpty()) {
			return List.of();
		}
		return playerIds.stream()
			.map(playerId -> server.getPlayerManager().getPlayer(playerId))
			.filter(java.util.Objects::nonNull)
			.toList();
	}
}
