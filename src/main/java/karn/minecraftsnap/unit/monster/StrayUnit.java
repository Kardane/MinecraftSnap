package karn.minecraftsnap.unit.monster;

import karn.minecraftsnap.game.FactionId;
import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.unit.ConfiguredUnitClass;
import karn.minecraftsnap.unit.UnitContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.List;

import static karn.minecraftsnap.unit.UnitSpecSupport.disguise;
import static karn.minecraftsnap.unit.UnitSpecSupport.item;
import static karn.minecraftsnap.unit.UnitSpecSupport.none;
import static karn.minecraftsnap.unit.UnitSpecSupport.unit;

public class StrayUnit extends AbstractMonsterUnit implements ConfiguredUnitClass {
	@Override
	public void onTick(UnitContext context) {
		super.onTick(context);
		if (isColdBiome(context.currentBiomeId())) {
			context.player().addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, false));
		}
	}

	boolean isColdBiome(String biomeId) {
		if (biomeId == null || biomeId.isBlank()) {
			return false;
		}
		return biomeId.contains("taiga")
			|| biomeId.contains("cold_ocean")
			|| biomeId.contains("frozen_ocean")
			|| biomeId.contains("jagged_peaks");
	}
}
