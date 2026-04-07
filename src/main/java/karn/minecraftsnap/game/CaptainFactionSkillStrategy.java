package karn.minecraftsnap.game;

import karn.minecraftsnap.config.SystemConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

interface CaptainFactionSkillStrategy {
	FactionId factionId();

	boolean use(ServerPlayerEntity captain, PlayerMatchState state, SystemConfig systemConfig);

	default void tick(MinecraftServer server, SystemConfig systemConfig) {
	}

	default void clearRuntimeState(MinecraftServer server, SystemConfig systemConfig) {
	}
}
