package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.config.EntitySpecEntry;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ChargedCreeperUnit extends CreeperUnit {
	private static final EntitySpecEntry POWERED_DISGUISE = disguise("minecraft:creeper", "{powered:1b}");
	private static final EntitySpecEntry POWERED_IGNITED_DISGUISE = disguise("minecraft:creeper", "{powered:1b,ignited:1b}");

	@Override
	EntitySpecEntry restingDisguise() {
		return POWERED_DISGUISE;
	}

	@Override
	EntitySpecEntry activeDisguise() {
		return POWERED_IGNITED_DISGUISE;
	}

	@Override
	double blastRadius() {
		return 9.0D;
	}

	@Override
	float blastDamage() {
		return 120.0f;
	}

	@Override
	boolean powered() {
		return true;
	}
}
