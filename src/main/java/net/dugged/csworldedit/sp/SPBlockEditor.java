package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public final class SPBlockEditor {
	public static final int FLAGS = 2 | 16;

	private SPBlockEditor() {
	}

	public static boolean setBlock(SPEditWorld world, BlockPos pos, IBlockState state, UndoSnapshot undo,
								   boolean recordUndo) {
		IBlockState oldState = world.getBlockState(pos);
		if (oldState == state) {
			return false;
		}
		if (world.isOutsideBuildHeight(pos)) {
			return false;
		}
		boolean recorded = false;
		if (recordUndo && undo != null) {
			recorded = undo.recordOriginal(pos, oldState);
		}
		Block oldBlock = oldState.getBlock();
		SPEditContext.enter();
		boolean changed;
		try {
			if (oldBlock != state.getBlock() && oldBlock.hasTileEntity(oldState)) {
				world.removeTileEntity(pos);
			}
			changed = world.setBlockState(pos, state, FLAGS);
		} finally {
			SPEditContext.exit();
		}
		if (recorded && !changed) {
			undo.removeOriginal(pos);
		}
		return changed;
	}
}
