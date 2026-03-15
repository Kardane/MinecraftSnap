package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.TeamId;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Formatting;

public final class SummonedMobSupport {
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
		var teamName = "mcsnap_mob_" + teamId.name().toLowerCase();
		var team = scoreboard.getTeam(teamName);
		if (team == null) {
			team = scoreboard.addTeam(teamName);
			team.setColor(teamId == TeamId.RED ? Formatting.RED : Formatting.BLUE);
			team.setFriendlyFireAllowed(false);
		}
		scoreboard.addScoreHolderToTeam(mob.getNameForScoreboard(), team);
		mob.setTarget(null);
	}
}
