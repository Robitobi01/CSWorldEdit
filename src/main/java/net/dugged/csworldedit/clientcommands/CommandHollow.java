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

public class CommandHollow extends ClientCommand {

	@Override
	public String getName() {
		return "hollow";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.hollow.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			Selection selection = Manager.getCurrentSelection();
			if (args.length > 0 && args.length <= 3) {
				Block block = getBlockByText(sender, args[0]);
				IBlockState blockState = block.getDefaultState();
				if (args.length >= 2) {
					blockState = convertArgToBlockState(block, args[1]);
				}
				int thickness = 0;
				if (args.length == 3) {
					thickness = parseInt(args[2]) - 1;
					if (thickness <= 0) {
						thickness = 0;
					}
				}
				BlockPos posMin = selection.minPos();
				BlockPos posMax = selection.maxPos();
				int thicknessX = Math.min(thickness, selection.widthX() - 1);
				int thicknessY = Math.min(thickness, selection.widthY() - 1);
				int thicknessZ = Math.min(thickness, selection.widthZ() - 1);

				List<BlockPosBB> positions = new ArrayList<>();
				BlockPos pos;
				// Front face (posMin.z side)
				pos = new BlockPos(posMax.getX(), posMax.getY(), posMin.getZ() + thicknessZ);
				positions.add(new BlockPosBB(posMin, pos));
				// Left face (posMin.x side)
				pos = new BlockPos(posMax.getX() + thicknessX, posMax.getY(), posMin.getZ());
				positions.add(new BlockPosBB(posMin, pos));
				// Bottom face (posMin.y side)
				pos = new BlockPos(posMax.getX(), posMax.getY() + thicknessY, posMin.getZ());
				positions.add(new BlockPosBB(posMin, pos));
				// Back face (posMax.z side)
				pos = new BlockPos(posMax.getX(), posMax.getY(), posMin.getZ() - thicknessZ);
				positions.add(new BlockPosBB(pos, posMax));
				// Right face (posMax.x side)
				pos = new BlockPos(posMax.getX(), posMax.getY() - thicknessX, posMin.getZ());
				positions.add(new BlockPosBB(pos, posMax));
				// Top face (posMax.y side)
				pos = new BlockPos(posMax.getX(), posMax.getY() - thicknessY, posMin.getZ());
				positions.add(new BlockPosBB(pos, posMax));
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

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}