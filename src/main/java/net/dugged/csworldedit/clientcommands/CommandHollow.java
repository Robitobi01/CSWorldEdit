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
				int[][] faces = net.dugged.csworldedit.Geometry.hollowFaces(posMin.getX(), posMin.getY(),
						posMin.getZ(), posMax.getX(), posMax.getY(), posMax.getZ(), thicknessX, thicknessY,
						thicknessZ);
				// Front face (posMin.z side)
				positions.add(new BlockPosBB(new BlockPos(faces[0][0], faces[0][1], faces[0][2]),
						new BlockPos(faces[0][3], faces[0][4], faces[0][5])));
				// Back face (posMax.z side)
				positions.add(new BlockPosBB(new BlockPos(faces[1][0], faces[1][1], faces[1][2]),
						new BlockPos(faces[1][3], faces[1][4], faces[1][5])));
				// Left face (posMin.x side)
				positions.add(new BlockPosBB(new BlockPos(faces[2][0], faces[2][1], faces[2][2]),
						new BlockPos(faces[2][3], faces[2][4], faces[2][5])));
				// Right face (posMax.x side)
				positions.add(new BlockPosBB(new BlockPos(faces[3][0], faces[3][1], faces[3][2]),
						new BlockPos(faces[3][3], faces[3][4], faces[3][5])));
				// Bottom face (posMin.y side)
				positions.add(new BlockPosBB(new BlockPos(faces[4][0], faces[4][1], faces[4][2]),
						new BlockPos(faces[4][3], faces[4][4], faces[4][5])));
				// Top face (posMax.y side)
				positions.add(new BlockPosBB(new BlockPos(faces[5][0], faces[5][1], faces[5][2]),
						new BlockPos(faces[5][3], faces[5][4], faces[5][5])));
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