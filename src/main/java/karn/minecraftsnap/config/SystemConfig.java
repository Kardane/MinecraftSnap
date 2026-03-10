package karn.minecraftsnap.config;

public class SystemConfig {
	public String prefix = "&7[&6!&7]&f ";
	public int gameDurationSeconds = 9 * 60;
	public BossBarConfig bossBar = new BossBarConfig();
	public MusicLoopConfig lobbyMusic = MusicLoopConfig.create("&6로비 브금", "minecraft:music.menu", 20 * 70);
	public MusicLoopConfig gameMusic = MusicLoopConfig.create("&c경기 브금", "minecraft:music.dragon", 20 * 110);
	public TeamChatConfig teamChat = new TeamChatConfig();
	public CaptureConfig capture = new CaptureConfig();
	public LobbyConfig lobby = new LobbyConfig();
	public GameStartConfig gameStart = new GameStartConfig();
	public BiomeRevealConfig biomeReveal = new BiomeRevealConfig();

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
		public CapturePointConfig lane1 = CapturePointConfig.create("1번 라인", 0.0, 64.0, 0.0);
		public CapturePointConfig lane2 = CapturePointConfig.create("2번 라인", 20.0, 64.0, 0.0);
		public CapturePointConfig lane3 = CapturePointConfig.create("3번 라인", 40.0, 64.0, 0.0);
	}

	public static class LobbyConfig {
		public String world = "minecraft:overworld";
		public double spawnX = 0.0;
		public double spawnY = 64.0;
		public double spawnZ = 0.0;
		public float spawnYaw = 0.0f;
		public float spawnPitch = 0.0f;
		public int factionSelectDelaySeconds = 5;
		public int factionSelectDurationSeconds = 15;
		public int gameStartDelaySeconds = 0;
	}

	public static class GameStartConfig {
		public int waitSeconds = 15;
		public boolean allowShiftF = true;
		public PositionConfig captainSpawn = PositionConfig.create("minecraft:overworld", 0.0, 64.0, 10.0);
		public PositionConfig unitSpawn = PositionConfig.create("minecraft:overworld", 0.0, 64.0, -10.0);
	}

	public static class BiomeRevealConfig {
		public int lane1RevealSecond = 0;
		public int lane2RevealSecond = 180;
		public int lane3RevealSecond = 360;
		public String messageTemplate = "&a{lane}&f 라인 바이옴 공개";
		public String hiddenWorldKey = "minecraft:the_void";
	}

	public static class PositionConfig {
		public String world = "minecraft:overworld";
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;

		public static PositionConfig create(String world, double x, double y, double z) {
			var config = new PositionConfig();
			config.world = world;
			config.x = x;
			config.y = y;
			config.z = z;
			return config;
		}
	}

	public static class CapturePointConfig {
		public boolean enabled = false;
		public String world = "world";
		public String label = "";
		public double x;
		public double y;
		public double z;
		public double radius = 4.0;

		public static CapturePointConfig create(String label, double x, double y, double z) {
			var config = new CapturePointConfig();
			config.label = label;
			config.x = x;
			config.y = y;
			config.z = z;
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
		if (gameStart.captainSpawn == null) {
			gameStart.captainSpawn = PositionConfig.create("minecraft:overworld", 0.0, 64.0, 10.0);
		}
		if (gameStart.unitSpawn == null) {
			gameStart.unitSpawn = PositionConfig.create("minecraft:overworld", 0.0, 64.0, -10.0);
		}
		if (capture.lane1 == null) {
			capture.lane1 = CapturePointConfig.create("1번 라인", 0.0, 64.0, 0.0);
		}
		if (capture.lane2 == null) {
			capture.lane2 = CapturePointConfig.create("2번 라인", 20.0, 64.0, 0.0);
		}
		if (capture.lane3 == null) {
			capture.lane3 = CapturePointConfig.create("3번 라인", 40.0, 64.0, 0.0);
		}
	}
}
