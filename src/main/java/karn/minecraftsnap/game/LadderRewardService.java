package karn.minecraftsnap.game;

import karn.minecraftsnap.config.ServerStatsRepository;
import karn.minecraftsnap.config.StatsRepository;
import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
		var tuning = createRewardTuning(matchManager, ladderRewardConfig);

		applyMatchResults(server, matchManager, statsRepository, winnerTeam, true);
		applyMatchResults(server, matchManager, statsRepository, loserTeam, false);
		applyCaptainReward(server, matchManager, statsRepository, winnerTeam, loserTeam, tuning.finalCaptainDelta());
		applyUnitRewards(server, matchManager, statsRepository, winnerTeam, loserTeam, ladderRewardConfig, tuning);
	}

	public void applyServerMatchStats(MatchManager matchManager, ServerStatsRepository serverStatsRepository) {
		if (matchManager == null || serverStatsRepository == null) {
			return;
		}
		var winnerTeam = matchManager.getWinnerTeam();
		if (winnerTeam == null) {
			return;
		}
		for (var teamId : TeamId.values()) {
			var factionId = matchManager.getFactionSelection(teamId);
			if (factionId != null) {
				serverStatsRepository.recordFactionGame(factionId, teamId == winnerTeam);
			}
		}
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

	private void applyCaptainReward(MinecraftServer server, MatchManager matchManager, StatsRepository statsRepository, TeamId winnerTeam, TeamId loserTeam, int amount) {
		if (amount <= 0) {
			return;
		}
		var winnerCaptainId = matchManager.getCaptainId(winnerTeam);
		var loserCaptainId = matchManager.getCaptainId(loserTeam);
		if (winnerCaptainId == null || loserCaptainId == null) {
			return;
		}

		var winnerName = resolvePlayerName(server, winnerCaptainId);
		statsRepository.addLadder(winnerCaptainId, winnerName, amount);

		var loserName = resolvePlayerName(server, loserCaptainId);
		statsRepository.addLadder(loserCaptainId, loserName, -amount);
	}

	private void applyUnitRewards(
		MinecraftServer server,
		MatchManager matchManager,
		StatsRepository statsRepository,
		TeamId winnerTeam,
		TeamId loserTeam,
		SystemConfig.LadderRewardConfig config,
		RewardTuning tuning
	) {
		applyTeamUnitLadder(server, matchManager, statsRepository, winnerTeam, config, tuning, true);
		applyTeamUnitLadder(server, matchManager, statsRepository, loserTeam, config, tuning, false);
	}

	private void applyTeamUnitLadder(
		MinecraftServer server,
		MatchManager matchManager,
		StatsRepository statsRepository,
		TeamId teamId,
		SystemConfig.LadderRewardConfig config,
		RewardTuning tuning,
		boolean winner
	) {
		var participants = collectUnitParticipants(matchManager, teamId);
		if (participants.isEmpty()) {
			return;
		}
		int teamDelta = calculateUnitTeamFinalDelta(participants.size(), tuning.swingBonus(), tuning.stakeMultiplier(), config);
		if (teamDelta <= 0) {
			return;
		}

		var distributed = winner
			? distributeWinnerUnitDelta(participants, teamDelta, config)
			: distributeLoserUnitDelta(participants, teamDelta, config);

		for (var participant : participants) {
			int delta = distributed.getOrDefault(participant.playerId(), 0);
			if (delta == 0) {
				continue;
			}
			var name = resolvePlayerName(server, statsRepository, participant.playerId());
			statsRepository.addLadder(participant.playerId(), name, delta);
		}
	}

	private List<RewardParticipant> collectUnitParticipants(MatchManager matchManager, TeamId teamId) {
		return matchManager.getPlayerStatesSnapshot().entrySet().stream()
			.filter(entry -> {
				var state = entry.getValue();
				return state.getTeamId() == teamId && state.getRoleType() == RoleType.UNIT;
			})
			.sorted(java.util.Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
			.map(entry -> new RewardParticipant(entry.getKey(), entry.getValue()))
			.toList();
	}

	private RewardTuning createRewardTuning(MatchManager matchManager, SystemConfig.LadderRewardConfig config) {
		int swingBonus = countRevealedExtraLanes(matchManager) * config.laneRevealBonus;
		float stakeMultiplier = resolveStakeMultiplier(matchManager.getSnapCount(), config);
		int captainBaseDelta = calculateCaptainBaseDelta(matchManager.getRedScore(), matchManager.getBlueScore(), swingBonus, config);
		int finalCaptainDelta = Math.max(0, Math.round(captainBaseDelta * stakeMultiplier));
		return new RewardTuning(swingBonus, stakeMultiplier, finalCaptainDelta);
	}

	private int countRevealedExtraLanes(MatchManager matchManager) {
		int count = 0;
		if (matchManager.isLaneRevealed(LaneId.LANE_2)) {
			count++;
		}
		if (matchManager.isLaneRevealed(LaneId.LANE_3)) {
			count++;
		}
		return count;
	}

	static int calculateCaptainAmount(int redScore, int blueScore) {
		return calculateCaptainAmount(redScore, blueScore, new SystemConfig.LadderRewardConfig());
	}

	static int calculateUnitAmount(int kills, int captureScore) {
		return calculateUnitAmount(kills, captureScore, new SystemConfig.LadderRewardConfig());
	}

	static int calculateCaptainAmount(int redScore, int blueScore, SystemConfig.LadderRewardConfig config) {
		return calculateCaptainBaseDelta(redScore, blueScore, 0, config);
	}

	static int calculateUnitAmount(int kills, int captureScore, SystemConfig.LadderRewardConfig config) {
		return Math.max(0, Math.round(calculateUnitPerformance(kills, captureScore, config)));
	}

	static int calculateWinningUnitAmount(int kills, int captureScore, SystemConfig.LadderRewardConfig config) {
		return Math.max(1, Math.round(1.0f + calculateUnitPerformance(kills, captureScore, config)));
	}

	static int calculateLosingUnitAmount(int kills, int captureScore, SystemConfig.LadderRewardConfig config) {
		return Math.max(1, Math.round(Math.max(config.lossWeightMin, config.lossWeightBase - calculateUnitPerformance(kills, captureScore, config))));
	}

	static int adjustMatchLadderDeltaForEarlySurrender(int amount, int elapsedSeconds, boolean surrendered) {
		return Math.max(0, amount);
	}

	private static int calculateCaptainBaseDelta(int redScore, int blueScore, int swingBonus, SystemConfig.LadderRewardConfig config) {
		int scoreGap = Math.abs(redScore - blueScore);
		int gapBonus = config.captainScoreGapDivisor <= 0 ? 0 : scoreGap / config.captainScoreGapDivisor;
		int pooled = config.captainBase + gapBonus;
		int clamped = Math.max(config.captainBase, Math.min(config.captainMax, pooled));
		return Math.min(MAX_MATCH_LADDER_DELTA, Math.max(0, clamped + swingBonus));
	}

	private int calculateUnitTeamFinalDelta(int teamUnitCount, int swingBonus, float stakeMultiplier, SystemConfig.LadderRewardConfig config) {
		int basePool = Math.max(0, Math.round(teamUnitCount * config.unitPoolPerPlayer));
		int baseDelta = Math.max(0, basePool + swingBonus);
		return Math.max(0, Math.round(baseDelta * stakeMultiplier));
	}

	private LinkedHashMap<UUID, Integer> distributeWinnerUnitDelta(List<RewardParticipant> participants, int totalDelta, SystemConfig.LadderRewardConfig config) {
		return distributeTeamPool(
			participants,
			totalDelta,
			config.unitEqualShareRatio,
			participant -> 1.0d + calculateUnitPerformance(participant.state().getMatchKills(), participant.state().getMatchCaptureScore(), config),
			false
		);
	}

	private LinkedHashMap<UUID, Integer> distributeLoserUnitDelta(List<RewardParticipant> participants, int totalDelta, SystemConfig.LadderRewardConfig config) {
		return distributeTeamPool(
			participants,
			totalDelta,
			config.unitEqualShareRatio,
			participant -> Math.max(config.lossWeightMin, config.lossWeightBase - calculateUnitPerformance(participant.state().getMatchKills(), participant.state().getMatchCaptureScore(), config)),
			true
		);
	}

	private LinkedHashMap<UUID, Integer> distributeTeamPool(
		List<RewardParticipant> participants,
		int totalDelta,
		float equalShareRatio,
		java.util.function.ToDoubleFunction<RewardParticipant> weightProvider,
		boolean negative
	) {
		var result = new LinkedHashMap<UUID, Integer>();
		if (participants.isEmpty() || totalDelta <= 0) {
			return result;
		}

		int equalPool = Math.max(0, Math.min(totalDelta, (int) Math.floor(totalDelta * equalShareRatio)));
		int weightedPool = totalDelta - equalPool;
		double equalShare = (double) equalPool / participants.size();
		double totalWeight = participants.stream()
			.mapToDouble(weightProvider)
			.map(weight -> Math.max(0.0d, weight))
			.sum();

		var allocations = new ArrayList<ShareAllocation>(participants.size());
		int assigned = 0;
		for (var participant : participants) {
			double weight = Math.max(0.0d, weightProvider.applyAsDouble(participant));
			double weightedShare = 0.0d;
			if (weightedPool > 0) {
				weightedShare = totalWeight > 0.0d
					? weightedPool * weight / totalWeight
					: (double) weightedPool / participants.size();
			}
			double exactShare = equalShare + weightedShare;
			int floored = (int) Math.floor(exactShare);
			allocations.add(new ShareAllocation(participant.playerId(), exactShare - floored, floored));
			assigned += floored;
		}

		allocations.sort(Comparator
			.comparingDouble(ShareAllocation::remainder)
			.reversed()
			.thenComparing(allocation -> allocation.playerId().toString()));

		int remainder = totalDelta - assigned;
		for (int index = 0; index < remainder && index < allocations.size(); index++) {
			allocations.get(index).allocated += 1;
		}

		var allocatedByPlayer = new LinkedHashMap<UUID, Integer>();
		for (var allocation : allocations) {
			allocatedByPlayer.put(allocation.playerId(), negative ? -allocation.allocated : allocation.allocated);
		}
		for (var participant : participants) {
			result.put(participant.playerId(), allocatedByPlayer.getOrDefault(participant.playerId(), 0));
		}
		return result;
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

	private static float calculateUnitPerformance(int kills, int captureScore, SystemConfig.LadderRewardConfig config) {
		return kills * config.unitKillWeight + (float) captureScore / config.unitCaptureScoreDivisor;
	}

	private static float resolveStakeMultiplier(int snapCount, SystemConfig.LadderRewardConfig config) {
		if (snapCount <= 0) {
			return 1.0f;
		}
		if (snapCount == 1) {
			return config.firstSnapMultiplier;
		}
		return config.doubleSnapMultiplier;
	}

	private record RewardParticipant(UUID playerId, PlayerMatchState state) {
	}

	private record RewardTuning(int swingBonus, float stakeMultiplier, int finalCaptainDelta) {
	}

	private static final class ShareAllocation {
		private final UUID playerId;
		private final double remainder;
		private int allocated;

		private ShareAllocation(UUID playerId, double remainder, int allocated) {
			this.playerId = playerId;
			this.remainder = remainder;
			this.allocated = allocated;
		}

		private UUID playerId() {
			return playerId;
		}

		private double remainder() {
			return remainder;
		}
	}
}
