package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class VillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"villager",
		"주민",
		FactionId.VILLAGER,
		true,
		1,
		5,
		20.0,
		0.8,
		karn.minecraftsnap.unit.UnitSpecSupport.item("minecraft:wooden_sword"),
		none(),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:bread", "밥먹기", 10),
		"밥먹기",
		10,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:villager"),
		List.of("&7체력 3 회복", "&7기본 유지력 유닛"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().heal(6.0f);
			return true;
		});
	}
}
