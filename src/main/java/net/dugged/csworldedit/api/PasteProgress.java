package net.dugged.csworldedit.api;

public final class PasteProgress {
	private final String sessionId;
	private final PasteState state;
	private final CancelReason cancelReason;
	private final long processed;
	private final long total;
	private final int queuedChanges;
	private final int nbtOmitted;

	PasteProgress(String sessionId, PasteState state, CancelReason cancelReason, long processed, long total,
				  int queuedChanges, int nbtOmitted) {
		this.sessionId = sessionId;
		this.state = state;
		this.cancelReason = cancelReason;
		this.processed = processed;
		this.total = total;
		this.queuedChanges = queuedChanges;
		this.nbtOmitted = nbtOmitted;
	}

	public String sessionId() {
		return sessionId;
	}

	public String undoGroup() {
		return sessionId;
	}

	public PasteState state() {
		return state;
	}

	public CancelReason cancelReason() {
		return cancelReason;
	}

	public long processed() {
		return processed;
	}

	public long total() {
		return total;
	}

	public int queuedChanges() {
		return queuedChanges;
	}

	public int nbtOmitted() {
		return nbtOmitted;
	}

	public boolean isFinished() {
		return state == PasteState.COMPLETED || state == PasteState.CANCELLED || state == PasteState.FAILED;
	}
}
