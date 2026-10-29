package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public final class SPFillOperation implements SPOperation {
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;
	private final IBlockState state;
	private final long total;
	private final Set<ChunkPos> chunks;
	private int x;
	private int y;
	private int z;
	private long processed;

	public SPFillOperation(BlockPos pos1, BlockPos pos2, IBlockState state) {
		BlockPos min = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()),
				Math.min(pos1.getZ(), pos2.getZ()));
		BlockPos max = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()),
				Math.max(pos1.getZ(), pos2.getZ()));
		this.minX = min.getX();
		this.minY = min.getY();
		this.minZ = min.getZ();
		this.maxX = max.getX();
		this.maxY = max.getY();
		this.maxZ = max.getZ();
		this.state = state;
		this.total = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		this.chunks = SPChunks.region(min, max);
		this.x = minX;
		this.y = minY;
		this.z = minZ;
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
		while (used < budget && !isDone()) {
			SPBlockEditor.setBlock(world, new BlockPos(x, y, z), state, undo, recordUndo);
			processed++;
			used++;
			x++;
			if (x > maxX) {
				x = minX;
				z++;
				if (z > maxZ) {
					z = minZ;
					y++;
				}
			}
		}
		return used;
	}

	@Override
	public boolean isDone() {
		return y > maxY;
	}
}
