package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.unit.AbstractUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractNetherUnit extends AbstractUnitClass {
	@Override
	public void onKill(UnitContext context, ServerPlayerEntity victim) {
		// 보상은 InGameRuleService에서 통합 관리함
	}

	@Override
	public void onShiftF(UnitContext context) {
		context.openTrade();
	}
}
