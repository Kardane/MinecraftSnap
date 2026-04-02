package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.StatsRepository;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 경기 종료 시 래더 보상을 계산/적용하는 서비스.
 * MinecraftSnap에서 추출된 순수 로직.
 */
public class LadderRewardService {
	static final int MAX_MATCH_LADDER_DELTA = 100;

	/**
	 * 경기 종료 시 사령관+유닛 전원의 래더 보상을 계산하여 적용한다.
	 */
	public void applyMatchRewards(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, SystemConfig systemConfig) {
		if (server == null || matchManager == null || statsRepository == null) {
			return;
		}
		var ladderRewardConfig = systemConfig == null || systemConfig.ladderReward == null
			? new SystemConfig.LadderRewardConfig()
			: systemConfig.ladderReward;
		var winnerTeam = matchManager.getWinnerTeam();
		if (winnerTeam == null) {
			return;
		}
		var loserTeam = winnerTeam == TeamId.RED ? TeamId.BLUE : TeamId.RED;

		applyMatchResults(server, matchManager, statsRepository, winnerTeam, true);
		applyMatchResults(server, matchManager, statsRepository, loserTeam, false);
		applyCaptainReward(server, matchManager, statsRepository, winnerTeam, loserTeam, ladderRewardConfig);
		applyUnitRewards(server, matchManager, statsRepository, winnerTeam, loserTeam, ladderRewardConfig);
	}

	private void applyMatchResults(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId teamId, boolean winner) {
		for (var entry : matchManager.getPlayerStatesSnapshot().entrySet()) {
			var state = entry.getValue();
			if (state.getTeamId() != teamId || state.getRoleType() == RoleType.NONE || state.getRoleType() == RoleType.SPECTATOR) {
				continue;
			}
			var playerId = entry.getKey();
			var name = resolvePlayerName(server, statsRepository, playerId);
			if (winner) {
				statsRepository.addWin(playerId, name, 1);
			} else {
				statsRepository.addLoss(playerId, name, 1);
			}
		}
	}

	private void applyCaptainReward(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId winnerTeam, TeamId loserTeam, SystemConfig.LadderRewardConfig config) {
		int amount = calculateCaptainAmount(matchManager.getRedScore(), matchManager.getBlueScore(), config);
		var winnerCaptainId = matchManager.getCaptainId(winnerTeam);
		var loserCaptainId = matchManager.getCaptainId(loserTeam);

		if (winnerCaptainId != null) {
			var name = resolvePlayerName(server, winnerCaptainId);
			statsRepository.addLadder(winnerCaptainId, name, amount);
		}
		if (loserCaptainId != null) {
			var name = resolvePlayerName(server, loserCaptainId);
			statsRepository.addLadder(loserCaptainId, name, -amount);
		}
	}

	private void applyUnitRewards(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId winnerTeam, TeamId loserTeam, SystemConfig.LadderRewardConfig config) {
		applyTeamUnitLadder(server, matchManager, statsRepository, winnerTeam, 1, config);
		applyTeamUnitLadder(server, matchManager, statsRepository, loserTeam, -1, config);
	}

	private void applyTeamUnitLadder(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId teamId, int sign, SystemConfig.LadderRewardConfig config) {
		var unitEntries = matchManager.getPlayerStatesSnapshot().entrySet().stream()
			.filter(entry -> {
				var state = entry.getValue();
				return state.getTeamId() == teamId && state.getRoleType() == RoleType.UNIT;
			})
			.toList();
		
		for (var entry : unitEntries) {
			var playerId = entry.getKey();
			var state = entry.getValue();
			int amount = calculateUnitAmount(state.getMatchKills(), state.getMatchCaptureScore(), config);
			var name = resolvePlayerName(server, statsRepository, playerId);
			statsRepository.addLadder(playerId, name, amount * sign);
		}
	}

	private String resolvePlayerName(MinecraftServer server, UUID playerId) {
		var player = server.getPlayerManager().getPlayer(playerId);
		return player == null ? playerId.toString() : player.getName().getString();
	}

	private String resolvePlayerName(MinecraftServer server, StatsRepository statsRepository, UUID playerId) {
		var player = server.getPlayerManager().getPlayer(playerId);
		return player == null
			? statsRepository.getLastKnownName(playerId, playerId.toString())
			: player.getName().getString();
	}

	static int calculateCaptainAmount(int redScore, int blueScore) {
		return calculateCaptainAmount(redScore, blueScore, new SystemConfig.LadderRewardConfig());
	}

	static int calculateUnitAmount(int kills, int captureScore) {
		return calculateUnitAmount(kills, captureScore, new SystemConfig.LadderRewardConfig());
	}

	static int calculateCaptainAmount(int redScore, int blueScore, SystemConfig.LadderRewardConfig config) {
		return clampMatchLadderDelta(Math.round(Math.abs(redScore - blueScore) * config.captainScoreGapMultiplier + config.captainBase));
	}

	static int calculateUnitAmount(int kills, int captureScore, SystemConfig.LadderRewardConfig config) {
		return clampMatchLadderDelta(Math.round(kills * config.unitKillWeight + (float) captureScore / config.unitCaptureScoreDivisor + config.unitBase));
	}

	private static int clampMatchLadderDelta(int amount) {
		return Math.min(MAX_MATCH_LADDER_DELTA, Math.max(0, amount));
	}
}
