package net.dugged.csworldedit.api;

public final class PasteOptions {
	public static final PasteOptions DEFAULT = new PasteOptions(false, PasteApi.MAX_QUEUED_CHANGES);

	private final boolean pasteAir;
	private final int maxQueuedChanges;

	public PasteOptions(boolean pasteAir, int maxQueuedChanges) {
		this.pasteAir = pasteAir;
		this.maxQueuedChanges = Math.max(1, Math.min(PasteApi.MAX_QUEUED_CHANGES, maxQueuedChanges));
	}

	public boolean pasteAir() {
		return pasteAir;
	}

	public int maxQueuedChanges() {
		return maxQueuedChanges;
	}
}
