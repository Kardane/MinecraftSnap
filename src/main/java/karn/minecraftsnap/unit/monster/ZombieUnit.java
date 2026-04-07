package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import net.minecraft.entity.damage.DamageSource;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.advanceOption;
import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class ZombieUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		context.restoreCaptainMana(captainManaRestoreOnDeath());
	}

	int captainManaRestoreOnDeath() {
		return 1;
	}
}
