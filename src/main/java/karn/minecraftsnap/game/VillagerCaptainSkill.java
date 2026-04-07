package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.Random;

final class VillagerCaptainSkill extends AbstractCaptainFactionSkillStrategy {
	static final int COOLDOWN_SECONDS = 60;

	VillagerCaptainSkill(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random
	) {
		super(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random);
	}

	@Override
	public FactionId factionId() {
		return FactionId.VILLAGER;
	}

	@Override
	public boolean use(ServerPlayerEntity captain, PlayerMatchState state, SystemConfig systemConfig) {
		var targetLane = UnitSpawnService.nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(targetLane)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().closedLaneMessage), true);
			playUiDeny(captain);
			return false;
		}
		var teleportTargets = new java.util.ArrayList<>(findRecallTargets(state.getTeamId(), targetLane));
		var buffTargets = new java.util.ArrayList<>(teamUnits(state.getTeamId()));
		if (buffTargets.isEmpty()) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainVillagerNoTargetMessage), true);
			playUiDeny(captain);
			return false;
		}
		var targetSpawn = UnitSpawnService.safeUnitSpawn(systemConfig, state.getTeamId(), targetLane);
		var world = resolveWorld(captain, systemConfig.world);
		var targetPos = new Vec3d(targetSpawn.x, targetSpawn.y, targetSpawn.z);
		for (var player : buffTargets) {
			if (teleportTargets.contains(player)) {
				spawnLineParticles(world, player.getPos(), targetPos, ParticleTypes.END_ROD);
				player.teleportTo(new TeleportTarget(world, targetPos, Vec3d.ZERO, targetSpawn.yaw, targetSpawn.pitch, TeleportTarget.NO_OP));
			}
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 30, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 30, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 30, 1));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 30, 1));
			fullHealAndTrack(captain, player);
		}
		captainManaService.triggerSkillCooldown(captain.getUuid(), COOLDOWN_SECONDS);
		recordCaptainSkillUse(FactionId.VILLAGER);
		playGlobal(world, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 10.0f, 1.0f);
		var detailMessage = textConfig().captainVillagerSuccessMessage.replace("{lane}", laneLabel(targetLane));
		captain.sendMessage(textTemplateResolver.format(detailMessage), false);
		broadcastCaptainSkill(captain, detailMessage);
		playEventSuccess(captain);
		return true;
	}
}
