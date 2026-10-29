package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class SPUndoOperation implements SPOperation {
	private final UndoSnapshot snapshot;
	private final Set<ChunkPos> chunks;
	private Iterator<Map.Entry<BlockPos, IBlockState>> blocks;
	private Iterator<Map.Entry<BlockPos, Map<Integer, ItemStack>>> items;
	private Iterator<Map.Entry<Integer, ItemStack>> slots;
	private BlockPos slotPos;
	private long processed;

	public SPUndoOperation(UndoSnapshot snapshot) {
		this.snapshot = snapshot;
		Set<ChunkPos> positions = new LinkedHashSet<>();
		for (BlockPos pos : snapshot.getUndoData().keySet()) {
			positions.add(new ChunkPos(pos));
		}
		for (BlockPos pos : snapshot.getItemData().keySet()) {
			positions.add(new ChunkPos(pos));
		}
		this.chunks = Collections.unmodifiableSet(positions);
	}

	@Override
	public long total() {
		return snapshot.size();
	}

	@Override
	public long processed() {
		return processed;
	}

	@Override
	public Set<ChunkPos> chunks() {
		return chunks;
	}

	@Override
	public int apply(SPEditWorld world, int budget, UndoSnapshot undo, boolean recordUndo) {
		if (blocks == null) {
			blocks = snapshot.getUndoData().entrySet().iterator();
			items = snapshot.getItemData().entrySet().iterator();
			slots = Collections.emptyIterator();
		}
		int used = 0;
		while (used < budget && blocks.hasNext()) {
			Map.Entry<BlockPos, IBlockState> entry = blocks.next();
			SPBlockEditor.setBlock(world, entry.getKey(), entry.getValue(), null, false);
			processed++;
			used++;
		}
		while (used < budget) {
			if (!slots.hasNext()) {
				if (!items.hasNext()) {
					break;
				}
				Map.Entry<BlockPos, Map<Integer, ItemStack>> entry = items.next();
				slotPos = entry.getKey();
				slots = entry.getValue().entrySet().iterator();
				continue;
			}
			Map.Entry<Integer, ItemStack> entry = slots.next();
			world.setStack(slotPos, entry.getKey(), entry.getValue().copy());
			processed++;
			used++;
		}
		return used;
	}

	@Override
	public boolean isDone() {
		if (blocks == null) {
			return snapshot.size() == 0;
		}
		return !blocks.hasNext() && !slots.hasNext() && !items.hasNext();
	}
}
