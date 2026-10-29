package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoManager;
import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SPEditJob {
	private final List<SPOperation> operations;
	private final Set<ChunkPos> chunks;
	private final UUID playerId;
	private final int dimension;
	private final long generation;
	private final UndoSnapshot undo;
	private final boolean recordUndo;
	private final boolean pushOwnUndo;
	private final long total;
	private int operationIndex;
	private volatile long processed;
	private boolean undoPushed;
	private volatile boolean cancelled;
	private volatile boolean discardUndo;
	private volatile boolean finished;

	public SPEditJob(UUID playerId, int dimension, long generation, List<SPOperation> operations,
					 boolean recordUndo) {
		this(playerId, dimension, generation, operations, recordUndo, null);
	}

	public SPEditJob(UUID playerId, int dimension, long generation, List<SPOperation> operations,
					 boolean recordUndo, UndoSnapshot sharedUndo) {
		this.playerId = playerId;
		this.dimension = dimension;
		this.generation = generation;
		this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
		this.recordUndo = recordUndo;
		this.undo = recordUndo ? (sharedUndo == null ? new UndoSnapshot() : sharedUndo) : null;
		this.pushOwnUndo = recordUndo && sharedUndo == null;
		Set<ChunkPos> allChunks = new LinkedHashSet<>();
		long sum = 0;
		for (SPOperation operation : operations) {
			allChunks.addAll(operation.chunks());
			sum += operation.total();
		}
		this.chunks = Collections.unmodifiableSet(allChunks);
		this.total = sum;
	}

	public UUID playerId() {
		return playerId;
	}

	public int dimension() {
		return dimension;
	}

	public long generation() {
		return generation;
	}

	public Set<ChunkPos> chunks() {
		return chunks;
	}

	public UndoSnapshot undo() {
		return undo;
	}

	public long total() {
		return total;
	}

	public long processed() {
		return processed;
	}

	public int totalInt() {
		return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
	}

	public int processedInt() {
		return processed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) processed;
	}

	public boolean isDone() {
		return finished || cancelled;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		cancelled = true;
	}

	public void discardUndo() {
		discardUndo = true;
		cancelled = true;
	}

	public int apply(SPEditWorld world, int budget) {
		if (cancelled || finished || budget <= 0) {
			return 0;
		}
		if (world.dimension() != dimension) {
			cancelled = true;
			return 0;
		}
		int used = 0;
		while (used < budget && !cancelled && operationIndex < operations.size()) {
			SPOperation operation = operations.get(operationIndex);
			long before = operation.processed();
			used += operation.apply(world, budget - used, undo, recordUndo);
			processed += operation.processed() - before;
			if (operation.isDone()) {
				operationIndex++;
			} else {
				break;
			}
		}
		if (operationIndex >= operations.size()) {
			finish();
		} else {
			pushUndo();
		}
		return used;
	}

	public void finish() {
		if (finished) {
			return;
		}
		finished = true;
		pushUndo();
	}

	private void pushUndo() {
		if (discardUndo || !pushOwnUndo || undoPushed || undo == null || undo.isEmpty()) {
			return;
		}
		undoPushed = true;
		UndoManager.getInstance().pushSnapshot(undo);
	}
}
