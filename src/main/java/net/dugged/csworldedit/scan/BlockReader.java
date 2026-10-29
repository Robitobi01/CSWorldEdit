package net.dugged.csworldedit.scan;

import net.minecraft.block.state.IBlockState;

public interface BlockReader {

	IBlockState getBlock(int x, int y, int z);
}
