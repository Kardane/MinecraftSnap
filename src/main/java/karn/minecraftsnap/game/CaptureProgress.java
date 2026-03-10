package karn.minecraftsnap.game;

public class CaptureProgress {
	private TeamId teamId;
	private int seconds;
	private boolean contested;

	public TeamId getTeamId() {
		return teamId;
	}

	public void start(TeamId teamId) {
		this.teamId = teamId;
		this.seconds = 1;
		this.contested = false;
	}

	public void increment() {
		this.seconds++;
		this.contested = false;
	}

	public int getSeconds() {
		return seconds;
	}

	public boolean isContested() {
		return contested;
	}

	public void setContested() {
		this.contested = true;
		this.teamId = null;
		this.seconds = 0;
	}

	public void reset() {
		this.teamId = null;
		this.seconds = 0;
		this.contested = false;
	}
}
