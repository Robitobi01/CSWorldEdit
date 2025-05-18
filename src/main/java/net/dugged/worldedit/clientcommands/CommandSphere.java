package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dugged.worldedit.WorldEditSelection.Position.A;

public class CommandSphere extends ClientCommand {

	@Override
	public String getName() {
		return "sphere";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.sphere.usage").getUnformattedText();
	}

	private void addSphereColumn(Map<BlockPos, IBlockState> blocksToPlace, BlockPos center, int offsetX, int offsetZ,
								 int y, IBlockState state) {
		BlockPos posTop = new BlockPos(center.getX() + offsetX, center.getY() + y, center.getZ() + offsetZ);
		blocksToPlace.put(posTop, state);
		int bottomY = Math.max(0, center.getY() - y);
		BlockPos posBottom = new BlockPos(center.getX() + offsetX, bottomY, center.getZ() + offsetZ);
		blocksToPlace.put(posBottom, state);
	}

	private void generateSphere(World world, BlockPos center, IBlockState blockState, double radius) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		int yMax = (int) Math.min(radius, world.getHeight() - center.getY());
		for (int x = 0; x <= (int) radius; x++) {
			for (int y = 0; y <= yMax; y++) {
				for (int z = 0; z <= (int) radius; z++) {
					if (Thread.interrupted()) {
						return;
					}
					if (WorldEditManager.checkSphere(x, y, z, radius)) {
						if (!WorldEditManager.checkSphere(x + 1, y, z, radius) || !WorldEditManager.checkSphere(x,
								y + 1, z, radius) || !WorldEditManager.checkSphere(x, y, z + 1, radius)) {
							addSphereColumn(blocksToPlace, center, x, z, y, blockState);
							addSphereColumn(blocksToPlace, center, x, -z, y, blockState);
							addSphereColumn(blocksToPlace, center, -x, z, y, blockState);
							addSphereColumn(blocksToPlace, center, -x, -z, y, blockState);
						}
					}
				}
			}
		}
		TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
		TaskManager.getInstance().addTask(task);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 3) {
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				if (selection.isOneByOne()) {
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = convertArgToBlockState(block, args[1]);
					World world = sender.getEntityWorld();
					double radius = parseInt(args[2]) + 0.5;
					Thread t = new Thread(() -> generateSphere(world, selection.getPos(A), blockState, radius));
					t.start();
					TaskManager.getInstance().threads.add(t);
				} else {
					WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noOneByOneSelected"));
				}
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
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