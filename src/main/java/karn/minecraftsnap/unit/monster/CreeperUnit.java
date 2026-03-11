package karn.minecraftsnap.unit.monster;

public class CreeperUnit extends AbstractMonsterUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> context.queueCreeperBomb(20, "&c자폭 준비"));
	}
}
