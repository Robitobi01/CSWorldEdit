package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.sp.SPEditJob;
import net.dugged.csworldedit.sp.SPReplaceItemOperation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockHopper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class TaskReplaceItem extends TaskChunk {

	public static boolean replaceItemPermission = false;
	private final Map<ChunkPos, Queue<ReplaceItemOperation>> opsMap = new HashMap<>();
	private final int totalOps;
	private int processedOps = 0;

	public TaskReplaceItem(Map<BlockPos, ItemStack> containers, World world) {
		this.totalOps = containers.size();
		if (Configuration.direct.enabled && !containers.isEmpty()) {
			List<SPReplaceItemOperation.Entry> entries = new ArrayList<>(containers.size());
			for (Map.Entry<BlockPos, ItemStack> entry : containers.entrySet()) {
				entries.add(new SPReplaceItemOperation.Entry(entry.getKey(), entry.getValue()));
			}
			SPEditJob job = SPEditBackend.getInstance().createJob(world,
					Collections.singletonList(new SPReplaceItemOperation(entries)), true);
			if (job != null) {
				useDirectJob(job);
				return;
			}
		}
		for (Map.Entry<BlockPos, ItemStack> entry : containers.entrySet()) {
			BlockPos pos = entry.getKey();
			ItemStack stack = entry.getValue();
			int invSize = getContainerSize(world.getBlockState(pos).getBlock());
			ReplaceItemOperation op = new ReplaceItemOperation(pos, stack, invSize);
			ChunkPos cp = new ChunkPos(pos);
			opsMap.computeIfAbsent(cp, k -> new LinkedList<>()).offer(op);
		}
	}

	private int getContainerSize(Block block) {
		if (block instanceof BlockDispenser) {
			return 9;
		} else if (block instanceof BlockHopper) {
			return 5;
		} else {
			return 27;
		}
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		if (isDirectMode()) {
			return 0;
		}
		int count = 0;
		Queue<ReplaceItemOperation> queue = opsMap.get(pos);
		if (queue == null || !replaceItemPermission) {
			return 0;
		}
		while (count < maxPackets && !queue.isEmpty()) {
			ReplaceItemOperation op = queue.peek();
			while (count < maxPackets && op.nextSlot < op.inventorySize) {
				String cmd = String.format("/replaceitem block %d %d %d slot.container.%d %s %d %d", op.pos.getX(),
						op.pos.getY(), op.pos.getZ(), op.nextSlot, op.stack.getItem().getRegistryName().toString(),
						op.stack.getCount(), op.stack.getItemDamage());
				sendCommand(cmd);
				op.nextSlot++;
				count++;
			}
			if (op.nextSlot >= op.inventorySize) {
				queue.poll();
				processedOps++;
			}
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

	public static class ReplaceItemOperation {

		public final BlockPos pos;
		public final ItemStack stack;
		public final int inventorySize;
		private int nextSlot = 0;

		public ReplaceItemOperation(BlockPos pos, ItemStack stack, int inventorySize) {
			this.pos = pos;
			this.stack = stack;
			this.inventorySize = inventorySize;
		}
	}
}