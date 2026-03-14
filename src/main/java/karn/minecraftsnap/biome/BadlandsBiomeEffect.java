package karn.minecraftsnap.biome;

import karn.minecraftsnap.game.TeamId;

public class BadlandsBiomeEffect extends NoOpBiomeEffect {
	@Override
	public void onCaptureScore(BiomeRuntimeContext context, TeamId ownerTeam) {
		if (ownerTeam != null) {
			context.matchManager().addScore(ownerTeam, 1);
		}
	}
}
