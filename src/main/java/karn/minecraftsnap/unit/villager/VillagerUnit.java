package karn.minecraftsnap.unit.villager;

public class VillagerUnit extends AbstractVillagerUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().heal(6.0f);
			return true;
		});
	}
}
