package karn.minecraftsnap.unit.villager;

public class PillagerUnit extends AbstractVillagerUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.giveFireworks(3));
	}
}
