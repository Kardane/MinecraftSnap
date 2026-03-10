package karn.minecraftsnap.config;

public class SystemConfig {
	public String prefix = "&7[&6!&7]&f ";
	public int gameDurationSeconds = 9 * 60;
	public BossBarConfig bossBar = new BossBarConfig();
	public MusicLoopConfig lobbyMusic = MusicLoopConfig.create("&6로비 브금", "minecraft:music.menu", 20 * 70);
	public MusicLoopConfig gameMusic = MusicLoopConfig.create("&c경기 브금", "minecraft:music.dragon", 20 * 110);
	public TeamChatConfig teamChat = new TeamChatConfig();

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
}
