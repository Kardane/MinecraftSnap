package karn.minecraftsnap.config;

public class SystemConfig {
	public String prefix = "&7[&6!&7]&f ";
	public String world = "minecraft:overworld";
	public int gameDurationSeconds = 9 * 60;
	public BossBarConfig bossBar = new BossBarConfig();
	public MusicLoopConfig lobbyMusic = MusicLoopConfig.create("&6로비 브금", "minecraft:music.menu", 20 * 70);
	public MusicLoopConfig gameMusic = MusicLoopConfig.create("&c경기 브금", "minecraft:music.dragon", 20 * 110);
	public TeamChatConfig teamChat = new TeamChatConfig();
	public CaptureConfig capture = new CaptureConfig();
	public LobbyConfig lobby = new LobbyConfig();
	public GameStartConfig gameStart = new GameStartConfig();
	public BiomeRevealConfig biomeReveal = new BiomeRevealConfig();
	public InGameConfig inGame = new InGameConfig();
	public AdvanceConfig advance = new AdvanceConfig();
	public GameEndConfig gameEnd = new GameEndConfig();
	public DisplayConfig display = new DisplayConfig();
	public AnnouncementConfig announcements = new AnnouncementConfig();

	public static class BossBarConfig {
		public String template = "&c레드 {red_score} &8| &f남은 시간 {time} &8| &9블루 {blue_score}";
		public String color = "WHITE";
		public String style = "PROGRESS";
	}

	public static class MusicLoopConfig {
		public boolean enabled = true;
		public String title = "";
		public String soundId = "minecraft:music.menu";
		public int intervalTicks = 20 * 70;
		public float volume = 1.0f;
		public float pitch = 1.0f;
		public String category = "MUSIC";

		public static MusicLoopConfig create(String title, String soundId, int intervalTicks) {
			var config = new MusicLoopConfig();
			config.title = title;
			config.soundId = soundId;
			config.intervalTicks = intervalTicks;
			return config;
		}
	}

	public static class TeamChatConfig {
		public String format = "&8[&a팀&8] &7[{team} / {role}] &f{player}&7: {message}";
		public String noTeamMessage = "&c팀 채팅은 팀 배정 이후에만 사용할 수 있음";
		public String emptyMessage = "&c팀 채팅 내용이 비어있음";
	}

	public static class CaptureConfig {
		public int captureStepSeconds = 5;
		public int scoreIntervalTicks = 20;
		public int allPointsHoldSeconds = 30;
		public CaptureRegionConfig lane1 = CaptureRegionConfig.create("1번 라인", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);
		public CaptureRegionConfig lane2 = CaptureRegionConfig.create("2번 라인", 16.0, 60.0, -4.0, 24.0, 68.0, 4.0);
		public CaptureRegionConfig lane3 = CaptureRegionConfig.create("3번 라인", 36.0, 60.0, -4.0, 44.0, 68.0, 4.0);
	}

	public static class LobbyConfig {
		public double spawnX = 0.0;
		public double spawnY = 64.0;
		public double spawnZ = 0.0;
		public float spawnYaw = 0.0f;
		public float spawnPitch = 0.0f;
		public int factionSelectDelaySeconds = 5;
		public int factionSelectDurationSeconds = 15;
		public int gameStartDelaySeconds = 0;
		public String factionSelectBossBarTemplate = "&6팩션 선택 남은 시간 &f{time}";
	}

	public static class GameStartConfig {
		public int waitSeconds = 15;
		public boolean allowShiftF = true;
		public PositionConfig redCaptainSpawn = PositionConfig.create(-10.0, 64.0, 10.0);
		public PositionConfig blueCaptainSpawn = PositionConfig.create(10.0, 64.0, 10.0);
		public PositionConfig redUnitSpawn = PositionConfig.create(-10.0, 64.0, -10.0);
		public PositionConfig blueUnitSpawn = PositionConfig.create(10.0, 64.0, -10.0);

		public PositionConfig captainSpawnFor(karn.minecraftsnap.game.TeamId teamId) {
			return teamId == karn.minecraftsnap.game.TeamId.BLUE ? blueCaptainSpawn : redCaptainSpawn;
		}

		public PositionConfig unitSpawnFor(karn.minecraftsnap.game.TeamId teamId) {
			return teamId == karn.minecraftsnap.game.TeamId.BLUE ? blueUnitSpawn : redUnitSpawn;
		}
	}

