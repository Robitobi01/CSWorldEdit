package net.dugged.csworldedit;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UndoSnapshot {

	private final Map<BlockPos, IBlockState> undoData = new ConcurrentHashMap<>();
	private final Map<BlockPos, Map<Integer, ItemStack>> itemData = new ConcurrentHashMap<>();
	private volatile boolean pending;

	public void markPending() {
		pending = true;
	}

	public boolean isPending() {
		return pending;
	}

	public void clearPending() {
		pending = false;
	}

	public void recordPositions(Set<BlockPos> positions, World world) {
		for (BlockPos pos : positions) {
			undoData.putIfAbsent(pos, world.getBlockState(pos));
		}
	}

	public void recordRegion(BlockPos pos1, BlockPos pos2, World world) {
		int x1 = Math.min(pos1.getX(), pos2.getX());
		int y1 = Math.min(pos1.getY(), pos2.getY());
		int z1 = Math.min(pos1.getZ(), pos2.getZ());
		int x2 = Math.max(pos1.getX(), pos2.getX());
		int y2 = Math.max(pos1.getY(), pos2.getY());
		int z2 = Math.max(pos1.getZ(), pos2.getZ());
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					undoData.putIfAbsent(pos, world.getBlockState(pos));
				}
			}
		}
	}

	public boolean recordOriginal(BlockPos pos, IBlockState state) {
		return undoData.putIfAbsent(pos, state) == null;
	}

	public void removeOriginal(BlockPos pos) {
		undoData.remove(pos);
	}

	public void recordItemSlot(BlockPos pos, int slot, ItemStack stack) {
		itemData.computeIfAbsent(pos, key -> new ConcurrentHashMap<>())
				.putIfAbsent(slot, stack == null ? ItemStack.EMPTY : stack.copy());
	}

	public Map<BlockPos, IBlockState> getUndoData() {
		return undoData;
	}

	public Map<BlockPos, Map<Integer, ItemStack>> getItemData() {
		return itemData;
	}

	public int size() {
		int size = undoData.size();
		for (Map<Integer, ItemStack> slots : itemData.values()) {
			size += slots.size();
		}
		return size;
	}

	public boolean isEmpty() {
		return undoData.isEmpty() && itemData.isEmpty();
	}
}
