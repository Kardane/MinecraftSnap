package karn.minecraftsnap.ui;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.game.CaptureOwner;
import karn.minecraftsnap.game.CapturePointService;
import karn.minecraftsnap.game.CapturePointState;
import karn.minecraftsnap.game.CaptureProgress;
import karn.minecraftsnap.game.LaneId;
import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.MatchPhase;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaptureHudService {
	private final MatchManager matchManager;
	private final TextTemplateResolver textTemplateResolver;
	private final Map<UUID, ServerBossBar> bossBars = new HashMap<>();

	public CaptureHudService(MatchManager matchManager, TextTemplateResolver textTemplateResolver) {
		this.matchManager = matchManager;
		this.textTemplateResolver = textTemplateResolver;
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig, CapturePointService capturePointService) {
		if (server == null || capturePointService == null || matchManager.getPhase() != MatchPhase.GAME_RUNNING) {
			clearAll();
			return;
		}

		var activePlayers = new java.util.HashSet<UUID>();
		for (var player : server.getPlayerManager().getPlayerList()) {
			var laneId = findCaptureLane(player, systemConfig);
			if (laneId == null) {
				remove(player.getUuid());
				continue;
			}
			var state = capturePointService.getState(laneId);
			if (!shouldShow(state)) {
				remove(player.getUuid());
				continue;
			}
			activePlayers.add(player.getUuid());
			var bossBar = bossBars.computeIfAbsent(player.getUuid(), ignored -> new ServerBossBar(
				textTemplateResolver.format("&f점령"),
				BossBar.Color.WHITE,
				BossBar.Style.PROGRESS
			));
			if (!bossBar.getPlayers().contains(player)) {
				bossBar.addPlayer(player);
			}
			bossBar.setVisible(true);
			bossBar.setName(textTemplateResolver.format(formatText(state, systemConfig.capture)));
			bossBar.setPercent(progressPercent(state, systemConfig.capture));
			bossBar.setColor(colorOf(state));
		}
		for (var playerId : java.util.List.copyOf(bossBars.keySet())) {
			if (!activePlayers.contains(playerId)) {
				remove(playerId);
			}
		}
	}

	static String formatText(CapturePointState state, SystemConfig.CaptureConfig captureConfig) {
		if (state == null) {
			return captureConfig == null ? "&f점령" : captureConfig.defaultBossBarText;
		}
		if (state.getProgress().isContested()) {
			return captureConfig.contestedBossBarText;
		}
		var owner = switch (state.getOwner()) {
			case RED -> captureConfig.redOwnerBossBarText;
			case BLUE -> captureConfig.blueOwnerBossBarText;
			case NEUTRAL -> captureConfig.neutralOwnerBossBarText;
		};
		var seconds = state.getProgress().getSeconds();
		if (seconds <= 0 || state.getProgress().getTeamId() == null) {
			return "&f" + owner;
		}
		var requiredSeconds = state.requiredCaptureSeconds(captureConfig.captureStepSeconds, captureConfig.firstCaptureStepSeconds);
		return captureConfig.captureProgressBossBarTemplate
			.replace("{owner}", owner)
			.replace("{current}", Integer.toString(Math.min(seconds, requiredSeconds)))
			.replace("{required}", Integer.toString(requiredSeconds));
	}

	static float progressPercent(CapturePointState state, SystemConfig.CaptureConfig captureConfig) {
		if (state == null || captureConfig == null) {
			return 1.0f;
		}
		var progress = state.getProgress();
		var requiredSeconds = state.requiredCaptureSeconds(captureConfig.captureStepSeconds, captureConfig.firstCaptureStepSeconds);
		if (progress == null || progress.isContested() || requiredSeconds <= 0) {
			return 1.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, progress.getTicks() / (float) (requiredSeconds * 20)));
	}

	static BossBar.Color colorOf(CapturePointState state) {
		if (state == null || state.getProgress().isContested()) {
			return BossBar.Color.YELLOW;
		}
		if (state.getProgress().getTeamId() != null) {
			return state.getProgress().getTeamId() == karn.minecraftsnap.game.TeamId.RED ? BossBar.Color.RED : BossBar.Color.BLUE;
		}
		return switch (state.getOwner()) {
			case RED -> BossBar.Color.RED;
			case BLUE -> BossBar.Color.BLUE;
			case NEUTRAL -> BossBar.Color.WHITE;
		};
	}

	private boolean shouldShow(CapturePointState state) {
		return state != null;
	}

	private LaneId findCaptureLane(ServerPlayerEntity player, SystemConfig systemConfig) {
		if (player == null || systemConfig == null) {
			return null;
		}
		if (contains(systemConfig.inGame.lane1Region, player.getPos())) {
			return LaneId.LANE_1;
		}
		if (contains(systemConfig.inGame.lane2Region, player.getPos())) {
			return LaneId.LANE_2;
		}
		if (contains(systemConfig.inGame.lane3Region, player.getPos())) {
			return LaneId.LANE_3;
		}
		return null;
	}

	private boolean contains(SystemConfig.LaneRegionConfig region, net.minecraft.util.math.Vec3d pos) {
		return region != null
			&& pos != null
			&& pos.x >= Math.min(region.minX, region.maxX)
			&& pos.x <= Math.max(region.minX, region.maxX)
			&& pos.y >= Math.min(region.minY, region.maxY)
			&& pos.y <= Math.max(region.minY, region.maxY)
			&& pos.z >= Math.min(region.minZ, region.maxZ)
			&& pos.z <= Math.max(region.minZ, region.maxZ);
	}

	private void remove(UUID playerId) {
		var bossBar = bossBars.remove(playerId);
		if (bossBar != null) {
			bossBar.clearPlayers();
			bossBar.setVisible(false);
		}
	}

	private void clearAll() {
		for (var playerId : java.util.List.copyOf(bossBars.keySet())) {
			remove(playerId);
		}
	}
}
