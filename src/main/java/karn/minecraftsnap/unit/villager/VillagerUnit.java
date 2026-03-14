package karn.minecraftsnap.unit.villager;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.abilityItem;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class VillagerUnit extends AbstractVillagerUnit implements ConfiguredUnitClass {
	public static final UnitDefinition DEFINITION = unit(
		"villager",
		"멍청이 주민",
		FactionId.VILLAGER,
		true,
		1,
		5,
		20.0,
		0.8,
		karn.minecraftsnap.unit.UnitSpecSupport.item("minecraft:wooden_sword"),
		item("minecraft:bread"),
		none(),
		none(),
		none(),
		none(),
		abilityItem("minecraft:bread", "빵 먹기", 8),
		"빵 먹기",
		8,
		UnitDefinition.AmmoType.NONE,
		disguise("minecraft:villager"),
		List.of("&7빵으로 체력 5 회복", "&7기본 유지력 유닛"),
		List.of()
	);

	@Override
	public UnitDefinition definition() {
		return DEFINITION;
	}

	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.player().heal(healAmount());
			context.player().getWorld().playSound(
				null,
				context.player().getBlockPos(),
				SoundEvents.ENTITY_GENERIC_EAT.value(),
				SoundCategory.PLAYERS,
				0.9f,
				1.0f
			);
			return true;
		});
	}

	float healAmount() {
		return 5.0f;
	}
}
