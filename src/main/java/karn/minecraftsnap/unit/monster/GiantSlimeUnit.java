package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.attribute.EntityAttributes;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class GiantSlimeUnit extends SlimeUnit {
	@Override
	public void applyAttributes(UnitContext context) {
		super.applyAttributes(context);
		applyScale(context);
	}

	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		applyScale(context);
	}

	private void applyScale(UnitContext context) {
		var scale = context.player().getAttributeInstance(EntityAttributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(context.unitDefinition().extraAttributes().scaleOrDefault(2.0D));
		}
	}

	@Override
	int spawnedSlimeCount() {
		return 3;
	}

	@Override
	int spawnedSlimeSize() {
		return 4;
	}
}
