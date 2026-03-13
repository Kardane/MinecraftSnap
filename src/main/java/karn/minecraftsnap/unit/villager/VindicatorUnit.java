package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class VindicatorUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"vindicator",
		"변명자",
		FactionId.VILLAGER,
		true,
		4,
		18,
		30.0,
		1.0,
		item("minecraft:iron_axe"),
		none(),
		none(),
		none(),
		none(),
		none(),
		none(),
		"도약",
		10,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:vindicator"),
		List.of("&7짧은 돌진 스킬", "&7근접 압박용"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

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
