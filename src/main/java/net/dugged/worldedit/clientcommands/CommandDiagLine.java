package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.tasks.TaskManager;
import net.dugged.worldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dugged.worldedit.WorldEditSelection.Position.A;

public class CommandDiagLine extends ClientCommand {

	@Override
	public String getName() {
		return "diagline";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.diagline.usage").getUnformattedText();
	}

	private int[] getDirection(BlockPos start) {
		int x = start.getX();
		int z = start.getZ();
		if ((x >= 0 && z >= 0) || (x < 0 && z < 0)) {
			return (x < z) ? new int[]{1, -1} : new int[]{-1, 1};
		} else {
			return (x < -z) ? new int[]{1, 1} : new int[]{-1, -1};
		}
	}

	private void generateDiagLine(World world, BlockPos start, IBlockState blockState) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		int y = start.getY();
		int[] dir = getDirection(start);
		int dx = dir[0];
		int dz = dir[1];
		BlockPos current = start;
		boolean useDiff = ((start.getX() >= 0 && start.getZ() >= 0) || (start.getX() < 0 && start.getZ() < 0));
		if (useDiff) {
			int initialDiff = start.getX() - start.getZ();
			while (true) {
				setBlocks(blockState, blocksToPlace, current);
				int currentDiff = current.getX() - current.getZ();
				if (initialDiff < 0) {
					if (currentDiff >= 0) {
						break;
					}
				} else {
					if (currentDiff <= 0) {
						break;
					}
				}
				current = current.add(dx, 0, dz);
				if (Thread.interrupted()) {
					return;
				}
			}
		} else {
			int initialSum = start.getX() + start.getZ();
			while (true) {
				setBlocks(blockState, blocksToPlace, current);
				int currentSum = current.getX() + current.getZ();
				if (initialSum < 0) {
					if (currentSum >= 0) {
						break;
					}
				} else {
					if (currentSum <= 0) {
						break;
					}
				}
				current = current.add(dx, 0, dz);
				if (Thread.interrupted()) {
					return;
				}
			}
		}
		if (!blocksToPlace.isEmpty()) {
			TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
			TaskManager.getInstance().addTask(task);
		}
	}

	private void setBlocks(IBlockState blockState, Map<BlockPos, IBlockState> blocksToPlace, BlockPos current) {
		for (int ox = -1; ox <= 1; ox++) {
			for (int oy = -1; oy <= 1; oy++) {
				BlockPos pos = new BlockPos(current.getX() + ox, current.getY() + oy, current.getZ());
				if (ox == 0 && oy == 0) {
					blocksToPlace.put(pos, blockState);
				} else {
					blocksToPlace.put(pos, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1 || args.length == 2) {
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				if (selection.isOneByOne()) {
					World world = sender.getEntityWorld();
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = (args.length == 2) ? convertArgToBlockState(block, args[1]) :
							block.getDefaultState();
					BlockPos start = selection.getPos(A);
					Thread t = new Thread(() -> generateDiagLine(world, start, blockState));
					t.start();
					TaskManager.getInstance().threads.add(t);
				} else {
					WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noOneByOneSelected"));
				}
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}