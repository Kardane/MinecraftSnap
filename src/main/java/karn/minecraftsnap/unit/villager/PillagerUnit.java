package karn.minecraftsnap.unit.villager;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class PillagerUnit extends AbstractVillagerUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().getInventory().insertStack(new ItemStack(Items.FIREWORK_ROCKET, 3));
			return true;
		});
	}
}
