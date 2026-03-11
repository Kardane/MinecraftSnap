package karn.minecraftsnap.unit.monster;

public class SkeletonUnit extends AbstractMonsterUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.boneBlast(4.0D, 6.0f, 3, 1));
	}
}
