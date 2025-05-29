package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.*;
import net.dugged.csworldedit.tasks.TaskFill;
import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandWalls extends ClientCommand {

	@Override
	public String getName() {
		return "walls";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.walls.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			if (args.length > 0 && args.length <= 3) {
				Block block = getBlockByText(sender, args[0]);
				IBlockState blockState = block.getDefaultState();
				int thickness = 0;
				if (args.length >= 2) {
					blockState = convertArgToBlockState(block, args[1]);
				}
				if (args.length == 3) {
					thickness = parseInt(args[2]) - 1;
					if (thickness <= 0) {
						thickness = 0;
					}
				}
				Selection selection = Manager.getCurrentSelection();
				BlockPos posMin = selection.minPos();
				BlockPos posMax = selection.maxPos();
				int wallThicknessZ = Math.min(thickness, selection.widthZ() - 1);
				int wallThicknessX = Math.min(thickness, selection.widthX() - 1);
				List<BlockPosBB> positions = new ArrayList<>();
				positions.add(new BlockPosBB(posMin, new BlockPos(posMax.getX(), posMax.getY(),
						posMin.getZ() + wallThicknessZ)));
				positions.add(new BlockPosBB(posMin, new BlockPos(posMin.getX() + wallThicknessX, posMax.getY(),
						posMax.getZ())));
				positions.add(new BlockPosBB(new BlockPos(posMin.getX(), posMin.getY(),
						posMax.getZ() - wallThicknessZ), posMax));
				positions.add(new BlockPosBB(new BlockPos(posMax.getX() - wallThicknessX, posMin.getY(),
						posMin.getZ()), posMax));
				UndoSnapshot snapshot = new UndoSnapshot();
				for (BlockPosBB bb : positions) {
					TaskFill task = new TaskFill(bb.getPos1(), bb.getPos2(), blockState, sender.getEntityWorld(),
							snapshot);
					TaskManager.getInstance().addTask(task);
				}
				UndoManager.getInstance().pushSnapshot(snapshot);
			} else {
				Manager.sendMessage(getUsage(sender));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}