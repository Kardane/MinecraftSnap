package karn.minecraftsnap.unit.monster;

import net.minecraft.entity.damage.DamageSource;

public class SlimeUnit extends AbstractMonsterUnit {
	@Override
	public void onDeath(karn.minecraftsnap.unit.UnitContext context, DamageSource source) {
		context.spawnSlimeSplit();
	}
}
