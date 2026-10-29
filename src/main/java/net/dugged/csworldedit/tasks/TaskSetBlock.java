package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.UndoManager;
import net.dugged.csworldedit.UndoSnapshot;
import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.sp.SPEditJob;
import net.dugged.csworldedit.sp.SPSetBlockOperation;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class TaskSetBlock extends TaskChunk {

	public static boolean setblockPermission = false;
	private final Map<ChunkPos, Queue<SetBlockOperation>> opsMap = new HashMap<>();
	private final Map<ChunkPos, Deque<Batch>> batches = new HashMap<>();
	private final World world;
	private final int totalOps;
	private int processedOps = 0;

	public TaskSetBlock(Map<BlockPos, IBlockState> blocksToPlace, World world) {
		this.world = world;
		this.totalOps = blocksToPlace.size();
		if (Configuration.direct.enabled && !blocksToPlace.isEmpty()) {
			SPEditJob job = SPEditBackend.getInstance().createJob(world,
					Collections.singletonList(new SPSetBlockOperation(blocksToPlace)), true);
			if (job != null) {
				useDirectJob(job);
				return;
			}
		}
		UndoSnapshot snapshot = new UndoSnapshot();
		if (!blocksToPlace.isEmpty()) {
			snapshot.recordPositions(blocksToPlace.keySet(), world);
			UndoManager.getInstance().pushSnapshot(snapshot);
		}
		for (Map.Entry<BlockPos, IBlockState> entry : blocksToPlace.entrySet()) {
			BlockPos pos = entry.getKey();
			IBlockState state = entry.getValue();
			SetBlockOperation op = new SetBlockOperation(pos, state);
			ChunkPos cp = new ChunkPos(pos);
			opsMap.computeIfAbsent(cp, k -> new LinkedList<>()).offer(op);
		}
	}

	private static boolean placeLast(Block block) {
		return (block instanceof BlockBed || block instanceof BlockBush || block instanceof BlockFlowerPot || block instanceof BlockFire || block instanceof BlockButton || block instanceof BlockSign || block instanceof BlockChorusFlower || block instanceof BlockCake || block instanceof BlockCarpet || block instanceof BlockRailBase || block instanceof BlockEndRod || block instanceof BlockLever || block instanceof BlockRedstoneWire || block instanceof BlockCactus || block instanceof BlockVine || block instanceof BlockSnow || block instanceof BlockTorch || block instanceof BlockLadder || block instanceof BlockBanner || block instanceof BlockDoor || block instanceof BlockRedstoneDiode || block instanceof BlockBasePressurePlate || block instanceof BlockPistonMoving || block instanceof BlockPistonExtension || block instanceof BlockReed || block instanceof BlockTripWireHook);
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		if (isDirectMode()) {
			return 0;
		}
		Queue<SetBlockOperation> queue = opsMap.get(pos);
		if (queue == null || !setblockPermission) {
			return 0;
		}

		Deque<Batch> chunkBatches = batches.get(pos);
		if (chunkBatches == null) {
			chunkBatches = planBatches(queue);
			batches.put(pos, chunkBatches);
			queue.clear();
		}

		int commandsSent = 0;
		while (commandsSent < maxPackets && !chunkBatches.isEmpty()) {
			Batch batch = chunkBatches.peek();
			if (batch.delayed || batch.volume() == 1) {
				chunkBatches.poll();
				processedOps += batch.opCount;
				if (!world.getBlockState(batch.singlePos()).equals(batch.state)) {
					sendSetBlockCommand(batch.singlePos(), batch.state);
					commandsSent++;
				}
				continue;
			}

			int changed = countChanged(batch);
			if (changed == 0) {
				chunkBatches.poll();
				processedOps += batch.opCount;
				continue;
			}
			if (changed == batch.opCount) {
				chunkBatches.poll();
				sendFillCommand(batch);
				processedOps += batch.opCount;
				commandsSent++;
				continue;
			}

			chunkBatches.poll();
			processedOps += batch.opCount - changed;
			Deque<Batch> replacements = new ArrayDeque<>(changed);
			for (BlockPos p : batch.positions()) {
				if (!world.getBlockState(p).equals(batch.state)) {
					replacements.add(Batch.single(p, batch.state));
				}
			}
			while (!replacements.isEmpty()) {
				chunkBatches.addFirst(replacements.removeLast());
			}
		}

		if (chunkBatches.isEmpty()) {
			batches.remove(pos);
			opsMap.remove(pos);
		}
		return commandsSent;
	}

	private int countChanged(Batch batch) {
		int changed = 0;
		for (BlockPos pos : batch.positions()) {
			if (!world.getBlockState(pos).equals(batch.state)) {
				changed++;
			}
		}
		return changed;
	}

	private Deque<Batch> planBatches(Queue<SetBlockOperation> queue) {
		List<SetBlockOperation> immediate = new ArrayList<>();
		List<SetBlockOperation> delayed = new ArrayList<>();
		for (SetBlockOperation op : queue) {
			(op.delayed ? delayed : immediate).add(op);
		}

		Deque<Batch> result = new ArrayDeque<>();
		if (immediate.size() > 1 && TaskFill.fillPermission) {
			result.addAll(buildBatches(immediate));
		} else {
			for (SetBlockOperation op : immediate) {
				result.add(Batch.single(op.pos, op.state));
			}
		}
		for (SetBlockOperation op : delayed) {
			Batch single = Batch.single(op.pos, op.state);
			single.delayed = true;
			result.add(single);
		}
		return result;
	}

	private static List<Batch> buildBatches(List<SetBlockOperation> operations) {
		Map<IBlockState, List<SetBlockOperation>> byState = new HashMap<>();
		for (SetBlockOperation op : operations) {
			byState.computeIfAbsent(op.state, k -> new ArrayList<>()).add(op);
		}

		List<Batch> boxes = new ArrayList<>();
		for (Map.Entry<IBlockState, List<SetBlockOperation>> entry : byState.entrySet()) {
			List<SetBlockOperation> ops = entry.getValue();
			ops.sort(Comparator.comparingInt((SetBlockOperation op) -> op.pos.getY())
					.thenComparingInt(op -> op.pos.getZ())
					.thenComparingInt(op -> op.pos.getX()));
			List<Batch> runs = buildXRuns(ops, entry.getKey());
			boxes.addAll(mergeAlongY(runs, entry.getKey()));
		}
		return boxes;
	}

	private static List<Batch> buildXRuns(List<SetBlockOperation> ops, IBlockState state) {
		List<Batch> runs = new ArrayList<>();
		Batch current = null;
		for (SetBlockOperation op : ops) {
			int x = op.pos.getX();
			int y = op.pos.getY();
			int z = op.pos.getZ();
			if (current != null && current.y1 == y && current.z1 == z && current.x2 + 1 == x) {
				current.x2 = x;
				current.opCount++;
			} else {
				current = Batch.box(x, y, z, x, y, z, state);
				current.opCount = 1;
				runs.add(current);
			}
		}
		return runs;
	}

	private static List<Batch> mergeAlongY(List<Batch> runs, IBlockState state) {
		Map<BatchKey, Batch> latest = new HashMap<>();
		List<Batch> merged = new ArrayList<>();
		for (Batch run : runs) {
			BatchKey key = new BatchKey(state, run.x1, run.x2, run.z1, 0);
			Batch previous = latest.get(key);
			if (previous != null && previous.y2 + 1 == run.y1
					&& volume(previous.x2 - previous.x1 + 1, run.y2 - previous.y1 + 1,
					previous.z2 - previous.z1 + 1) <= TaskFill.VANILLA_FILL_LIMIT) {
				previous.y2 = run.y2;
				previous.opCount += run.opCount;
			} else {
				Batch box = Batch.box(run.x1, run.y1, run.z1, run.x2, run.y2, run.z1, state);
				box.opCount = run.opCount;
				merged.add(box);
				latest.put(key, box);
			}
		}
		return mergeAlongZ(merged, state);
	}

	private static List<Batch> mergeAlongZ(List<Batch> boxes, IBlockState state) {
		boxes.sort(Comparator.comparingInt((Batch box) -> box.y1)
				.thenComparingInt(box -> box.y2)
				.thenComparingInt(box -> box.z1)
				.thenComparingInt(box -> box.x1));
		Map<BatchKey, Batch> latest = new HashMap<>();
		List<Batch> merged = new ArrayList<>();
		for (Batch box : boxes) {
			BatchKey key = new BatchKey(state, box.x1, box.x2, box.y1, box.y2);
			Batch previous = latest.get(key);
			if (previous != null && previous.z2 + 1 == box.z1
					&& volume(box.x2 - box.x1 + 1, box.y2 - box.y1 + 1,
					box.z2 - previous.z1 + 1) <= TaskFill.VANILLA_FILL_LIMIT) {
				previous.z2 = box.z2;
				previous.opCount += box.opCount;
			} else {
				merged.add(box);
				latest.put(key, box);
			}
		}
		return merged;
	}

	private static long volume(int x, int y, int z) {
		return (long) x * y * z;
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

	private void sendSetBlockCommand(BlockPos pos, IBlockState blockState) {
		String name = blockState.getBlock().getRegistryName().toString();
		int meta = blockState.getBlock().getMetaFromState(blockState);
		String cmd = String.format("/setblock %d %d %d %s %d", pos.getX(), pos.getY(), pos.getZ(), name, meta);
		sendCommand(cmd);
	}

	private void sendFillCommand(Batch batch) {
		String name = batch.state.getBlock().getRegistryName().toString();
		int meta = batch.state.getBlock().getMetaFromState(batch.state);
		String cmd = String.format("/fill %d %d %d %d %d %d %s %d", batch.x1, batch.y1, batch.z1,
				batch.x2, batch.y2, batch.z2, name, meta);
		sendCommand(cmd);
	}

	public static class SetBlockOperation {

		public final BlockPos pos;
		public final IBlockState state;
		public final boolean delayed;

		public SetBlockOperation(BlockPos pos, IBlockState state) {
			this.pos = pos;
			this.state = state;
			this.delayed = placeLast(state.getBlock());
		}
	}

	private static final class Batch {
		final IBlockState state;
		int x1, y1, z1, x2, y2, z2;
		int opCount;
		boolean delayed;

		private Batch(IBlockState state, int x1, int y1, int z1, int x2, int y2, int z2) {
			this.state = state;
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
		}

		static Batch box(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
			return new Batch(state, x1, y1, z1, x2, y2, z2);
		}

		static Batch single(BlockPos pos, IBlockState state) {
			Batch batch = new Batch(state, pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
			batch.opCount = 1;
			return batch;
		}

		long volume() {
			return (long) (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
		}

		BlockPos singlePos() {
			return new BlockPos(x1, y1, z1);
		}

		Iterable<BlockPos> positions() {
			return BlockPos.getAllInBox(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
		}
	}

	private record BatchKey(IBlockState state, int a, int b, int c, int d) {
	}
}
