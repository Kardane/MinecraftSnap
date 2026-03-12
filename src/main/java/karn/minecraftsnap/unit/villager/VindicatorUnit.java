package karn.minecraftsnap.unit.villager;

public class VindicatorUnit extends AbstractVillagerUnit {
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			var player = context.player();
			var dash = player.getRotationVec(1.0f).multiply(1.2D);
			player.addVelocity(dash.x, 0.3D, dash.z);
			return true;
		});
	}
}
