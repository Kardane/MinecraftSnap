package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class UnitAbilityService {
	private final TextTemplateResolver textTemplateResolver;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final Map<UUID, Long> unitCooldownTicks = new HashMap<>();
	private final java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier;

	public UnitAbilityService() {
		this(new TextTemplateResolver(), null, () -> null);
	}

	public UnitAbilityService(TextTemplateResolver textTemplateResolver) {
		this(textTemplateResolver, null, () -> null);
	}

	public UnitAbilityService(
		TextTemplateResolver textTemplateResolver,
		LaneRuntimeRegistry laneRuntimeRegistry,
		java.util.function.Supplier<CaptainSkillService> captainSkillServiceSupplier
	) {
		this.textTemplateResolver = textTemplateResolver;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.captainSkillServiceSupplier = captainSkillServiceSupplier;
	}

	public void tick(MinecraftServer server, MatchManager matchManager) {
	}

	public boolean useCaptainSkill(ServerPlayerEntity captain, MatchManager matchManager, CaptainManaService captainManaService) {
		var mod = MinecraftSnap.getInstance();
		var captainSkillService = captainSkillServiceSupplier.get();
		if (captainSkillService == null || mod == null) {
			return false;
		}
		var used = captainSkillService.useCaptainSkill(captain, mod.getSystemConfig());
		if (used) {
			notifyActiveSkillHook(captain, null, matchManager);
		}
		return used;
	}

	public boolean activateUnitSkill(ServerPlayerEntity player, MatchManager matchManager, UnitDefinition definition, BooleanSupplier action) {
		return activateUnitSkill(player, matchManager, definition, null, action);
	}

	public boolean activateUnitSkill(ServerPlayerEntity player, MatchManager matchManager, UnitDefinition definition, Long cooldownTicksOverride, BooleanSupplier action) {
		if (player == null || matchManager == null || definition == null || action == null) {
			return false;
		}
		var nextUseTick = unitCooldownTicks.getOrDefault(player.getUuid(), Long.MIN_VALUE);
		if (matchManager.getServerTicks() < nextUseTick) {
			return false;
		}
		if (!action.getAsBoolean()) {
			return false;
		}
		notifyActiveSkillHook(player, definition, matchManager);
		var cooldownTicks = resolveCooldownTicks(definition, cooldownTicksOverride);
		unitCooldownTicks.put(player.getUuid(), matchManager.getServerTicks() + cooldownTicks);
		var cooldownItem = definition.skillCooldownItem();
		if (cooldownItem != null) {
			player.getItemCooldownManager().set(cooldownItem.getDefaultStack(), (int) cooldownTicks);
		}
		return true;
	}

	long resolveCooldownTicks(UnitDefinition definition, Long cooldownTicksOverride) {
		if (cooldownTicksOverride != null && cooldownTicksOverride >= 0L) {
			return cooldownTicksOverride;
		}
		return definition == null ? 0L : definition.abilityCooldownSeconds() * 20L;
	}

	public void clearPlayerState(UUID playerId) {
		unitCooldownTicks.remove(playerId);
	}

	public int remainingUnitCooldownSeconds(UUID playerId, long serverTicks) {
		if (playerId == null) {
			return 0;
		}
		var nextUseTick = unitCooldownTicks.getOrDefault(playerId, Long.MIN_VALUE);
		if (serverTicks >= nextUseTick) {
			return 0;
		}
		return (int) Math.ceil((nextUseTick - serverTicks) / 20.0D);
	}

	private void notifyActiveSkillHook(ServerPlayerEntity player, UnitDefinition definition, MatchManager matchManager) {
		if (laneRuntimeRegistry == null) {
			return;
		}
		var runtime = laneRuntimeRegistry.findByPlayer(player);
		if (runtime == null || !runtime.hasActiveBiome()) {
			return;
		}
		var context = new BiomeRuntimeContext(
			player.getServer(),
			(ServerWorld) player.getWorld(),
			matchManager,
			runtime,
			runtime.biomeEntry(),
			textTemplateResolver,
			matchManager.getServerTicks(),
			matchManager.getElapsedSeconds()
		);
		runtime.biomeEffect().onActiveSkill(context, player, definition);
	}

}
