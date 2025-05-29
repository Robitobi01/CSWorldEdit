package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

public class CommandCount extends ClientCommand {

	public CommandCount() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "count";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.count.usage").getUnformattedText();
	}

	private void countBlock(World world, Selection selection, IBlockState blockState, boolean exclusive,
							boolean compareStates) {
		int count = 0;
		for (BlockPos pos : BlockPos.MutableBlockPos.getAllInBox(selection.getPos(A), selection.getPos(B))) {
			if (Thread.interrupted()) {
				return;
			}
			if (exclusive) {
				if (!((compareStates && world.getBlockState(pos) == blockState) || (!compareStates && world.getBlockState(pos).getBlock() == blockState.getBlock()))) {
					count++;
				}
			} else {
				if ((compareStates && world.getBlockState(pos) == blockState) || (!compareStates && world.getBlockState(pos).getBlock() == blockState.getBlock())) {
					count++;
				}
			}
		}
		if (exclusive) {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.count.responseInclusive", count));
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.count.responseExclusive", count));
		}
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 2 || args.length == 3) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				boolean exclusive = parseBoolean(args[0]);
				Block block = getBlockByText(sender, args[1]);
				IBlockState blockState;
				boolean compareStates;
				if (args.length == 3) {
					blockState = convertArgToBlockState(block, args[2]);
					compareStates = true;
				} else {
					blockState = block.getDefaultState();
					compareStates = false;
				}
				World world = sender.getEntityWorld();
				Thread t = new Thread(() -> countBlock(world, selection, blockState, exclusive, compareStates));
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
			return getListOfStringsMatchingLastWord(args, "true", "false");
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}
