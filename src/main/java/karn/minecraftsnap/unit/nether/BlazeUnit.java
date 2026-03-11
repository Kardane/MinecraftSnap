package karn.minecraftsnap.unit.nether;

public class BlazeUnit extends AbstractNetherUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(context::spawnFireballBurst);
	}
}
