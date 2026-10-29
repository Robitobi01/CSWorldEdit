package net.dugged.csworldedit.sp;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.LinkedHashSet;
import java.util.Set;

final class SPChunks {
	private SPChunks() {
	}

	static Set<ChunkPos> region(BlockPos min, BlockPos max) {
		Set<ChunkPos> chunks = new LinkedHashSet<>();
		for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
			for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
				chunks.add(new ChunkPos(x, z));
			}
		}
		return chunks;
	}

	static Set<ChunkPos> positions(Iterable<BlockPos> positions) {
		Set<ChunkPos> chunks = new LinkedHashSet<>();
		for (BlockPos pos : positions) {
			chunks.add(new ChunkPos(pos));
		}
		return chunks;
	}
}
