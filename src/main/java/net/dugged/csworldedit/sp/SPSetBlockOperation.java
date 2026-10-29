package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SPSetBlockOperation implements SPOperation {
	private final List<Change> changes;
	private final Map<BlockPos, IBlockState> blocks;
	private final Set<ChunkPos> chunks;
	private final int total;
	private long processed;
	private int index;
	private Iterator<Map.Entry<BlockPos, IBlockState>> iterator;

	public SPSetBlockOperation(List<Change> changes) {
		this.changes = Collections.unmodifiableList(new ArrayList<>(changes));
		this.blocks = null;
		this.total = changes.size();
		Set<ChunkPos> positions = new LinkedHashSet<>();
		for (Change change : changes) {
			positions.add(new ChunkPos(change.pos()));
		}
		this.chunks = Collections.unmodifiableSet(positions);
	}

	public SPSetBlockOperation(Map<BlockPos, IBlockState> blocks) {
		this.changes = null;
		this.blocks = Map.copyOf(blocks);
		this.total = this.blocks.size();
		Set<ChunkPos> positions = new LinkedHashSet<>();
		for (BlockPos pos : this.blocks.keySet()) {
			positions.add(new ChunkPos(pos));
		}
		this.chunks = Collections.unmodifiableSet(positions);
	}

	@Override
	public long total() {
		return total;
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
		if (changes != null) {
			while (used < budget && index < changes.size()) {
				Change change = changes.get(index++);
				SPBlockEditor.setBlock(world, change.pos(), change.state(), undo, recordUndo);
				processed++;
				used++;
			}
			return used;
		}
		if (iterator == null) {
			iterator = blocks.entrySet().iterator();
		}
		while (used < budget && iterator.hasNext()) {
			Map.Entry<BlockPos, IBlockState> entry = iterator.next();
			SPBlockEditor.setBlock(world, entry.getKey(), entry.getValue(), undo, recordUndo);
			processed++;
			used++;
		}
		return used;
	}

	@Override
	public boolean isDone() {
		return processed >= total;
	}

	public record Change(BlockPos pos, IBlockState state) {
	}
}
