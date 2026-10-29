package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.sp.SPEditJob;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.Set;

public abstract class TaskChunk extends AbstractTask {

	private SPEditJob directJob;
	private boolean directSubmitted;
	private boolean cancelled;

	public abstract int processChunk(ChunkPos pos, int maxPackets);

	public abstract int getTotalOperations();

	public abstract int getProcessedOperations();

	public abstract Set<ChunkPos> getPendingChunks();

	protected final boolean useDirectJob(SPEditJob job) {
		this.directJob = job;
		return job != null;
	}

	public final boolean isDirectMode() {
		return directJob != null;
	}

	public final boolean trySubmitDirect() {
		if (directJob == null) {
			return false;
		}
		if (directSubmitted) {
			return true;
		}
		boolean submitted = SPEditBackend.getInstance().submit(directJob);
		if (submitted) {
			directSubmitted = true;
		}
		return submitted;
	}

	public final void cancelDirect() {
		if (directJob != null) {
			directJob.cancel();
		}
	}

	public final void cancel() {
		cancelled = true;
		cancelDirect();
	}

	public final boolean isCancelled() {
		return cancelled || (directJob != null && directJob.isCancelled());
	}

	protected final boolean isTaskCancelled() {
		return cancelled;
	}

	protected final SPEditJob directJob() {
		return directJob;
	}

	protected final boolean directDone() {
		return directJob != null && directJob.isDone();
	}

	protected final int directTotal() {
		return directJob == null ? 0 : directJob.totalInt();
	}

	protected final int directProcessed() {
		return directJob == null ? 0 : directJob.processedInt();
	}

	protected final Set<ChunkPos> directChunks() {
		return directJob == null ? Collections.emptySet() : directJob.chunks();
	}
}
