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
	@Override
	public void onSkillUse(karn.minecraftsnap.unit.UnitContext context) {
		context.activateSkill(() -> {
			context.healSelf(healAmount());
			context.player().getWorld().playSound(
				null,
				context.player().getBlockPos(),
				SoundEvents.ENTITY_GENERIC_EAT.value(),
				SoundCategory.PLAYERS,
				0.9f,
				1.0f
			);
			var player = context.player();
			player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 0.9f, 1.1f);
			return true;
		});
	}

	float healAmount() {
		return 5.0f;
	}
}
