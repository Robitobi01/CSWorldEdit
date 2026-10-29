package net.dugged.csworldedit.brushes;

import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BrushBase {

	private long useCooldown;

	private IBlockState blockState;

	public abstract String getName();

	public abstract ArrayList<BlockPos> run(World world, BlockPos pos);

	public long getUseCooldown() {
		return useCooldown;
	}

	public void setUseCooldown(long useCooldown) {
		this.useCooldown = useCooldown;
	}

	public IBlockState getBlockState() {
		return blockState;
	}

	public void setBlockState(IBlockState blockState) {
		this.blockState = blockState;
	}

	public void setRadius(int radius) {
	}

	public void execute(World world, BlockPos pos) {
		List<BlockPos> positions = run(world, pos);
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		for (BlockPos blockPos : positions) {
			blocksToPlace.put(blockPos, blockState);
		}
		if (!blocksToPlace.isEmpty()) {
			TaskManager.getInstance().addTask(new TaskSetBlock(blocksToPlace, world));
		}
	}

}