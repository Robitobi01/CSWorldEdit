package net.dugged.worldedit.tasks;

import net.dugged.worldedit.UndoManager;
import net.dugged.worldedit.UndoRecorder;
import net.dugged.worldedit.UndoSnapshot;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.*;

public class TaskSetBlock extends TaskChunk {

	public static boolean setblockPermission = false;
	private final Map<ChunkPos, Queue<SetBlockOperation>> opsMap = new HashMap<>();
	private final World world;
	private final int totalOps;
	private int processedOps = 0;

	public TaskSetBlock(Map<BlockPos, IBlockState> blocksToPlace, World world) {
		this.world = world;
		BlockPos minPos = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		BlockPos maxPos = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
		UndoManager.getInstance().pushSnapshot(new UndoSnapshot(UndoRecorder.recordPositions(blocksToPlace.keySet(),
				world)));
		for (Map.Entry<BlockPos, IBlockState> entry : blocksToPlace.entrySet()) {
			BlockPos pos = entry.getKey();
			IBlockState state = entry.getValue();
			minPos = getMinPos(minPos, pos);
			maxPos = getMaxPos(maxPos, pos);
			SetBlockOperation op = new SetBlockOperation(pos, state);
			ChunkPos cp = new ChunkPos(pos);
			opsMap.computeIfAbsent(cp, k -> new LinkedList<>()).offer(op);
		}
		totalOps = blocksToPlace.size();
	}

	private static boolean placeLast(Block block) {
		return (block instanceof BlockBed || block instanceof BlockBush || block instanceof BlockFlowerPot || block instanceof BlockFire || block instanceof BlockButton || block instanceof BlockSign || block instanceof BlockChorusFlower || block instanceof BlockCake || block instanceof BlockCarpet || block instanceof BlockRailBase || block instanceof BlockEndRod || block instanceof BlockLever || block instanceof BlockRedstoneWire || block instanceof BlockCactus || block instanceof BlockVine || block instanceof BlockSnow || block instanceof BlockTorch || block instanceof BlockLadder || block instanceof BlockBanner || block instanceof BlockDoor || block instanceof BlockRedstoneDiode || block instanceof BlockBasePressurePlate || block instanceof BlockPistonMoving || block instanceof BlockPistonExtension || block instanceof BlockReed || block instanceof BlockTripWireHook);
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		int processedThisCall = 0;
		Queue<SetBlockOperation> queue = opsMap.get(pos);
		if (queue == null || !setblockPermission) {
			return 0;
		}
		boolean hasImmediate = false;
		for (SetBlockOperation op : queue) {
			if (!op.delayed) {
				hasImmediate = true;
				break;
			}
		}
		int size = queue.size();
		for (int i = 0; i < size && processedThisCall < maxPackets; i++) {
			SetBlockOperation op = queue.poll();
			if (hasImmediate && op.delayed) {
				queue.offer(op);
			} else {
				if (world.getBlockState(op.pos).equals(op.state)) {
					processedOps++;
					processedThisCall++;
				} else {
					sendSetBlockCommand(op.pos, op.state);
					processedOps++;
					processedThisCall++;
				}
			}
		}
		if (queue.isEmpty()) {
			opsMap.remove(pos);
		}
		return processedThisCall;
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

	private void sendSetBlockCommand(BlockPos pos, IBlockState blockState) {
		String name = blockState.getBlock().getRegistryName().toString();
		int meta = blockState.getBlock().getMetaFromState(blockState);
		String cmd = String.format("/setblock %d %d %d %s %d", pos.getX(), pos.getY(), pos.getZ(), name, meta);
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
}