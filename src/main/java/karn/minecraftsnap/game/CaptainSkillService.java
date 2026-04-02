package karn.minecraftsnap.game;

import karn.minecraftsnap.audio.UiSoundService;
import karn.minecraftsnap.config.SystemConfig;
import karn.minecraftsnap.lane.LaneRuntimeRegistry;
import karn.minecraftsnap.ui.CaptainWeatherGuiService;
import karn.minecraftsnap.util.TextTemplateResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Random;

public class CaptainSkillService {
	static final int SKILL_UNLOCK_SECONDS = 60;

	private final MatchManager matchManager;
	private final CaptainManaService captainManaService;
	private final TextTemplateResolver textTemplateResolver;
	private final UiSoundService uiSoundService;
	private final CaptainFactionSkillRegistry strategyRegistry;

	public CaptainSkillService(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService
	) {
		this(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, new Random(), null);
	}

	CaptainSkillService(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random
	) {
		this(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random, null);
	}

	CaptainSkillService(
		MatchManager matchManager,
		LaneRuntimeRegistry laneRuntimeRegistry,
		CaptainWeatherGuiService captainWeatherGuiService,
		CaptainManaService captainManaService,
		TextTemplateResolver textTemplateResolver,
		UiSoundService uiSoundService,
		Random random,
		CaptainFactionSkillRegistry strategyRegistry
	) {
		this.matchManager = matchManager;
		this.captainManaService = captainManaService;
		this.textTemplateResolver = textTemplateResolver;
		this.uiSoundService = uiSoundService;
		this.strategyRegistry = strategyRegistry != null
			? strategyRegistry
			: new CaptainFactionSkillRegistry(List.of(
				new VillagerCaptainSkill(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random),
				new MonsterCaptainSkill(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random),
				new NetherCaptainSkill(matchManager, laneRuntimeRegistry, captainWeatherGuiService, captainManaService, textTemplateResolver, uiSoundService, random)
			));
	}

	public boolean useCaptainSkill(ServerPlayerEntity captain, SystemConfig systemConfig) {
		if (captain == null || systemConfig == null) {
			return false;
		}
		var state = matchManager.getPlayerState(captain.getUuid());
		if (!state.isCaptain() || state.getFactionId() == null) {
			return false;
		}
		if (!isSkillUnlocked(matchManager.getElapsedSeconds())) {
			captain.sendMessage(textTemplateResolver.format("&c사령관 스킬은 게임 시작 1분 후부터 사용할 수 있습니다."), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		var captainState = captainManaService.getOrCreate(captain.getUuid());
		if (captainState.getSkillCooldownSeconds() > 0) {
			captain.sendMessage(textTemplateResolver.format(textConfig().captainSkillCooldownMessage.replace("{seconds}", Integer.toString(captainState.getSkillCooldownSeconds()))), true);
			if (uiSoundService != null) {
				uiSoundService.playUiDeny(captain);
			}
			return false;
		}
		var strategy = strategyRegistry.strategyFor(state.getFactionId());
		return strategy != null && strategy.use(captain, state, systemConfig);
	}

	public void tick(MinecraftServer server, SystemConfig systemConfig) {
		if (server == null || systemConfig == null) {
			return;
		}
		if (!isCaptainSkillPhase(matchManager.getPhase())) {
			clearRuntimeStates(server, systemConfig);
			return;
		}
		for (var strategy : strategyRegistry.all()) {
			strategy.tick(server, systemConfig);
		}
	}

	static int skillCooldownFor(FactionId factionId) {
		return switch (factionId) {
			case VILLAGER -> VillagerCaptainSkill.COOLDOWN_SECONDS;
			case MONSTER -> MonsterCaptainSkill.COOLDOWN_SECONDS;
			case NETHER -> NetherCaptainSkill.COOLDOWN_SECONDS;
		};
	}

	public static boolean isSkillUnlocked(int elapsedSeconds) {
		return elapsedSeconds >= SKILL_UNLOCK_SECONDS;
	}

	private karn.minecraftsnap.config.TextConfigFile textConfig() {
		var mod = karn.minecraftsnap.MinecraftSnap.getInstance();
		return mod == null ? new karn.minecraftsnap.config.TextConfigFile() : mod.getTextConfig();
	}

	private void clearRuntimeStates(MinecraftServer server, SystemConfig systemConfig) {
		for (var strategy : strategyRegistry.all()) {
			strategy.clearRuntimeState(server, systemConfig);
		}
	}

	private boolean isCaptainSkillPhase(MatchPhase phase) {
		return phase == MatchPhase.GAME_START || phase == MatchPhase.GAME_RUNNING;
	}
}
