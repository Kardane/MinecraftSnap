package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.damage.DamageSource;

import java.util.List;
import java.util.Set;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombieUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	private static final Set<String> ZOMBIE_FAMILY_UNIT_IDS = Set.of("zombie", "drowned", "husk");

	@Override
	public void onDeath(UnitContext context, DamageSource source) {
		if (!shouldRestoreCaptainManaOnDeath(context)) {
			return;
		}
		context.restoreCaptainMana(captainManaRestoreOnDeath());
		context.resetCaptainSpawnCooldown();
	}

	boolean shouldRestoreCaptainManaOnDeath(UnitContext context) {
		return countFriendlyZombieFamilyUnitsOnLane(context) <= maxFriendlyZombieFamilyUnitsForRefund();
	}

	int countFriendlyZombieFamilyUnitsOnLane(UnitContext context) {
		if (context == null || context.laneRuntime() == null || context.matchManager() == null || context.matchManager().getServer() == null || context.state() == null || context.state().getTeamId() == null) {
			return Integer.MAX_VALUE;
		}
		var teamId = context.state().getTeamId();
		return (int) context.laneRuntime().resolveAliveUnitPlayers(context.matchManager().getServer()).stream()
			.map(player -> context.matchManager().getPlayerState(player.getUuid()))
			.filter(state -> state.getTeamId() == teamId)
			.filter(state -> ZOMBIE_FAMILY_UNIT_IDS.contains(state.getCurrentUnitId()))
			.count();
	}

	int maxFriendlyZombieFamilyUnitsForRefund() {
		return 5;
	}

	int captainManaRestoreOnDeath() {
		return 1;
	}
}
