package karn.minecraftsnap.biome;

import karn.minecraftsnap.game.UnitDefinition;
import karn.minecraftsnap.game.TeamId;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public interface BiomeEffect {
	default List<String> revealMessages(BiomeRuntimeContext context) {
		return context.biomeEntry().revealMessages;
	}

	default void onReveal(BiomeRuntimeContext context) {
	}

	default void onTick(BiomeRuntimeContext context) {
	}

	default void onActiveSkill(BiomeRuntimeContext context, ServerPlayerEntity actor, UnitDefinition unit) {
	}

	default void onDamaged(BiomeRuntimeContext context, ServerPlayerEntity victim, DamageSource source, float amount) {
	}

	default void onAttack(BiomeRuntimeContext context, ServerPlayerEntity attacker, ServerPlayerEntity victim, float amount) {
	}

	default void onDeath(BiomeRuntimeContext context, ServerPlayerEntity victim, DamageSource source) {
	}

	default void onCaptureScore(BiomeRuntimeContext context, TeamId ownerTeam) {
	}

	default int captureScoreAmount(BiomeRuntimeContext context, TeamId ownerTeam) {
		return 1;
	}
}
