package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.integration.DisguiseSupport;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;

public class VindicatorUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		if (context == null || context.player() == null || !shouldRefreshAggressiveDisguise(context.player().getMainHandStack() != null && !context.player().getMainHandStack().isEmpty())) {
			return;
		}
		//DisguiseSupport.applyDisguise(context.player(), context.unitDefinition().disguise());
	}

	boolean shouldRefreshAggressiveDisguise(boolean hasWeaponInMainHand) {
		return hasWeaponInMainHand;
	}
}
