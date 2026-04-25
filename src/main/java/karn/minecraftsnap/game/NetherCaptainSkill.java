package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Random;

final class NetherCaptainSkill extends AbstractCaptainFactionSkillStrategy {
	static final int COOLDOWN_SECONDS = 100;
	static final long PORTAL_DURATION_TICKS = 20L * 30L;

	NetherCaptainSkill(
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
		return FactionId.NETHER;
	}

	@Override
	public boolean use(ServerPlayerEntity captain, PlayerMatchState state, SystemConfig systemConfig) {
		var laneId = UnitSpawnService.nearestLaneForCaptain(captain, systemConfig);
		if (!matchManager.isLaneRevealed(laneId)) {
			captain.sendMessage(textTemplateResolver.format(textConfig().closedLaneMessage), true);
			playUiDeny(captain);
			return false;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		captainState.setNetherPortalLaneId(laneId);
		captainState.setNetherPortalEndTick(matchManager.getServerTicks() + PORTAL_DURATION_TICKS);
		captainState.setNetherPortalLastProcessedSecond(-1);
		captainState.setCurrentMana(captainState.getMaxMana());
		captainManaService.triggerSkillCooldown(captain.getUuid(), COOLDOWN_SECONDS);
		recordCaptainSkillUse(FactionId.NETHER);
		var world = resolveWorld(captain, systemConfig.world);
		playGlobal(world, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.MASTER, 1.0f, 1.0f);
		playGlobal(world, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.MASTER, 1.0f, 2.0f);
		spawnPortalBurst(world, UnitSpawnService.safeUnitSpawn(systemConfig, state.getTeamId(), laneId));
		var detailMessage = textConfig().captainNetherPortalSuccessMessage.replace("{lane}", laneLabel(laneId));
		captain.sendMessage(textTemplateResolver.format(detailMessage), false);
		broadcastCaptainSkill(captain, detailMessage);
		playEventSuccess(captain);
		return true;
	}

	@Override
	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		for (var captainId : captainIdsByFaction(FactionId.NETHER)) {
			var captain = server.getPlayerManager().getPlayer(captainId);
			if (captain == null) {
				continue;
			}
			var state = matchManager.getPlayerState(captainId);
			var captainState = captainManaService.getOrCreate(captainId);
			if (!isPortalActive(captainState, matchManager.getServerTicks())) {
				captainState.clearNetherPortal();
				continue;
			}
			var laneId = captainState.getNetherPortalLaneId();
			if (laneId == null) {
				continue;
			}
			var world = resolveWorld(captain, systemConfig.world);
			var spawn = UnitSpawnService.safeUnitSpawn(systemConfig, state.getTeamId(), laneId);
			if (matchManager.getServerTicks() % 10L == 0L) {
				spawnPortalBurst(world, spawn);
			}
			var currentSecond = (int) (matchManager.getServerTicks() / 20L);
			if (captainState.getNetherPortalLastProcessedSecond() == currentSecond) {
				continue;
			}
			captainState.setNetherPortalLastProcessedSecond(currentSecond);
			for (var ally : laneUnits(state.getTeamId(), laneId)) {
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0));
				ally.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0));
			}
		}
	}

	@Override
	public void clearRuntimeState(MinecraftServer server, SystemConfig systemConfig) {
		for (var captainId : captainIdsByFaction(FactionId.NETHER)) {
			captainManaService.getOrCreate(captainId).clearNetherPortal();
		}
	}

	private boolean isPortalActive(CaptainState state, long serverTicks) {
		return state.getNetherPortalLaneId() != null && state.getNetherPortalEndTick() > serverTicks;
	}
}
