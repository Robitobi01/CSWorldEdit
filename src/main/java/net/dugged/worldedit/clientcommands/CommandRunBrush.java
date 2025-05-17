package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.brushes.BrushBase;
import net.dugged.worldedit.tasks.TaskManager;
import net.dugged.worldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

public class CommandRunBrush extends ClientCommand {

	@Override
	public String getName() {
		return "runbrush";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation(
				"text.worldedit.runbrush.usage").getUnformattedText();
	}

	private void runBrush(World world, WorldEditSelection selection, IBlockState maskBlockState,
						  BrushBase brush) {
		List<BlockPos> positions = new ArrayList<>();
		for (BlockPos pos : BlockPos.getAllInBox(selection.getPos(A), selection.getPos(B))) {
			if (Thread.interrupted()) {
				return;
			}
			if (world.getBlockState(pos).equals(maskBlockState)) {
				positions.addAll(brush.run(world, pos));
			}
		}
		if (!positions.isEmpty()) {
			Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
			for (BlockPos pos : positions) {
				blocksToPlace.put(pos, brush.getBlockState());
			}
			TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
			TaskManager.getInstance().addTask(task);
		}
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args)
			throws CommandException {
		if (WorldEditManager.hasCurrentSelection()) {
			if (args.length > 3 && args.length < 7) {
				Block maskBlock = getBlockByText(sender, args[0]);
				IBlockState maskBlockState = convertArgToBlockState(maskBlock, args[1]);
				BrushBase brush;
				try {
					brush = WorldEditManager.getBrush(args[2]).getClass().newInstance();
				} catch (Exception e) {
					brush = null;
				}
				if (brush != null) {
					Block block = getBlockByText(sender, args[3]);
					IBlockState blockState = block.getDefaultState();
					if (args.length > 4) {
						blockState = convertArgToBlockState(block, args[4]);
					}
					brush.setBlockState(blockState);
					brush.setRadius(3);
					if (args.length > 5) {
						brush.setRadius(parseInt(args[5], 1));
					}
					BrushBase finalBrush = brush;
					Thread t = new Thread(
							() -> runBrush(sender.getEntityWorld(), WorldEditManager.getCurrentSelection(),
									maskBlockState, finalBrush));
					t.start();
					TaskManager.getInstance().threads.add(t);
				} else {
					WorldEditManager.sendMessage(new TextComponentTranslation(
							"text.worldedit.brush.notFound", args[0]));
				}
			} else {
				WorldEditManager.sendMessage(getUsage(sender));
			}
		} else {
			WorldEditManager.sendMessage(new TextComponentTranslation(
					"text.worldedit.noAreaSelected"));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
										  String[] args, @Nullable BlockPos pos) {
		List<String> result = new ArrayList<>();
		if (args.length == 3) {
			for (BrushBase brush : WorldEditManager.brushes) {
				result.add(brush.getName());
			}
			return getListOfStringsMatchingLastWord(args, result);
		} else if (args.length == 1 || args.length == 4) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}