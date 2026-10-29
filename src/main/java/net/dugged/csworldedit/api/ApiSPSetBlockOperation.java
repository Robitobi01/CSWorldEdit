package net.dugged.csworldedit.api;

import net.dugged.csworldedit.UndoSnapshot;
import net.dugged.csworldedit.sp.SPBlockEditor;
import net.dugged.csworldedit.sp.SPEditWorld;
import net.dugged.csworldedit.sp.SPOperation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ApiSPSetBlockOperation implements SPOperation {
	private final List<BlockChange> changes;
	private final Set<ChunkPos> chunks;
	private long processed;
	private int index;

	ApiSPSetBlockOperation(List<BlockChange> changes) {
		this.changes = Collections.unmodifiableList(new ArrayList<>(changes));
		Set<ChunkPos> positions = new LinkedHashSet<>();
		for (BlockChange change : changes) {
			positions.add(new ChunkPos(change.pos()));
		}
		this.chunks = Collections.unmodifiableSet(positions);
	}

	@Override
	public long total() {
		return changes.size();
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
		while (used < budget && index < changes.size()) {
			BlockChange change = changes.get(index++);
			BlockPos pos = change.pos();
			if (!world.isOutsideBuildHeight(pos)) {
				boolean changed = SPBlockEditor.setBlock(world, pos, change.state(), undo, recordUndo);
				NBTTagCompound nbt = change.rawTileEntityNbt();
				if (nbt != null && (changed || world.getBlockState(pos) == change.state())) {
					world.setTileEntityNbt(pos, nbt.copy());
				}
			}
			processed++;
			used++;
		}
		return used;
	}

	@Override
	public boolean isDone() {
		return processed >= changes.size();
	}
}
