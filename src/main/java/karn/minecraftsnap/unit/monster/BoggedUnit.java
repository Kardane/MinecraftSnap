package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class BoggedUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		if (isPoisonImmune()) {
			context.player().removeStatusEffect(StatusEffects.POISON);
		}
	}

	boolean isPoisonImmune() {
		return true;
	}
}
