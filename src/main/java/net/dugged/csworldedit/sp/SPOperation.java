package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public interface SPOperation {
	long total();

	long processed();

	Set<ChunkPos> chunks();

	int apply(SPEditWorld world, int budget, UndoSnapshot undo, boolean recordUndo);

	boolean isDone();
}
