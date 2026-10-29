package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SPReplaceItemOperation implements SPOperation {
	private final List<Entry> entries;
	private final Set<ChunkPos> chunks;
	private int index;
	private int slot;
	private long processed;

	public SPReplaceItemOperation(List<Entry> entries) {
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
		Set<ChunkPos> positions = new LinkedHashSet<>();
		for (Entry entry : entries) {
			positions.add(new ChunkPos(entry.pos()));
		}
		this.chunks = Collections.unmodifiableSet(positions);
	}

	@Override
	public long total() {
		return entries.size();
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
		int used = 0;
		while (used < budget && index < entries.size()) {
			Entry entry = entries.get(index);
			int size = world.inventorySize(entry.pos());
			if (slot >= size) {
				index++;
				slot = 0;
				processed++;
				continue;
			}
			if (recordUndo) {
				undo.recordItemSlot(entry.pos(), slot, world.getStack(entry.pos(), slot));
			}
			world.setStack(entry.pos(), slot, entry.stack().copy());
			slot++;
			used++;
			if (slot >= size) {
				index++;
				slot = 0;
				processed++;
			}
		}
		return used;
	}

	@Override
	public boolean isDone() {
		return index >= entries.size();
	}

	public record Entry(BlockPos pos, ItemStack stack) {
	}
}
