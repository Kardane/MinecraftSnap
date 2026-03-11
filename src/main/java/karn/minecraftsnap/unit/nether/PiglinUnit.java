package karn.minecraftsnap.unit.nether;

import net.minecraft.entity.damage.DamageSource;

public class PiglinUnit extends AbstractNetherUnit {
	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		context.trySpawnZombifiedPiglin(0.5f);
	}
}
