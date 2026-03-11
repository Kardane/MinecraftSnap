package karn.minecraftsnap.unit.villager;

public class VindicatorUnit extends AbstractVillagerUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.dash(1.2D, 0.3D));
	}
}
