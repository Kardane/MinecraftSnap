package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.unit.AbstractUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractVillagerUnit extends AbstractUnitClass {
	@Override
	public void onKill(UnitContext context, ServerPlayerEntity victim) {
		// 보상은 InGameRuleService에서 통합 관리함
	}

	@Override
	public void onCaptureScore(UnitContext context) {
		context.rewardEmerald(1);
	}

	@Override
	public void onShiftF(UnitContext context) {
		context.openTrade();
	}
}
