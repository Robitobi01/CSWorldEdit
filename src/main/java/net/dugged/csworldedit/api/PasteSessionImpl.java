package net.dugged.csworldedit.api;

import net.dugged.csworldedit.UndoManager;
import net.dugged.csworldedit.UndoSnapshot;
import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class PasteSessionImpl implements PasteSession {
	private final String id = UUID.randomUUID().toString();
	private final int dimension;
	private final long generation;
	private final PasteOptions options;
	private final UndoSnapshot undo = new UndoSnapshot();
	private final List<Batch> batches = new ArrayList<>();
	private PasteState state = PasteState.OPEN;
	private CancelReason cancelReason;
	private boolean committed;
	private Boolean direct;
	private boolean undoPushed;
	private long total;
	private long processed;
	private int queuedChanges;
	private int nbtOmitted;

	PasteSessionImpl(int dimension, long generation, PasteOptions options) {
		this.dimension = dimension;
		this.generation = generation;
		this.options = options;
		this.undo.markPending();
	}

	long generation() {
		return generation;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public PasteState state() {
		return state;
	}

	@Override
	public SubmitResult submit(Iterable<BlockChange> changes) {
		PasteApiImpl.requireClientThread();
		if (state != PasteState.OPEN) {
			return SubmitResult.CLOSED;
		}
		if (changes == null) {
			return SubmitResult.INVALID;
		}
		List<BlockChange> batch = new ArrayList<>();
		for (BlockChange change : changes) {
			if (change == null) {
				return SubmitResult.INVALID;
			}
			batch.add(change);
			if (batch.size() > PasteApi.MAX_BATCH_CHANGES) {
				return SubmitResult.INVALID;
			}
		}
		if (batch.isEmpty()) {
			return SubmitResult.INVALID;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null || mc.player == null || mc.world.provider.getDimension() != dimension) {
			return SubmitResult.CLOSED;
		}
		for (BlockChange change : batch) {
			BlockPos pos = change.pos();
			if (mc.world.isOutsideBuildHeight(pos)) {
				return SubmitResult.OUT_OF_HEIGHT;
			}
			if (!mc.world.isBlockLoaded(pos)) {
				return SubmitResult.NOT_LOADED;
			}
			if (change.state().getBlock().getRegistryName() == null) {
				return SubmitResult.INVALID;
			}
		}
		if (queuedChanges + batch.size() > options.maxQueuedChanges()) {
			return SubmitResult.BACKPRESSURE;
		}
		boolean expectedDirect = direct != null ? direct : SPEditBackend.isAvailable(mc.world);
		List<TaskSetBlock.SetBlockOperation> operations = new ArrayList<>(batch.size());
		for (BlockChange change : batch) {
			operations.add(new TaskSetBlock.SetBlockOperation(change.pos(), change.state(),
					change.rawTileEntityNbt()));
		}
		TaskSetBlock task = new TaskSetBlock(operations, mc.world, undo);
		if (task.isDirectMode() != expectedDirect) {
			task.cancel();
			cancelInternal(CancelReason.INVALID);
			return SubmitResult.UNAVAILABLE;
		}
		if (!task.isDirectMode() && !TaskSetBlock.setblockPermission) {
			task.cancel();
			cancelInternal(CancelReason.INVALID);
			return SubmitResult.UNAVAILABLE;
		}
		if (!task.isDirectMode()) {
			for (BlockChange change : batch) {
				if (change.rawTileEntityNbt() != null) {
					nbtOmitted++;
				}
			}
		}
		if (task.isDirectMode() && !task.trySubmitDirect()) {
			return SubmitResult.BACKPRESSURE;
		}
		TaskManager.getInstance().addTask(task);
		if (direct == null) {
			direct = task.isDirectMode();
		}
		Batch entry = new Batch(batch.size());
		entry.task = task;
		batches.add(entry);
		total += batch.size();
		queuedChanges += batch.size();
		return SubmitResult.ACCEPTED;
	}

	@Override
	public boolean commit() {
		PasteApiImpl.requireClientThread();
		if (state != PasteState.OPEN) {
			return false;
		}
		committed = true;
		if (batches.isEmpty()) {
			state = PasteState.COMPLETED;
			return true;
		}
		state = PasteState.RUNNING;
		return true;
	}

	@Override
	public boolean cancel(CancelReason reason) {
		PasteApiImpl.requireClientThread();
		if (state == PasteState.COMPLETED || state == PasteState.CANCELLED || state == PasteState.FAILED) {
			return false;
		}
		cancelInternal(reason == null ? CancelReason.USER : reason);
		return true;
	}

	private void cancelInternal(CancelReason reason) {
		state = PasteState.CANCELLED;
		cancelReason = reason;
		for (Batch batch : batches) {
			if (batch.done) {
				continue;
			}
			if (batch.task != null) {
				batch.processed = batch.task.getProcessedOperations();
				batch.done = true;
				batch.task.cancel();
				batch.task = null;
			}
		}
		processed = processed();
		queuedChanges = 0;
		if (reason == CancelReason.WORLD_CHANGED || reason == CancelReason.SHUTDOWN || processed == 0) {
			undo.clearPending();
		} else {
			pushUndo();
		}
	}

	@Override
	public PasteProgress progress() {
		return new PasteProgress(id, state, cancelReason, processed, total, queuedChanges, nbtOmitted);
	}

	void tick() {
		if (state != PasteState.OPEN && state != PasteState.RUNNING) {
			return;
		}
		boolean externallyCancelled = false;
		for (Batch batch : batches) {
			if (batch.done) {
				continue;
			}
			if (batch.task != null) {
				if (batch.task.isCancelled()) {
					batch.done = true;
					batch.processed = batch.task.getProcessedOperations();
					externallyCancelled = true;
				} else if (batch.task.isComplete()) {
					batch.done = true;
					batch.processed = batch.task.getProcessedOperations();
				} else {
					batch.processed = batch.task.getProcessedOperations();
				}
				if (batch.done) {
					batch.task = null;
				}
			}
		}
		if (externallyCancelled) {
			cancelInternal(CancelReason.CANCELLED_EXTERNALLY);
			return;
		}

		processed = processed();
		int queued = 0;
		for (Batch batch : batches) {
			if (!batch.done) {
				queued += batch.size;
			}
		}
		queuedChanges = queued;
		if (committed && queued == 0 && state == PasteState.RUNNING) {
			state = PasteState.COMPLETED;
			pushUndo();
		}
	}

	private long processed() {
		long result = 0;
		for (Batch batch : batches) {
			result += Math.max(0, Math.min(batch.size, batch.processed));
		}
		return result;
	}

	private void pushUndo() {
		if (undoPushed) {
			return;
		}
		undoPushed = true;
		if (undo.isEmpty()) {
			undo.clearPending();
			return;
		}
		UndoManager.getInstance().pushSnapshot(undo);
	}

	private static final class Batch {
		private final int size;
		private long processed;
		private boolean done;
		private TaskSetBlock task;

		private Batch(int size) {
			this.size = size;
		}
	}
}
