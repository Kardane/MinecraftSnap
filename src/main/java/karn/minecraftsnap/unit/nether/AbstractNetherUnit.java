package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.unit.AbstractUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractNetherUnit extends AbstractUnitClass {
	@Override
	public void onKill(UnitContext context, ServerPlayerEntity victim) {
		context.rewardGold(1);
	}

	@Override
	public void onShiftF(UnitContext context) {
		context.openTrade();
	}
}
