package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.TeamId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombifiedPiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"zombified_piglin",
		"좀비 피글린",
		FactionId.NETHER,
		true,
		1,
		10,
		20.0,
		1.0,
		item("minecraft:golden_sword"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"",
		0,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:zombified_piglin"),
		List.of("&7피격 시 20% 확률로 주변 5칸 내에 같은 팀 좀비 피글린 소환"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onDamaged(UnitContext context, DamageSource source, float amount) {
		if (context.player().getRandom().nextDouble() >= summonChance()) {
			return;
		}
		var world = context.world();
		var player = context.player();
		var piglin = EntityType.ZOMBIFIED_PIGLIN.create(world, SpawnReason.MOB_SUMMONED);
		if (piglin == null) {
			return;
		}
		var random = player.getRandom();
		var x = player.getX() + random.nextBetween(-5, 5);
		var z = player.getZ() + random.nextBetween(-5, 5);
		piglin.refreshPositionAndAngles(x, player.getY(), z, player.getYaw(), player.getPitch());
		world.spawnEntity(piglin);
		applyFriendlyTeam(context, piglin);
		piglin.setTarget(null);
	}

	private void applyFriendlyTeam(UnitContext context, ZombifiedPiglinEntity piglin) {
		var teamId = context.state().getTeamId();
		var server = context.player().getServer();
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
		scoreboard.addScoreHolderToTeam(piglin.getNameForScoreboard(), team);
	}

	double summonChance() {
		return 0.2D;
	}

	double summonRange() {
		return 5.0D;
	}
}
