package net.dugged.csworldedit.scan;

import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.world.World;

public abstract class ClientJob {

	private final World world;
	private final Thread ownerThread;
	private SnapshotManager manager;
	private long globalEpoch;
	private long worldEpoch;
	private long taskGeneration;
	private volatile boolean cancelled;
	private volatile boolean finished;

	protected ClientJob(World world) {
		this.world = world;
		this.ownerThread = Thread.currentThread();
	}

	final void bind(SnapshotManager manager, long globalEpoch, long worldEpoch) {
		this.manager = manager;
		this.globalEpoch = globalEpoch;
		this.worldEpoch = worldEpoch;
		this.taskGeneration = TaskManager.getInstance().getGeneration();
	}

	public abstract int step(int budget);

	public final World world() {
		return world;
	}

	final Object worldKey() {
		return world == null ? SnapshotManager.NULL_WORLD : world;
	}

	final Thread ownerThread() {
		return ownerThread;
	}

	final boolean alive() {
		return !cancelled && manager != null && manager.globalEpoch() == globalEpoch
				&& manager.worldEpoch(worldKey()) == worldEpoch
				&& TaskManager.getInstance().getGeneration() == taskGeneration;
	}

	public final boolean isCancelled() {
		return cancelled || !alive();
	}

	public final boolean isFinished() {
		return finished;
	}

	protected final void markFinished() {
		finished = true;
	}

	final void cancelInternal() {
		cancelled = true;
	}
}
