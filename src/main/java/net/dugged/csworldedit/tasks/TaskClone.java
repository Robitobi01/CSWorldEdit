package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.UndoManager;
import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class TaskClone extends TaskChunk {

	private static final int SUBCHUNK = 16;
	public static boolean clonePermission = false;
	private final Map<ChunkPos, Queue<CloneOperation>> opsMap = new HashMap<>();
	private final Mode mode;
	private final UndoSnapshot snapshot;
	private final int totalOps;
	private final List<ChunkPos> orderedDestinationChunks;
	private final boolean actualOverlapOccurred;
	private final AxisAlignedBB sourceRegion;
	private int processedOps = 0;

	public TaskClone(BlockPos src1, BlockPos src2, BlockPos dstOrigin, World world, Mode mode) {
		this(src1, src2, dstOrigin, world, mode, new UndoSnapshot());
		UndoManager.getInstance().pushSnapshot(snapshot);
	}

	public TaskClone(BlockPos src1, BlockPos src2, BlockPos dstOrigin, World world, Mode mode, UndoSnapshot snapshot) {
		this.mode = mode;
		this.snapshot = snapshot;
		BlockPos minSrc = getMinPos(src1, src2);
		BlockPos maxSrc = getMaxPos(src1, src2);
		sourceRegion = new AxisAlignedBB((minSrc.getX() & ~15), 0, (minSrc.getZ() & ~15), ((maxSrc.getX() | 15) + 1),
				256, ((maxSrc.getZ() | 15) + 1));
		BlockPos maxDst = dstOrigin.add(maxSrc.subtract(minSrc));
		snapshot.recordRegion(dstOrigin, maxDst, world);

		BlockPos delta = dstOrigin.subtract(minSrc);
		AxisAlignedBB sourceBB = new AxisAlignedBB(minSrc, maxSrc.add(1, 1, 1));
		AxisAlignedBB destBB = new AxisAlignedBB(dstOrigin, maxDst.add(1, 1, 1));
		this.actualOverlapOccurred = sourceBB.intersects(destBB);

		int x_iter_start = minSrc.getX();
		int x_iter_end = maxSrc.getX();
		int x_iter_step = SUBCHUNK;

		int y_iter_start = maxSrc.getY();
		int y_iter_end = minSrc.getY();
		int y_iter_step = -SUBCHUNK;

		int z_iter_start = minSrc.getZ();
		int z_iter_end = maxSrc.getZ();
		int z_iter_step = SUBCHUNK;

		if (this.actualOverlapOccurred) {
			if (delta.getX() > 0) {
				x_iter_start = maxSrc.getX();
				x_iter_end = minSrc.getX();
				x_iter_step = -SUBCHUNK;
			}
			if (delta.getY() < 0) {
				y_iter_start = minSrc.getY();
				y_iter_end = maxSrc.getY();
				y_iter_step = SUBCHUNK;
			} else {
				y_iter_start = maxSrc.getY();
				y_iter_end = minSrc.getY();
			}
			if (delta.getZ() > 0) {
				z_iter_start = maxSrc.getZ();
				z_iter_end = minSrc.getZ();
				z_iter_step = -SUBCHUNK;
			}
		}

		List<CloneOperation> tempOperationsList = new ArrayList<>();
		this.orderedDestinationChunks = new ArrayList<>();
		Set<ChunkPos> uniqueDestinationChunksInOrder = new LinkedHashSet<>();

		for (int y = y_iter_start; (y_iter_step > 0 ? y <= y_iter_end : y >= y_iter_end); y += y_iter_step) {
			for (int z = z_iter_start; (z_iter_step > 0 ? z <= z_iter_end : z >= z_iter_end); z += z_iter_step) {
				for (int x = x_iter_start; (x_iter_step > 0 ? x <= x_iter_end : x >= x_iter_end); x += x_iter_step) {
					int currentSrcMinX = (x_iter_step > 0) ? x : Math.max(x_iter_end, x - SUBCHUNK + 1);
					int currentSrcMinY = (y_iter_step > 0) ? y : Math.max(y_iter_end, y - SUBCHUNK + 1);
					int currentSrcMinZ = (z_iter_step > 0) ? z : Math.max(z_iter_end, z - SUBCHUNK + 1);

					int currentSrcMaxX = (x_iter_step > 0) ? Math.min(x + SUBCHUNK - 1, x_iter_end) : x;
					int currentSrcMaxY = (y_iter_step > 0) ? Math.min(y + SUBCHUNK - 1, y_iter_end) : y;
					int currentSrcMaxZ = (z_iter_step > 0) ? Math.min(z + SUBCHUNK - 1, z_iter_end) : z;

					int finalX1 = Math.min(currentSrcMinX, currentSrcMaxX);
					int finalY1 = Math.min(currentSrcMinY, currentSrcMaxY);
					int finalZ1 = Math.min(currentSrcMinZ, currentSrcMaxZ);
					int finalX2 = Math.max(currentSrcMinX, currentSrcMaxX);
					int finalY2 = Math.max(currentSrcMinY, currentSrcMaxY);
					int finalZ2 = Math.max(currentSrcMinZ, currentSrcMaxZ);

					if (finalX1 > maxSrc.getX() || finalX2 < minSrc.getX() || finalY1 > maxSrc.getY() || finalY2 < minSrc.getY() || finalZ1 > maxSrc.getZ() || finalZ2 < minSrc.getZ()) {
						continue;
					}

					int destSubMinX = dstOrigin.getX() + (finalX1 - minSrc.getX());
					int destSubMinY = dstOrigin.getY() + (finalY1 - minSrc.getY());
					int destSubMinZ = dstOrigin.getZ() + (finalZ1 - minSrc.getZ());

					CloneOperation op = new CloneOperation(finalX1, finalY1, finalZ1, finalX2, finalY2, finalZ2,
							destSubMinX, destSubMinY, destSubMinZ);
					tempOperationsList.add(op);
					uniqueDestinationChunksInOrder.add(new ChunkPos(new BlockPos(op.x3, op.y3, op.z3)));
				}
			}
		}
		this.orderedDestinationChunks.addAll(uniqueDestinationChunksInOrder);

		for (CloneOperation op : tempOperationsList) {
			ChunkPos cp = new ChunkPos(new BlockPos(op.x3, op.y3, op.z3));
			opsMap.computeIfAbsent(cp, k -> new LinkedList<>()).offer(op);
		}

		this.totalOps = tempOperationsList.size();
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		int count = 0;
		Queue<CloneOperation> queue = opsMap.get(pos);
		if (queue == null || !TaskClone.clonePermission) {
			return 0;
		}

		while (count < maxPackets && !queue.isEmpty()) {
			CloneOperation op = queue.poll();
			String cmd = String.format("/clone %d %d %d %d %d %d %d %d %d %s", op.x1, op.y1, op.z1, op.x2, op.y2,
					op.z2, op.x3, op.y3, op.z3, mode.getCommandArg());
			sendCommand(cmd);
			count++;
			processedOps++;
		}

		if (queue.isEmpty()) {
			opsMap.remove(pos);
		}
		return count;
	}

	public List<ChunkPos> getOrderedDestinationChunks() {
		return orderedDestinationChunks;
	}

	public boolean isOverlapSensitive() {
		return this.actualOverlapOccurred;
	}

	@Override
	public int getTotalOperations() {
		return totalOps;
	}

	@Override
	public int getProcessedOperations() {
		return processedOps;
	}

	@Override
	public Set<ChunkPos> getPendingChunks() {
		return opsMap.keySet();
	}

	@Override
	public boolean isComplete() {
		return opsMap.isEmpty();
	}

	public AxisAlignedBB getSourceRegion() {
		return sourceRegion;
	}

	public enum Mode {
		FORCE("replace force"), MASKED("masked force"), MOVE("replace move");
		private final String commandArg;

		Mode(String commandArg) {
			this.commandArg = commandArg;
		}

		public String getCommandArg() {
			return commandArg;
		}
	}

	public static class CloneOperation {
		public final int x1, y1, z1, x2, y2, z2;
		public final int x3, y3, z3;

		public CloneOperation(int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
			this.x3 = x3;
			this.y3 = y3;
			this.z3 = z3;
		}
	}
}