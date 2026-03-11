package karn.minecraftsnap.unit.monster;

import net.minecraft.server.network.ServerPlayerEntity;

public class ZombieUnit extends AbstractMonsterUnit {
	@Override
	public void onKill(karn.minecraftsnap.unit.UnitContext context, ServerPlayerEntity victim) {
		context.reduceCaptainSpawnCooldown(2);
	}
}
