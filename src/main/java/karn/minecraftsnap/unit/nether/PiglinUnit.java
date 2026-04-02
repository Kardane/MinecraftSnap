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
	public static final UnitDefinition DEFINITION = unit(
		"piglin",
		"피글린",
		FactionId.NETHER,
		true,
		2,
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
		UnitDefinition.AmmoType.ARROW,
		disguise("minecraft:piglin"),
			List.of("&f패시브 &7- 적 처치시 금괴 1개 추가 획득","&f무기 &7- 금 검, 쇠뇌"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void buildLoadout(UnitContext context) {
		context.baseBuildLoadout();
		context.player().getInventory().insertStack(new ItemStack(Items.CROSSBOW, 1));
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
