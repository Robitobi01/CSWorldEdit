package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.UndoManager;
import net.dugged.csworldedit.UndoSnapshot;
import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.sp.SPEditJob;
import net.dugged.csworldedit.sp.SPFillOperation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class TaskFill extends TaskChunk {

	private static final int SUBCHUNK = 16;
	public static final int VANILLA_FILL_LIMIT = 32768;
	public static boolean fillPermission = false;
	private final Map<ChunkPos, Queue<FillOperation>> opsMap = new HashMap<>();
	private final UndoSnapshot snapshot;
	private final int totalOps;
	private int processedOps = 0;

	public TaskFill(BlockPos pos1, BlockPos pos2, IBlockState state, World world) {
		this(pos1, pos2, state, world, new UndoSnapshot(), true);
	}

	public TaskFill(BlockPos pos1, BlockPos pos2, IBlockState state, World world, UndoSnapshot snapshot) {
		this(pos1, pos2, state, world, snapshot, false);
	}

	private TaskFill(BlockPos pos1, BlockPos pos2, IBlockState state, World world, UndoSnapshot snapshot,
					 boolean autoPush) {
		this.snapshot = snapshot == null ? new UndoSnapshot() : snapshot;
		BlockPos minPos = getMinPos(pos1, pos2);
		BlockPos maxPos = getMaxPos(pos1, pos2);
		if (Configuration.direct.enabled) {
			UndoSnapshot shared = autoPush ? null : this.snapshot;
			if (shared != null) {
				shared.markPending();
			}
			SPEditJob job = SPEditBackend.getInstance().createJob(world,
					Collections.singletonList(new SPFillOperation(minPos, maxPos, state)), true, shared);
			if (job != null) {
				useDirectJob(job);
				this.totalOps = 0;
				return;
			}
		}
		this.snapshot.recordRegion(minPos, maxPos, world);
		for (int x = minPos.getX(); x <= maxPos.getX(); x += SUBCHUNK) {
			int ex = Math.min(x + SUBCHUNK - 1, maxPos.getX());
			for (int z = minPos.getZ(); z <= maxPos.getZ(); z += SUBCHUNK) {
				int ez = Math.min(z + SUBCHUNK - 1, maxPos.getZ());
				int y = minPos.getY();
				while (y <= maxPos.getY()) {
					int ey = mergeAlongY(x, ex, y, z, ez, maxPos.getY());
					FillOperation op = new FillOperation(x, y, z, ex, ey, ez, state);
					ChunkPos cp = new ChunkPos(new BlockPos(x, 0, z));
					opsMap.computeIfAbsent(cp, k -> new LinkedList<>()).offer(op);
					y = ey + 1;
				}
			}
		}
		totalOps = opsMap.values().stream().mapToInt(Queue::size).sum();
		if (autoPush) {
			UndoManager.getInstance().pushSnapshot(snapshot);
		}
	}

	private static int mergeAlongY(int x1, int x2, int y, int z1, int z2, int maxY) {
		int end = Math.min(y + SUBCHUNK - 1, maxY);
		while (end < maxY) {
			int nextEnd = Math.min(end + SUBCHUNK, maxY);
			long volume = (long) (x2 - x1 + 1) * (nextEnd - y + 1) * (z2 - z1 + 1);
			if (volume > VANILLA_FILL_LIMIT) {
				break;
			}
			end = nextEnd;
		}
		return end;
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		if (isDirectMode()) {
			return 0;
		}
		int count = 0;
		Queue<FillOperation> queue = opsMap.get(pos);
		if (queue == null || !fillPermission) {
			return 0;
		}
		while (count < maxPackets && !queue.isEmpty()) {
			FillOperation op = queue.poll();
			String name = Objects.requireNonNull(op.state.getBlock().getRegistryName()).toString();
			int meta = op.state.getBlock().getMetaFromState(op.state);
			String cmd = String.format("/fill %d %d %d %d %d %d %s %d", op.x1, op.y1, op.z1, op.x2, op.y2, op.z2, name
					, meta);
			sendCommand(cmd);
			processedOps++;
			count++;
		}
		if (queue.isEmpty()) {
			opsMap.remove(pos);
		}
		return count;
	}

	@Override
	public int getTotalOperations() {
		return isDirectMode() ? directTotal() : totalOps;
	}

	@Override
	public int getProcessedOperations() {
		return isDirectMode() ? directProcessed() : processedOps;
	}

	@Override
	public Set<ChunkPos> getPendingChunks() {
		return isDirectMode() ? directChunks() : opsMap.keySet();
	}

	@Override
	public boolean isComplete() {
		return isDirectMode() ? directDone() : opsMap.isEmpty();
	}

	public static class FillOperation {

		public final int x1, y1, z1, x2, y2, z2;
		public final IBlockState state;

		public FillOperation(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
			this.state = state;
		}
	}
}