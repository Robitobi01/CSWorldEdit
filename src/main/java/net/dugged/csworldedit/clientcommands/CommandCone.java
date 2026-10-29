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
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandCone extends ClientCommand {

	@Override
	public String getName() {
		return "cone";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.cone.usage").getUnformattedText();
	}

	private List<BlockPosBB> generateCircleBBoxes(BlockPos center, double radius) {
		List<BlockPosBB> positions = new ArrayList<>();
		for (double x = 0; x <= radius; x++) {
			for (double z = 0; z <= radius; z++) {
				if (Thread.interrupted()) {
					return positions;
				}
				if (Manager.checkCircle(x, z, radius)) {
					if (!Manager.checkCircle(x + 1, z, radius) || !Manager.checkCircle(x, z + 1, radius)) {
						BlockPos pos1 = new BlockPos(center.getX() + (int) x, center.getY(), center.getZ() - (int) z);
						BlockPos pos2 = new BlockPos(center.getX() + (int) x, center.getY(), center.getZ() + (int) z);
						positions.add(new BlockPosBB(pos1, pos2));
						BlockPos pos3 = new BlockPos(center.getX() - (int) x, center.getY(), center.getZ() - (int) z);
						BlockPos pos4 = new BlockPos(center.getX() - (int) x, center.getY(), center.getZ() + (int) z);
						positions.add(new BlockPosBB(pos3, pos4));
					}
				}
			}
		}
		return positions;
	}

	public int calculateBezierValue(int start, int end, double pointA, double pointB, double t) {
		double u = 1.0 - t;
		double tt = t * t;
		double uu = u * u;
		double ttt = tt * t;
		double x = 3 * uu * t * pointA + 3 * u * tt * pointB + ttt;
		return (int) (start + (end - start) * x);
	}

	private void generateCone(World world, Selection selection, IBlockState blockState, int startRadius, int endRadius
			, int height, double bezierA, double bezierB) {
		double firstT = 1.0D / height;
		int baseRadius = Math.max(startRadius,
				calculateBezierValue(startRadius, endRadius, bezierA, bezierB, firstT));
		List<BlockPosBB> positions = generateCircleBBoxes(selection.minPos().down(height - 1), baseRadius);
		for (int i = 2; i <= height; i++) {
			if (Thread.interrupted()) {
				return;
			}
			double t = (1.0D / height) * i;
			int currentRadius = calculateBezierValue(startRadius, endRadius, bezierA, bezierB, t);
			positions.addAll(generateCircleBBoxes(selection.minPos().down(height - i), currentRadius));
		}
		TaskManager.getInstance().scheduleClient(() -> {
			UndoSnapshot snapshot = new UndoSnapshot();
			for (BlockPosBB bb : positions) {
				TaskFill task = new TaskFill(bb.getPos1(), bb.getPos2(), blockState, world, snapshot);
				TaskManager.getInstance().addTask(task);
			}
			UndoManager.getInstance().pushSnapshot(snapshot);
		});
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 7) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				if (selection.isOneByOne()) {
					World world = sender.getEntityWorld();
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = convertArgToBlockState(block, args[1]);
					int startRadius = Math.max(1, parseInt(args[2]));
					int endRadius = Math.max(1, parseInt(args[3]));
					int height = Math.max(1, parseInt(args[4]));
					double bezierA = parseDouble(args[5]);
					double bezierB = parseDouble(args[6]);
					Selection snapshot = selection.copy();
					Thread t = new Thread(() -> generateCone(world, snapshot, blockState, startRadius, endRadius,
							height, bezierA, bezierB));
					t.start();
					TaskManager.getInstance().threads.add(t);
				} else {
					Manager.sendMessage(new TextComponentTranslation("text.worldedit.noOneByOneSelected"));
				}
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