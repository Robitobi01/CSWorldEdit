package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dugged.csworldedit.Selection.Position.A;

public class CommandHSphere extends ClientCommand {

	@Override
	public String getName() {
		return "hsphere";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.hsphere.usage").getUnformattedText();
	}

	private void generateHollowSphere(World world, int worldHeight, BlockPos center, IBlockState blockState,
									  double radius) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		double yMax = net.dugged.csworldedit.Geometry.sphereYMax(center.getY(), worldHeight, radius);
		for (double x = 0; x <= radius; x++) {
			for (double y = 0; y <= yMax; y++) {
				for (double z = 0; z <= radius; z++) {
					if (Thread.interrupted()) {
						return;
					}
					if (Manager.checkSphere(x, y, z, radius)) {
						if (!Manager.checkSphere(x + 1, y, z, radius) || !Manager.checkSphere(x, y + 1, z, radius) || !Manager.checkSphere(x, y, z + 1, radius)) {
							blocksToPlace.put(new BlockPos(center.getX() + (int) x, center.getY() + (int) y,
									center.getZ() + (int) z), blockState);
							blocksToPlace.put(new BlockPos(center.getX() + (int) x, center.getY() + (int) y,
									center.getZ() - (int) z), blockState);
							blocksToPlace.put(new BlockPos(center.getX() - (int) x, center.getY() + (int) y,
									center.getZ() + (int) z), blockState);
							blocksToPlace.put(new BlockPos(center.getX() - (int) x, center.getY() + (int) y,
									center.getZ() - (int) z), blockState);
							if (center.getY() - (int) y >= 0) {
								blocksToPlace.put(new BlockPos(center.getX() + (int) x, center.getY() - (int) y,
										center.getZ() + (int) z), blockState);
								blocksToPlace.put(new BlockPos(center.getX() + (int) x, center.getY() - (int) y,
										center.getZ() - (int) z), blockState);
								blocksToPlace.put(new BlockPos(center.getX() - (int) x, center.getY() - (int) y,
										center.getZ() + (int) z), blockState);
								blocksToPlace.put(new BlockPos(center.getX() - (int) x, center.getY() - (int) y,
										center.getZ() - (int) z), blockState);
							}
						}
					}
				}
			}
		}
		TaskManager.getInstance().scheduleClient(
				() -> TaskManager.getInstance().addTask(new TaskSetBlock(blocksToPlace, world)));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 3) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				if (selection.isOneByOne()) {
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = convertArgToBlockState(block, args[1]);
					World world = sender.getEntityWorld();
					int worldHeight = world.getHeight();
					BlockPos center = selection.getPos(A);
					double radius = Math.max(0, parseInt(args[2])) + 0.5;
					Thread t = new Thread(() -> generateHollowSphere(world, worldHeight, center, blockState, radius));
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