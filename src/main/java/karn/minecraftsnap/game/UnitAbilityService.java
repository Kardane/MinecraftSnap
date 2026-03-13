package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.biome.BiomeRuntimeContext;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class UnitAbilityService {
	private static final int CAPTAIN_SKILL_COOLDOWN_SECONDS = 30;

	private final TextTemplateResolver textTemplateResolver;
	private final LaneRuntimeRegistry laneRuntimeRegistry;
	private final Map<UUID, Long> unitCooldownTicks = new HashMap<>();

	public UnitAbilityService() {
		this(new TextTemplateResolver(), null);
	}

	public UnitAbilityService(TextTemplateResolver textTemplateResolver) {
		this(textTemplateResolver, null);
	}

	public UnitAbilityService(TextTemplateResolver textTemplateResolver, LaneRuntimeRegistry laneRuntimeRegistry) {
		this.textTemplateResolver = textTemplateResolver;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
	}

	public void tick(MinecraftServer server, MatchManager matchManager) {
	}

	public boolean useCaptainSkill(ServerPlayerEntity captain, MatchManager matchManager, CaptainManaService captainManaService) {
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() == null) {
			return false;
		}

		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getSkillCooldownSeconds() > 0) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillCooldownMessage.replace("{seconds}", Integer.toString(captainState.getSkillCooldownSeconds()))), true);
			return false;
		}

		switch (state.getFactionId()) {
			case VILLAGER -> captain.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 8, 1));
			case MONSTER -> captain.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 0));
			case NETHER -> {
				captain.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 8, 0));
				captain.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 0));
			}
		}

		captainManaService.triggerSkillCooldown(captain.getUuid(), CAPTAIN_SKILL_COOLDOWN_SECONDS);
		notifyActiveSkillHook(captain, null, matchManager);
		captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillActivatedMessage), true);
		return true;
	}

	public boolean activateUnitSkill(ServerPlayerEntity player, MatchManager matchManager, UnitDefinition definition, BooleanSupplier action) {
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
		unitCooldownTicks.put(player.getUuid(), matchManager.getServerTicks() + definition.abilityCooldownSeconds() * 20L);
		player.getItemCooldownManager().set(player.getMainHandStack(), definition.abilityCooldownSeconds() * 20);
		return true;
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
			runtime,
			runtime.biomeEntry(),
			textTemplateResolver,
			matchManager.getServerTicks(),
			matchManager.getTotalSeconds() - matchManager.getRemainingSeconds()
		);
		runtime.biomeEffect().onActiveSkill(context, player, definition);
	}

	private TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}
}
