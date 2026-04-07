package karn.minecraftsnap.game;

import karn.minecraftsnap.MinecraftSnap;
import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.config.TextConfigFile;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

abstract class AbstractCaptainFactionSkillStrategy implements CaptainFactionSkillStrategy {
	protected final MatchManager matchManager;
	protected final LaneRuntimeRegistry laneRuntimeRegistry;
	protected final CaptainWeatherGuiService captainWeatherGuiService;
	protected final CaptainManaService captainManaService;
	protected final TextTemplateResolver textTemplateResolver;
	protected final UiSoundService uiSoundService;
	protected final Random random;

	protected AbstractCaptainFactionSkillStrategy(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random
	) {
		this.matchManager = matchManager;
		this.laneRuntimeRegistry = laneRuntimeRegistry;
		this.captainWeatherGuiService = captainWeatherGuiService;
		this.captainManaService = captainManaService;
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
		this.random = random;
	}

	protected final TextConfigFile textConfig() {
		var mod = MinecraftSnap.getInstance();
		return mod == null ? new TextConfigFile() : mod.getTextConfig();
	}

	protected final void playUiDeny(ServerPlayerEntity player) {
		if (uiSoundService != null) {
			uiSoundService.playUiDeny(player);
		}
	}

	protected final void playEventSuccess(ServerPlayerEntity player) {
		if (uiSoundService != null) {
			uiSoundService.playEventSuccess(player);
		}
	}

	protected final ServerWorld resolveWorld(ServerPlayerEntity player, String worldId) {
		return player == null ? null : resolveWorld(player.getServer(), worldId);
	}

	protected final ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		if (server == null) {
			return null;
		}
		try {
			var key = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
			var world = server.getWorld(key);
			return world != null ? world : server.getOverworld();
		} catch (Exception ignored) {
			return server.getOverworld();
		}
	}

	protected final void spawnLineParticles(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect particle) {
		if (world == null || from == null || to == null || particle == null) {
			return;
		}
		var delta = to.subtract(from);
		var length = delta.length();
		var steps = Math.max(1, (int) Math.ceil(length / 0.6D));
		for (int i = 0; i <= steps; i++) {
			var pos = from.add(delta.multiply(i / (double) steps));
			for (var player : world.getPlayers()) {
				world.spawnParticles(player, particle, true, false, pos.x, pos.y + 1.0D, pos.z, 3, 0.03D, 0.03D, 0.03D, 0.0D);
			}
		}
	}

	protected final void spawnPortalBurst(ServerWorld world, SystemConfig.PositionConfig position) {
		if (world == null || position == null) {
			return;
		}
		for (var player : world.getPlayers()) {
			world.spawnParticles(player, ParticleTypes.PORTAL, true, false, position.x, position.y + 1.0D, position.z, 180, 3.2D, 2.2D, 3.2D, 0.2D);
		}
	}

	protected final void playGlobal(ServerWorld world, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		if (world == null || sound == null || category == null) {
			return;
		}
		for (var player : world.getPlayers()) {
			player.playSoundToPlayer(sound, category, volume, pitch);
		}
	}

	protected final void spawnLightningStrike(ServerWorld world, ServerPlayerEntity target) {
		if (world == null || target == null) {
			return;
		}
		var lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
		lightning.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
		lightning.setCosmetic(false);
		world.spawnEntity(lightning);
	}

	protected final double healAndTrack(ServerPlayerEntity healer, ServerPlayerEntity target, float amount) {
		if (target == null || amount <= 0.0F) {
			return 0.0D;
		}
		float before = target.getHealth();
		target.heal(amount);
		double healed = Math.max(0.0D, (double) target.getHealth() - (double) before);
		if (healer != null && healed > 0.0D) {
			var mod = MinecraftSnap.getInstance();
			if (mod != null) {
				mod.getStatsRepository().addHealingDone(healer.getUuid(), healer.getName().getString(), healed);
			}
		}
		return healed;
	}

	protected final double fullHealAndTrack(ServerPlayerEntity healer, ServerPlayerEntity target) {
		if (target == null) {
			return 0.0D;
		}
		float before = target.getHealth();
		target.setHealth(target.getMaxHealth());
		double healed = Math.max(0.0D, (double) target.getHealth() - (double) before);
		if (healer != null && healed > 0.0D) {
			var mod = MinecraftSnap.getInstance();
			if (mod != null) {
				mod.getStatsRepository().addHealingDone(healer.getUuid(), healer.getName().getString(), healed);
			}
		}
		return healed;
	}

	protected final void recordCaptainSkillUse(FactionId factionId) {
		if (factionId == null) {
			return;
		}
		var mod = MinecraftSnap.getInstance();
		if (mod != null) {
			mod.getServerStatsRepository().addCaptainSkillUse(factionId, 1);
		}
	}

	protected final void broadcastCaptainSkill(ServerPlayerEntity captain, String detailMessage) {
		if (captain == null || captain.getServer() == null || detailMessage == null || detailMessage.isBlank()) {
			return;
		}
		var template = textConfig().captainSkillActivatedMessage;
		captain.getServer().getPlayerManager().broadcast(
			textTemplateResolver.format(template
				.replace("{captain}", captain.getName().getString())
				.replace("{detail}", detailMessage)),
			false
		);
	}

	protected final String laneLabel(LaneId laneId) {
		var textConfig = textConfig();
		return switch (laneId) {
			case LANE_1 -> textConfig.captureLane1Name;
			case LANE_2 -> textConfig.captureLane2Name;
			case LANE_3 -> textConfig.captureLane3Name;
		};
	}

	protected final List<UUID> captainIdsByFaction(FactionId factionId) {
		return matchManager.getCaptainIds().stream()
			.filter(captainId -> matchManager.getPlayerState(captainId).getFactionId() == factionId)
			.toList();
	}

	protected final List<ServerPlayerEntity> findRecallTargets(TeamId teamId, LaneId targetLane) {
		var result = new ArrayList<ServerPlayerEntity>();
		for (var player : matchManager.getOnlinePlayers()) {
			var state = matchManager.getPlayerState(player.getUuid());
			if (!state.isUnit() || state.getTeamId() != teamId || state.getCurrentUnitId() == null || player.isSpectator()) {
				continue;
			}
			var runtime = laneRuntimeRegistry.findByPlayer(player);
			if (runtime == null || runtime.laneId() == targetLane) {
				continue;
			}
			result.add(player);
		}
		result.sort(Comparator.comparing(player -> player.getUuid().toString()));
		return result;
	}

	protected final List<ServerPlayerEntity> teamUnits(TeamId teamId) {
		return matchManager.getOnlinePlayers().stream()
			.filter(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return state.isUnit() && state.getTeamId() == teamId && state.getCurrentUnitId() != null && !player.isSpectator();
			})
			.toList();
	}

	protected final List<ServerPlayerEntity> enemyUnits(TeamId teamId) {
		return matchManager.getOnlinePlayers().stream()
			.filter(player -> {
				var state = matchManager.getPlayerState(player.getUuid());
				return state.isUnit() && state.getTeamId() != null && state.getTeamId() != teamId && state.getCurrentUnitId() != null && !player.isSpectator();
			})
			.toList();
	}

	protected final List<ServerPlayerEntity> laneUnits(TeamId teamId, LaneId laneId) {
		return teamUnits(teamId).stream()
			.filter(player -> {
				var runtime = laneRuntimeRegistry.findByPlayer(player);
				return runtime != null && runtime.laneId() == laneId;
			})
			.toList();
	}
}
