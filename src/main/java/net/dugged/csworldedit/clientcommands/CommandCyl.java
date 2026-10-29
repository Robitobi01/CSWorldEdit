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

import static net.dugged.csworldedit.Selection.Position.A;

public class CommandCyl extends ClientCommand {

	@Override
	public String getName() {
		return "cyl";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.cyl.usage").getUnformattedText();
	}

	private void generateCyl(World world, int worldHeight, BlockPos center, IBlockState blockState, double radius,
							 int height) {
		height = net.dugged.csworldedit.Geometry.cylinderHeight(center.getY(), worldHeight, height);
		List<BlockPosBB> positions = new ArrayList<>();
		for (double x = 0; x <= radius; x++) {
			for (double z = 0; z <= radius; z++) {
				if (Thread.interrupted()) {
					return;
				}
				if (Manager.checkCircle(x, z, radius)) {
					if (!Manager.checkCircle(x + 1, z, radius) || !Manager.checkCircle(x, z + 1, radius)) {
						BlockPos pos1 = new BlockPos(center.getX() + (int) x, center.getY(), center.getZ() - (int) z);
						BlockPos pos2 = new BlockPos(center.getX() + (int) x, center.getY() + height - 1,
								center.getZ() + (int) z);
						positions.add(new BlockPosBB(pos1, pos2));
						if (net.dugged.csworldedit.Geometry.isDistinctCylinderMirror((int) x)) {
							BlockPos pos3 = new BlockPos(center.getX() - (int) x, center.getY(),
									center.getZ() - (int) z);
							BlockPos pos4 = new BlockPos(center.getX() - (int) x, center.getY() + height - 1,
									center.getZ() + (int) z);
							positions.add(new BlockPosBB(pos3, pos4));
						}
					}
				}
			}
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
		if (args.length >= 3 && args.length <= 4) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				if (selection.isOneByOne()) {
					World world = sender.getEntityWorld();
					BlockPos center = selection.getPos(A);
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = convertArgToBlockState(block, args[1]);
					int worldHeight = world.getHeight();
					double radius = Math.max(0, parseInt(args[2])) + 0.5;
					int height;
					if (args.length > 3) {
						height = Math.max(1, parseInt(args[3]));
					} else {
						height = 1;
					}
					Thread t = new Thread(() -> generateCyl(world, worldHeight, center, blockState, radius,
							height));
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
