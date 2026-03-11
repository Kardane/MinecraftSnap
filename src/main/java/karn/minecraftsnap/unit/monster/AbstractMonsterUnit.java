package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.unit.AbstractUnitClass;
import karn.minecraftsnap.unit.UnitContext;

public abstract class AbstractMonsterUnit extends AbstractUnitClass {
	@Override
	public void onTick(UnitContext context) {
		context.updateMonsterAdvance();
	}

	@Override
	public void onShiftF(UnitContext context) {
		context.openAdvance();
	}
}
