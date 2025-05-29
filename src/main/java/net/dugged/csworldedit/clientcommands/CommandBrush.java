package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.brushes.BrushBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandBrush extends ClientCommand {

	@Override
	public String getName() {
		return "brush";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.brush.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		Item itemInHand = mc.player.getHeldItemMainhand().getItem();
		if (Manager.currentBrushes.containsKey(itemInHand)) {
			Manager.currentBrushes.remove(itemInHand);
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.brush.cleared"));
		} else if (args.length > 1 && args.length < 5) {
			BrushBase selectedBrush = Manager.getBrush(args[0]);
			if (selectedBrush != null) {
				Block block = getBlockByText(sender, args[1]);
				IBlockState blockState = block.getDefaultState();
				if (args.length > 2) {
					blockState = convertArgToBlockState(block, args[2]);
				}
				selectedBrush.setBlockState(blockState);
				selectedBrush.setRadius(3);
				if (args.length > 3) {
					selectedBrush.setRadius(parseInt(args[3], 1));
				}
				Manager.currentBrushes.put(itemInHand, selectedBrush);
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.brush.bound",
						selectedBrush.getName()));
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.brush.notFound", args[0]));
			}
		} else {
			Manager.sendMessage(getUsage(sender));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		ArrayList<String> brushNames = new ArrayList<>();
		if (args.length == 1) {
			for (BrushBase brush : Manager.brushes) {
				brushNames.add(brush.getName());
			}
			return getListOfStringsMatchingLastWord(args, brushNames);
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}