	public static class BiomeRevealConfig {
		public int lane1RevealSecond = 0;
		public int lane2RevealSecond = 180;
		public int lane3RevealSecond = 360;
		public String messageTemplate = "&a{lane}&f 라인 바이옴 공개";
		public String hiddenWorldKey = "minecraft:the_void";
		public String assignmentPolicy = "unique_random";
		public boolean applyHiddenVoidBiome = true;
		public boolean restoreOriginalBiomesOnEnd = true;
	}

	public static class GameEndConfig {
		public int finalTickRate = 10;
		public int restoreTickRate = 20;
		public int returnToLobbyDelaySeconds = 5;
		public int winnerGlowSeconds = 5;
		public String titleTemplate = "&6승리: &f{winner}";
		public String drawTitleTemplate = "&e무승부";
	}

	public static class PositionConfig {
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;

		public static PositionConfig create(double x, double y, double z) {
			var config = new PositionConfig();
			config.x = x;
			config.y = y;
			config.z = z;
			return config;
		}
	}

	public static class LaneRegionConfig {
		public double minX;
		public double minY;
		public double minZ;
		public double maxX;
		public double maxY;
		public double maxZ;

		public static LaneRegionConfig create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			var config = new LaneRegionConfig();
			config.minX = minX;
			config.minY = minY;
			config.minZ = minZ;
			config.maxX = maxX;
			config.maxY = maxY;
			config.maxZ = maxZ;
			return config;
		}
	}

	public static class InGameConfig {
		public String closedLaneMessage = "&c아직 공개되지 않은 라인임";
		public int laneWarningCooldownTicks = 40;
		public double captainMinY = 0.0;
		public float captainFlySpeed = 3.0f;
		public float defaultFlySpeed = 0.05f;
		public LaneRegionConfig lane1Region = LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		public LaneRegionConfig lane2Region = LaneRegionConfig.create(12.0, 0.0, -8.0, 28.0, 320.0, 8.0);
		public LaneRegionConfig lane3Region = LaneRegionConfig.create(32.0, 0.0, -8.0, 48.0, 320.0, 8.0);
	}

	public static class DisplayConfig {
		public String ladderPrefixFormat = "[{ladder}] ";
		public String captainStar = "★ ";
	}

	public static class AnnouncementConfig {
		public String lobbyPhaseMessage = "&e로비로 복귀";
		public String teamSelectPhaseMessage = "&a팀 배정 시작";
		public String factionSelectPhaseMessage = "&6팩션 선택 시작";
		public String gameStartPhaseMessage = "&b게임 준비 시작";
		public String gameRunningPhaseMessage = "&c게임 시작";
		public String gameEndPhaseMessage = "&d게임 종료";
		public String factionSelectionMessage = "&f{team} 팀 사령관이 &6{faction} &f팩션 선택";
		public String villagerFactionName = "주민";
		public String monsterFactionName = "몬스터";
		public String netherFactionName = "네더";
	}

	public static class AdvanceConfig {
		public String notAvailableMessage = "&c아직 전직할 수 없음";
		public String readyMessage = "&a전직 가능 상태가 됨";
		public java.util.List<AdvanceConditionConfig> conditions = new java.util.ArrayList<>();
	}

	public static class AdvanceConditionConfig {
		public String unitId = "";
		public java.util.List<String> biomes = new java.util.ArrayList<>();
		public java.util.List<String> weathers = new java.util.ArrayList<>();
		public int requiredExp = 15;
		public int requiredSeconds = 0;
		public String resultUnitId = "";
	}

	public static class CaptureRegionConfig {
		public boolean enabled = false;
		public String label = "";
		public double minX;
		public double minY;
		public double minZ;
		public double maxX;
		public double maxY;
		public double maxZ;

		public static CaptureRegionConfig create(String label, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			var config = new CaptureRegionConfig();
			config.label = label;
			config.minX = minX;
			config.minY = minY;
			config.minZ = minZ;
			config.maxX = maxX;
			config.maxY = maxY;
			config.maxZ = maxZ;
			return config;
		}
	}

	public void normalize() {
		if (bossBar == null) {
			bossBar = new BossBarConfig();
		}
		if (lobbyMusic == null) {
			lobbyMusic = MusicLoopConfig.create("&6로비 브금", "minecraft:music.menu", 20 * 70);
		}
		if (gameMusic == null) {
			gameMusic = MusicLoopConfig.create("&c경기 브금", "minecraft:music.dragon", 20 * 110);
		}
		if (teamChat == null) {
			teamChat = new TeamChatConfig();
		}
		if (capture == null) {
			capture = new CaptureConfig();
		}
		if (lobby == null) {
			lobby = new LobbyConfig();
		}
		if (gameStart == null) {
			gameStart = new GameStartConfig();
		}
		if (biomeReveal == null) {
			biomeReveal = new BiomeRevealConfig();
		}
		if (inGame == null) {
			inGame = new InGameConfig();
		}
		if (advance == null) {
			advance = new AdvanceConfig();
		}
		if (gameEnd == null) {
			gameEnd = new GameEndConfig();
		}
		if (display == null) {
			display = new DisplayConfig();
		}
		if (announcements == null) {
			announcements = new AnnouncementConfig();
		}
		if (gameStart.redCaptainSpawn == null) {
			gameStart.redCaptainSpawn = PositionConfig.create(-10.0, 64.0, 10.0);
		}
		if (gameStart.blueCaptainSpawn == null) {
			gameStart.blueCaptainSpawn = PositionConfig.create(10.0, 64.0, 10.0);
		}
		if (gameStart.redUnitSpawn == null) {
			gameStart.redUnitSpawn = PositionConfig.create(-10.0, 64.0, -10.0);
		}
		if (gameStart.blueUnitSpawn == null) {
			gameStart.blueUnitSpawn = PositionConfig.create(10.0, 64.0, -10.0);
		}
		if (inGame.lane1Region == null) {
			inGame.lane1Region = LaneRegionConfig.create(-8.0, 0.0, -8.0, 8.0, 320.0, 8.0);
		}
		if (inGame.lane2Region == null) {
			inGame.lane2Region = LaneRegionConfig.create(12.0, 0.0, -8.0, 28.0, 320.0, 8.0);
		}
		if (inGame.lane3Region == null) {
			inGame.lane3Region = LaneRegionConfig.create(32.0, 0.0, -8.0, 48.0, 320.0, 8.0);
		}
		if (capture.lane1 == null) {
			capture.lane1 = CaptureRegionConfig.create("1번 라인", -4.0, 60.0, -4.0, 4.0, 68.0, 4.0);
		}
		if (capture.lane2 == null) {
			capture.lane2 = CaptureRegionConfig.create("2번 라인", 16.0, 60.0, -4.0, 24.0, 68.0, 4.0);
		}
		if (capture.lane3 == null) {
			capture.lane3 = CaptureRegionConfig.create("3번 라인", 36.0, 60.0, -4.0, 44.0, 68.0, 4.0);
		}
		if (advance.conditions == null || advance.conditions.isEmpty()) {
			advance.conditions = defaultAdvanceConditions();
		} else {
			for (var condition : advance.conditions) {
				if (condition.biomes == null) {
					condition.biomes = new java.util.ArrayList<>();
				}
				if (condition.weathers == null) {
					condition.weathers = new java.util.ArrayList<>();
				}
				if (condition.requiredExp <= 0) {
					condition.requiredExp = condition.requiredSeconds > 0 ? condition.requiredSeconds : 15;
				}
				condition.requiredSeconds = 0;
			}
		}
	}

	private java.util.List<AdvanceConditionConfig> defaultAdvanceConditions() {
		var zombie = new AdvanceConditionConfig();
		zombie.unitId = "zombie";
		zombie.biomes = java.util.List.of("minecraft:swamp", "minecraft:mangrove_swamp");
		zombie.weathers = java.util.List.of("rain", "thunder");
		zombie.requiredExp = 15;
		zombie.resultUnitId = "zombie_veteran";

		var skeleton = new AdvanceConditionConfig();
		skeleton.unitId = "skeleton";
		skeleton.biomes = java.util.List.of("minecraft:snowy_plains", "minecraft:snowy_taiga");
		skeleton.weathers = java.util.List.of("clear", "rain");
		skeleton.requiredExp = 15;
		skeleton.resultUnitId = "skeleton_sniper";

		var slime = new AdvanceConditionConfig();
		slime.unitId = "slime";
		slime.biomes = java.util.List.of("minecraft:swamp", "minecraft:mangrove_swamp");
		slime.weathers = java.util.List.of("clear", "rain", "thunder");
		slime.requiredExp = 12;
		slime.resultUnitId = "slime_brute";

		var creeper = new AdvanceConditionConfig();
		creeper.unitId = "creeper";
		creeper.biomes = java.util.List.of("minecraft:plains", "minecraft:forest", "minecraft:dark_forest");
		creeper.weathers = java.util.List.of("thunder");
		creeper.requiredExp = 10;
		creeper.resultUnitId = "charged_creeper";

		return new java.util.ArrayList<>(java.util.List.of(zombie, skeleton, slime, creeper));
	}
}
