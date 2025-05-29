package net.dugged.csworldedit.tasks;

import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public abstract class TaskChunk extends AbstractTask {

	public abstract int processChunk(ChunkPos pos, int maxPackets);

	public abstract int getTotalOperations();

	public abstract int getProcessedOperations();

	public abstract Set<ChunkPos> getPendingChunks();
}
