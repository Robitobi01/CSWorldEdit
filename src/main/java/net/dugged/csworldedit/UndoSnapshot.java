package net.dugged.csworldedit;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UndoSnapshot {

	private final Map<BlockPos, IBlockState> undoData = new HashMap<>();

	public void recordPositions(Set<BlockPos> positions, World world) {
		for (BlockPos pos : positions) {
			undoData.put(pos, world.getBlockState(pos));
		}
	}

	public void recordRegion(BlockPos pos1, BlockPos pos2, World world) {
		int x1 = Math.min(pos1.getX(), pos2.getX());
		int y1 = Math.min(pos1.getY(), pos2.getY());
		int z1 = Math.min(pos1.getZ(), pos2.getZ());
		int x2 = Math.max(pos1.getX(), pos2.getX());
		int y2 = Math.max(pos1.getY(), pos2.getY());
		int z2 = Math.max(pos1.getZ(), pos2.getZ());
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					undoData.put(pos, world.getBlockState(pos));
				}
			}
		}
	}

	public Map<BlockPos, IBlockState> getUndoData() {
		return undoData;
	}
}