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
import java.util.UUID;

final class MonsterCaptainSkill extends AbstractCaptainFactionSkillStrategy {
	static final int COOLDOWN_SECONDS = 120;
	static final long RAIN_DURATION_TICKS = 20L * 120L;
	static final long THUNDER_DURATION_TICKS = 20L * 40L;
	static final float THUNDER_HEAL_AMOUNT = 1.0F;
	static final int THUNDER_SPEED_AMPLIFIER = 0;
	static final int THUNDER_LIGHTNING_INTERVAL_SECONDS = 5;

	MonsterCaptainSkill(
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
		return FactionId.MONSTER;
	}

	@Override
	public boolean use(ServerPlayerEntity captain, PlayerMatchState state, SystemConfig systemConfig) {
		return activateWeather(captain, CaptainWeatherGuiService.WeatherOption.THUNDER, systemConfig);
	}

	@Override
	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		var activeCaptainId = activeWeatherCaptainId();
		if (activeCaptainId == null) {
			return;
		}
		var captain = server.getPlayerManager().getPlayer(activeCaptainId);
		if (captain == null) {
			return;
		}
		var captainState = captainManaService.getOrCreate(activeCaptainId);
		var option = parseWeather(captainState.getMonsterWeatherType());
		var world = resolveWorld(captain, systemConfig.world);
		if (option != CaptainWeatherGuiService.WeatherOption.CLEAR
			&& captainState.getMonsterWeatherEndTick() > 0L
			&& matchManager.getServerTicks() >= captainState.getMonsterWeatherEndTick()) {
			captainState.clearMonsterWeather();
			applyWorldWeather(world, CaptainWeatherGuiService.WeatherOption.CLEAR);
			return;
		}
		var currentSecond = (int) (matchManager.getServerTicks() / 20L);
		if (captainState.getMonsterWeatherLastProcessedSecond() == currentSecond) {
			return;
		}
		captainState.setMonsterWeatherLastProcessedSecond(currentSecond);
		var allies = teamUnits(matchManager.getPlayerState(activeCaptainId).getTeamId());
		if (option == CaptainWeatherGuiService.WeatherOption.THUNDER) {
			if (currentSecond % 3 == 0) {
				for (var ally : allies) {
					healAndTrack(captain, ally, THUNDER_HEAL_AMOUNT);
					ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 5, THUNDER_SPEED_AMPLIFIER));
				}
			}
			if (shouldTriggerThunderLightning(currentSecond)) {
				var target = selectLightningTarget(matchManager.getPlayerState(activeCaptainId).getTeamId(), world);
				if (target != null) {
					spawnLightningStrike(world, target);
				}
			}
		}
	}

	@Override
	public void clearRuntimeState(MinecraftServer server, SystemConfig systemConfig) {
		for (var captainId : captainIdsByFaction(FactionId.MONSTER)) {
			captainManaService.getOrCreate(captainId).clearMonsterWeather();
		}
		applyWorldWeather(resolveWorld(server, systemConfig.world), CaptainWeatherGuiService.WeatherOption.CLEAR);
	}

	private boolean activateWeather(ServerPlayerEntity captain, CaptainWeatherGuiService.WeatherOption option, SystemConfig systemConfig) {
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() != FactionId.MONSTER) {
			return false;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getSkillCooldownSeconds() > 0) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillCooldownMessage.replace("{seconds}", Integer.toString(captainState.getSkillCooldownSeconds()))), true);
			playUiDeny(captain);
			return false;
		}
		clearRuntimeState(captain.getServer(), systemConfig);
		captainState.setMonsterWeatherType(option.name());
		captainState.setMonsterWeatherLastProcessedSecond(-1);
		captainState.setMonsterWeatherEndTick(switch (option) {
			case CLEAR -> 0L;
			case RAIN -> matchManager.getServerTicks() + RAIN_DURATION_TICKS;
			case THUNDER -> matchManager.getServerTicks() + THUNDER_DURATION_TICKS;
		});
		captainManaService.triggerSkillCooldown(captain.getUuid(), COOLDOWN_SECONDS);
		recordCaptainSkillUse(FactionId.MONSTER);
		var world = resolveWorld(captain, systemConfig.world);
		applyWorldWeather(world, option);
		playGlobal(world, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 3.0f, 1.0f);
		var detailMessage = textConfig().captainMonsterWeatherSuccessMessage.replace("{weather}", weatherLabel(option));
		captain.sendMessage(textTemplateResolver.format(detailMessage), false);
		broadcastCaptainSkill(captain, detailMessage);
		playEventSuccess(captain);
		return true;
	}

	static boolean shouldTriggerThunderLightning(int currentSecond) {
		return currentSecond > 0 && currentSecond % THUNDER_LIGHTNING_INTERVAL_SECONDS == 0;
	}

	private UUID activeWeatherCaptainId() {
		return captainIdsByFaction(FactionId.MONSTER).stream()
			.filter(captainId -> !captainManaService.getOrCreate(captainId).getMonsterWeatherType().isBlank())
			.findFirst()
			.orElse(null);
	}

	private CaptainWeatherGuiService.WeatherOption currentWeatherOption() {
		var captainId = activeWeatherCaptainId();
		if (captainId == null) {
			return CaptainWeatherGuiService.WeatherOption.CLEAR;
		}
		return parseWeather(captainManaService.getOrCreate(captainId).getMonsterWeatherType());
	}

	private int currentWeatherRemainingSeconds() {
		var captainId = activeWeatherCaptainId();
		if (captainId == null) {
			return 0;
		}
		var endTick = captainManaService.getOrCreate(captainId).getMonsterWeatherEndTick();
		if (endTick <= 0L) {
			return 0;
		}
		return Math.max(0, (int) Math.ceil((endTick - matchManager.getServerTicks()) / 20.0D));
	}

	private CaptainWeatherGuiService.WeatherOption parseWeather(String value) {
		try {
			return CaptainWeatherGuiService.WeatherOption.valueOf(value);
		} catch (Exception ignored) {
			return CaptainWeatherGuiService.WeatherOption.CLEAR;
		}
	}

	private ServerPlayerEntity selectLightningTarget(TeamId teamId, net.minecraft.server.world.ServerWorld world) {
		if (teamId == null || world == null) {
			return null;
		}
		var targets = enemyUnits(teamId).stream()
			.filter(target -> target.getWorld() == world && world.isSkyVisible(target.getBlockPos()))
			.toList();
		if (targets.isEmpty()) {
			return null;
		}
		return targets.get(random.nextInt(targets.size()));
	}

	private void applyWorldWeather(net.minecraft.server.world.ServerWorld world, CaptainWeatherGuiService.WeatherOption option) {
		if (world == null) {
			return;
		}
		switch (option) {
			case CLEAR -> world.setWeather(20 * 300, 0, false, false);
			case RAIN -> world.setWeather(0, (int) RAIN_DURATION_TICKS, true, false);
			case THUNDER -> world.setWeather(0, (int) THUNDER_DURATION_TICKS, true, true);
		}
	}

	private String weatherLabel(CaptainWeatherGuiService.WeatherOption option) {
		var textConfig = textConfig();
		return switch (option) {
			case CLEAR -> textConfig.captainMonsterWeatherClearName;
			case RAIN -> textConfig.captainMonsterWeatherRainName;
			case THUNDER -> textConfig.captainMonsterWeatherThunderName;
		};
	}
}
