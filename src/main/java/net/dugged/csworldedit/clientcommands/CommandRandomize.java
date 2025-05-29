package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class CommandRandomize extends ClientCommand {

	@Override
	public String getName() {
		return "randomize";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.randomize.usage").getUnformattedText();
	}

	private List<IBlockState> parseBlockList(String[] argList, ICommandSender sender) throws CommandException {
		List<IBlockState> blockList = new ArrayList<>();
		for (String args : String.join(" ", argList).split(",")) {
			String[] arg = args.trim().split(" ");
			if (arg.length <= 2) {
				Block block = getBlockByText(sender, arg[0]);
				IBlockState blockState = block.getDefaultState();
				if (arg.length == 2) {
					blockState = convertArgToBlockState(block, arg[1]);
				}
				blockList.add(blockState);
			}
		}
		return blockList;
	}

	private void placeRandomBlocks(World world, Selection selection, List<IBlockState> blockList, int percentage) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		Random rand = new Random();
		for (BlockPos pos : BlockPos.MutableBlockPos.getAllInBox(selection.minPos(), selection.maxPos())) {
			if (Thread.interrupted()) {
				return;
			}
			if (rand.nextFloat() <= (float) percentage / 100F) {
				IBlockState blockState = blockList.get(rand.nextInt(blockList.size()));
				blocksToPlace.put(pos, blockState);
			}
		}
		TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
		TaskManager.getInstance().addTask(task);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length >= 2) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, 0);
				int percentage = parseInt(args[0], 0, 100);
				World world = sender.getEntityWorld();
				List<IBlockState> blockList = parseBlockList(Arrays.copyOfRange(args, 1, args.length), sender);
				Thread t = new Thread(() -> placeRandomBlocks(world, selection, blockList, percentage));
				t.start();
				TaskManager.getInstance().threads.add(t);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(getUsage(sender));
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