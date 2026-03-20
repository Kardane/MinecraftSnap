package karn.minecraftsnap.game;

import karn.minecraftsnap.config.StatsRepository;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * 경기 종료 시 래더 보상을 계산/적용하는 서비스.
 * MinecraftSnap에서 추출된 순수 로직.
 */
public class LadderRewardService {

	/**
	 * 경기 종료 시 사령관+유닛 전원의 래더 보상을 계산하여 적용한다.
	 */
	public void applyMatchRewards(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository) {
		if (server == null || matchManager == null || statsRepository == null) {
			return;
		}
		var winnerTeam = matchManager.getWinnerTeam();
		if (winnerTeam == null) {
			return;
		}
		var loserTeam = winnerTeam == TeamId.RED ? TeamId.BLUE : TeamId.RED;

		applyCaptainReward(server, matchManager, statsRepository, winnerTeam, loserTeam);
		applyUnitRewards(server, matchManager, statsRepository, winnerTeam, loserTeam);
	}

	private void applyCaptainReward(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId winnerTeam, TeamId loserTeam) {
		int amount = calculateCaptainAmount(matchManager.getRedScore(), matchManager.getBlueScore());
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

	private void applyUnitRewards(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId winnerTeam, TeamId loserTeam) {
		applyTeamUnitLadder(server, matchManager, statsRepository, winnerTeam, 1);
		applyTeamUnitLadder(server, matchManager, statsRepository, loserTeam, -1);
	}

	private void applyTeamUnitLadder(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId teamId, int sign) {
		var unitEntries = matchManager.getPlayerStatesSnapshot().entrySet().stream()
			.filter(entry -> {
				var state = entry.getValue();
				return state.getTeamId() == teamId && state.getRoleType() == RoleType.UNIT;
			})
			.toList();
		
		for (var entry : unitEntries) {
			var playerId = entry.getKey();
			var state = entry.getValue();
			int amount = calculateUnitAmount(state.getMatchKills(), state.getMatchCaptureScore());
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
		return Math.abs(redScore - blueScore) + 30;
	}

	static int calculateUnitAmount(int kills, int captureScore) {
		return Math.round(kills + (float) captureScore / 12.0f + 5);
	}
}
