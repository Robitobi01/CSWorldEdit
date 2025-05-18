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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandCenter extends ClientCommand {

	@Override
	public String getName() {
		return "center";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.center.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (WorldEditManager.hasCurrentSelection()) {
			if (args.length <= 2) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				Block block = Blocks.GLOWSTONE;
				if (args.length > 0) {
					block = getBlockByText(sender, args[0]);
				}
				IBlockState blockState = block.getDefaultState();
				if (args.length == 2) {
					blockState = convertArgToBlockState(block, args[1]);
				}
				Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
				BlockPos center = new BlockPos(selection.minPos().getX() + selection.widthX() / 2,
						selection.minPos().getY() + selection.widthY() / 2,
						selection.minPos().getZ() + selection.widthZ() / 2);
				blocksToPlace.put(center, blockState);
				boolean x = selection.widthX() % 2 == 0;
				boolean y = selection.widthY() % 2 == 0;
				boolean z = selection.widthZ() % 2 == 0;
				if (x) {
					blocksToPlace.put(center.west(), blockState);
				}
				if (y) {
					blocksToPlace.put(center.down(), blockState);
				}
				if (z) {
					blocksToPlace.put(center.north(), blockState);
				}
				if (x && y) {
					blocksToPlace.put(center.west().down(), blockState);
				}
				if (y && z) {
					blocksToPlace.put(center.down().north(), blockState);
				}
				if (x && z) {
					blocksToPlace.put(center.west().north(), blockState);
				}
				if (x && y && z) {
					blocksToPlace.put(center.west().down().north(), blockState);
				}
				if (!blocksToPlace.isEmpty()) {
					TaskSetBlock task = new TaskSetBlock(blocksToPlace, sender.getEntityWorld());
					TaskManager.getInstance().addTask(task);
				}
			} else {
				WorldEditManager.sendMessage(getUsage(sender));
			}
		} else {
			WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
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
