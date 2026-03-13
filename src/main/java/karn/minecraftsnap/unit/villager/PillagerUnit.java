package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class PillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"pillager",
		"약탈자",
		FactionId.VILLAGER,
		true,
		3,
		15,
		16.0,
		1.1,
		item("minecraft:crossbow"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:firework_rocket", "폭죽 화살 지급", 15),
		"폭죽 화살 지급",
		15,
		UnitDefinition.AmmoType.FIREWORK,
		disguise("minecraft:pillager"),
		List.of("&73발 폭죽 지급", "&7원거리 견제용"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().getInventory().insertStack(new ItemStack(Items.FIREWORK_ROCKET, 3));
			return true;
		});
	}
}
