package karn.minecraftsnap.biome;

public class NetherBiomeEffect extends NoOpBiomeEffect {
	private static final float BONUS_DAMAGE = 3.0F;

	@Override
	public void onAttack(BiomeRuntimeContext context, net.minecraft.server.network.ServerPlayerEntity attacker, net.minecraft.server.network.ServerPlayerEntity victim, float amount) {
		if (attacker == null || victim == null || victim.getWorld() == null || context == null || context.matchManager() == null) {
			return;
		}
		var attackerState = context.matchManager().getPlayerState(attacker.getUuid());
		var victimState = context.matchManager().getPlayerState(victim.getUuid());
		if (attackerState.getTeamId() == null || attackerState.getTeamId() == victimState.getTeamId()) {
			return;
		}
		if (victim.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
			victim.damage(serverWorld, victim.getWorld().getDamageSources().magic(), BONUS_DAMAGE);
		}
	}
}
