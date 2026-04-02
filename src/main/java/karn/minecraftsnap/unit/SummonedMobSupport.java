package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.MatchManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Formatting;

public final class SummonedMobSupport {
	private static final String MANAGED_TEAM_PREFIX = "mcsnap_mob_";

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
		var scoreboard = server.getScoreboard();
		var teamName = managedTeamName(teamId);
		var team = scoreboard.getTeam(teamName);
		if (team == null) {
			team = scoreboard.addTeam(teamName);
			team.setColor(teamId == TeamId.RED ? Formatting.RED : Formatting.BLUE);
			team.setFriendlyFireAllowed(false);
		}
		scoreboard.addScoreHolderToTeam(mob.getNameForScoreboard(), team);
		mob.setTarget(null);
	}

	public static String managedTeamName(TeamId teamId) {
		return teamId == null ? "" : MANAGED_TEAM_PREFIX + teamId.name().toLowerCase();
	}

	public static TeamId resolveManagedTeam(Entity entity) {
		if (entity == null || entity.getScoreboardTeam() == null) {
			return null;
		}
		var name = entity.getScoreboardTeam().getName();
		if (!name.startsWith(MANAGED_TEAM_PREFIX)) {
			return null;
		}
		var rawTeam = name.substring(MANAGED_TEAM_PREFIX.length()).toUpperCase();
		try {
			return TeamId.valueOf(rawTeam);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
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
