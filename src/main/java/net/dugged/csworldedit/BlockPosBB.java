package net.dugged.csworldedit;

import net.minecraft.util.math.BlockPos;

public class BlockPosBB {
	private final BlockPos pos1;
	private final BlockPos pos2;

	public BlockPosBB(BlockPos pos2, BlockPos pos1) {
		this.pos2 = pos2;
		this.pos1 = pos1;
	}

	public BlockPos getPos1() {
		return pos1;
	}

	public BlockPos getPos2() {
		return pos2;
	}
}
