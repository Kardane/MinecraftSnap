package karn.minecraftsnap.biome;

import karn.minecraftsnap.game.TeamId;

public class ReverseIcicleBiomeEffect extends NoOpBiomeEffect {
	@Override
	public int captureScoreAmount(BiomeRuntimeContext context, TeamId ownerTeam) {
		return 0;
	}
}
