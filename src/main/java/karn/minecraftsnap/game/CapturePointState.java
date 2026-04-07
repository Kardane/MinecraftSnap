package karn.minecraftsnap.game;

public class CapturePointState {
	private final LaneId laneId;
	private final CaptureProgress progress = new CaptureProgress();
	private CaptureOwner owner = CaptureOwner.NEUTRAL;
	private boolean firstCapturePending = true;

	public CapturePointState(LaneId laneId) {
		this.laneId = laneId;
	}

	public LaneId getLaneId() {
		return laneId;
	}

	public CaptureOwner getOwner() {
		return owner;
	}

	public CaptureProgress getProgress() {
		return progress;
	}

	public boolean update(TeamId occupyingTeam, boolean contested, int captureStepSeconds) {
		if (contested) {
			progress.setContested();
			return false;
		}

		if (occupyingTeam == null) {
			progress.reset();
			return false;
		}

		if (owner == CaptureOwner.fromTeam(occupyingTeam)) {
			progress.reset();
			return false;
		}

		if (progress.getTeamId() != occupyingTeam) {
			progress.start(occupyingTeam);
			return false;
		}

		progress.increment();
		if (progress.getTicks() < captureStepSeconds * 20) {
			return false;
		}

		advanceOwner(occupyingTeam);
		progress.reset();
		return true;
	}

	public void reset() {
		owner = CaptureOwner.NEUTRAL;
		firstCapturePending = true;
		progress.reset();
	}

	public int requiredCaptureSeconds(int captureStepSeconds, int firstCaptureStepSeconds) {
		if (!firstCapturePending) {
			return captureStepSeconds;
		}
		return laneId == LaneId.LANE_1 ? firstCaptureStepSeconds : captureStepSeconds;
	}

	private void advanceOwner(TeamId teamId) {
		if (owner == CaptureOwner.NEUTRAL) {
			owner = CaptureOwner.fromTeam(teamId);
			firstCapturePending = false;
			return;
		}

		if ((owner == CaptureOwner.RED && teamId == TeamId.BLUE)
			|| (owner == CaptureOwner.BLUE && teamId == TeamId.RED)) {
			owner = CaptureOwner.NEUTRAL;
		}
	}
}
