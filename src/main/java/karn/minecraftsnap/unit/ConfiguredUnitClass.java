package karn.minecraftsnap.unit;

import karn.minecraftsnap.game.UnitDefinition;

public interface ConfiguredUnitClass extends UnitClass {
	UnitDefinition definition();
}
