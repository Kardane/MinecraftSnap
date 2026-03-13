package karn.minecraftsnap.game;

public class CaptureProgress {
	private TeamId teamId;
	private int ticks;
	private boolean contested;

	public TeamId getTeamId() {
		return teamId;
	}

	public void start(TeamId teamId) {
		this.teamId = teamId;
		this.ticks = 1;
		this.contested = false;
	}

	public void increment() {
		this.ticks++;
		this.contested = false;
	}

	public int getSeconds() {
		if (ticks <= 0) {
			return 0;
		}
		return (ticks + 19) / 20;
	}

	public int getTicks() {
		return ticks;
	}

	public boolean isContested() {
		return contested;
	}

	public void setContested() {
		this.contested = true;
		this.teamId = null;
		this.ticks = 0;
	}

	public void reset() {
		this.teamId = null;
		this.ticks = 0;
		this.contested = false;
	}
}
