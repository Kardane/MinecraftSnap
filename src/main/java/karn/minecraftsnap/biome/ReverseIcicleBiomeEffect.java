package karn.minecraftsnap.biome;

import karn.minecraftsnap.game.TeamId;

public class ReverseIcicleBiomeEffect extends NoOpBiomeEffect {
	@Override
	public int captureScoreAmount(BiomeRuntimeContext context, TeamId ownerTeam) {
		if (context == null) {
			return 0;
		}
		return context.serverTicks() % 40L == 0L ? 1 : 0;
	}
}
