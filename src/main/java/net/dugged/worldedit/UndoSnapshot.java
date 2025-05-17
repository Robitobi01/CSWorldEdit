package net.dugged.worldedit;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class UndoSnapshot {

	private final Map<BlockPos, IBlockState> snapshot;

	public UndoSnapshot(Map<BlockPos, IBlockState> snapshot) {
		this.snapshot = new HashMap<>(snapshot);
	}

	public Map<BlockPos, IBlockState> getSnapshot() {
		return snapshot;
	}
}