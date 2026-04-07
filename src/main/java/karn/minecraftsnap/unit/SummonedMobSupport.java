package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.MatchManager;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.VanillaPlayerTeamService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public final class SummonedMobSupport {
	private static final VanillaPlayerTeamService PLAYER_TEAM_SERVICE = new VanillaPlayerTeamService();
	private static final double[][] SAFE_SPAWN_OFFSETS = {
		{0.0D, 0.0D, 0.0D},
		{1.0D, 0.0D, 0.0D},
		{-1.0D, 0.0D, 0.0D},
		{0.0D, 0.0D, 1.0D},
		{0.0D, 0.0D, -1.0D},
		{1.0D, 0.0D, 1.0D},
		{-1.0D, 0.0D, 1.0D},
		{1.0D, 0.0D, -1.0D},
		{-1.0D, 0.0D, -1.0D},
		{0.0D, 1.0D, 0.0D},
		{1.0D, 1.0D, 0.0D},
		{-1.0D, 1.0D, 0.0D},
		{0.0D, 1.0D, 1.0D},
		{0.0D, 1.0D, -1.0D}
	};

	private SummonedMobSupport() {
	}

	public static void applyFriendlyTeam(UnitContext context, MobEntity mob) {
		if (context == null || mob == null || context.state() == null) {
			return;
		}
		var teamId = context.state().getTeamId();
		var server = context.player() == null ? null : context.player().getServer();
		if (teamId == null || server == null) {
			return;
		}
		PLAYER_TEAM_SERVICE.assignScoreHolder(server.getScoreboard(), mob.getNameForScoreboard(), teamId);
		mob.setTarget(null);
	}

	public static boolean placeMobSafely(ServerWorld world, MobEntity mob, double x, double y, double z, float yaw, float pitch) {
		if (world == null || mob == null) {
			return false;
		}
		for (var offset : SAFE_SPAWN_OFFSETS) {
			mob.refreshPositionAndAngles(x + offset[0], y + offset[1], z + offset[2], yaw, pitch);
			if (world.isSpaceEmpty(mob)) {
				return true;
			}
		}
		return false;
	}

	public static TeamId resolveManagedTeam(Entity entity) {
		if (entity == null || entity instanceof net.minecraft.server.network.ServerPlayerEntity || entity.getScoreboardTeam() == null) {
			return null;
		}
		var name = entity.getScoreboardTeam().getName();
		if (VanillaPlayerTeamService.RED_TEAM_NAME.equals(name)) {
			return TeamId.RED;
		}
		if (VanillaPlayerTeamService.BLUE_TEAM_NAME.equals(name)) {
			return TeamId.BLUE;
		}
		return null;
	}

	public static void clearFriendlyTargets(MinecraftServer server, MatchManager matchManager) {
		if (server == null || matchManager == null) {
			return;
		}
		for (var world : server.getWorlds()) {
			for (var entity : world.iterateEntities()) {
				if (entity instanceof MobEntity mob && resolveManagedTeam(mob) != null) {
					clearFriendlyTarget(mob, matchManager);
				}
			}
		}
	}

	public static boolean shouldClearFriendlyTarget(TeamId mobTeamId, TeamId targetTeamId) {
		return mobTeamId != null && targetTeamId != null && mobTeamId == targetTeamId;
	}

	private static void clearFriendlyTarget(MobEntity mob, MatchManager matchManager) {
		if (mob == null || matchManager == null) {
			return;
		}
		var target = mob.getTarget();
		if (!(target instanceof LivingEntity livingTarget)) {
			return;
		}
		if (shouldClearFriendlyTarget(resolveManagedTeam(mob), resolveTargetTeam(livingTarget, matchManager))) {
			mob.setTarget(null);
		}
	}

	private static TeamId resolveTargetTeam(LivingEntity target, MatchManager matchManager) {
		if (target instanceof net.minecraft.server.network.ServerPlayerEntity player) {
			return matchManager.getPlayerState(player.getUuid()).getTeamId();
		}
		return resolveManagedTeam(target);
	}
}
