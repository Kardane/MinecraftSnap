package karn.minecraftsnap.unit.nether;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PiglinUnit extends AbstractNetherUnit implements ConfiguredUnitClass {
	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		context.player().getInventory().insertStack(new ItemStack(Items.CROSSBOW, 1));
		context.player().getInventory().insertStack(new ItemStack(Items.ARROW, 3));
	}

	@Override
	public void onKill(UnitContext context, ServerPlayerEntity victim) {
		super.onKill(context, victim);
		context.rewardGold(bonusGoldOnKill());
	}

	int supportGoldCount() {
		return 3;
	}

	int bonusGoldOnKill() {
		return 1;
	}
}
