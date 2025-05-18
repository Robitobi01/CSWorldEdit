package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.tasks.TaskFill;
import net.dugged.worldedit.tasks.TaskManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.dugged.worldedit.WorldEditSelection.Position.A;

public class CommandHCyl extends ClientCommand {

	@Override
	public String getName() {
		return "hcyl";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.hcyl.usage").getUnformattedText();
	}

	private void generateHCyl(World world, BlockPos center, IBlockState blockState, double radius, int height) {
		if (height > world.getHeight() - center.getY()) {
			height = world.getHeight() - center.getY();
		}
		Map<AxisAlignedBB, IBlockState> regionMap = new LinkedHashMap<>();
		int intRadius = (int) radius;
		for (int x = 0; x <= intRadius; x++) {
			for (int z = 0; z <= intRadius; z++) {
				if (Thread.interrupted()) {
					return;
				}
				if (WorldEditManager.checkCircle(x, z, radius)) {
					if (!WorldEditManager.checkCircle(x + 1, z, radius) || !WorldEditManager.checkCircle(x, z + 1,
							radius)) {
						if (x == 0 && z == 0) {
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX(), center.getY(), center.getZ()),
											new BlockPos(center.getX(), center.getY() + height - 1, center.getZ())),
									blockState);
						} else if (x == 0) {
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX(), center.getY(),
									center.getZ() + z), new BlockPos(center.getX(), center.getY() + height - 1,
									center.getZ() + z)), blockState);
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX(), center.getY(),
									center.getZ() - z), new BlockPos(center.getX(), center.getY() + height - 1,
									center.getZ() - z)), blockState);
						} else if (z == 0) {
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() + x, center.getY(),
									center.getZ()), new BlockPos(center.getX() + x, center.getY() + height - 1,
									center.getZ())), blockState);
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() - x, center.getY(),
									center.getZ()), new BlockPos(center.getX() - x, center.getY() + height - 1,
									center.getZ())), blockState);
						} else {
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() + x, center.getY(),
									center.getZ() + z), new BlockPos(center.getX() + x, center.getY() + height - 1,
									center.getZ() + z)), blockState);
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() + x, center.getY(),
									center.getZ() - z), new BlockPos(center.getX() + x, center.getY() + height - 1,
									center.getZ() - z)), blockState);
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() - x, center.getY(),
									center.getZ() + z), new BlockPos(center.getX() - x, center.getY() + height - 1,
									center.getZ() + z)), blockState);
							regionMap.put(new AxisAlignedBB(new BlockPos(center.getX() - x, center.getY(),
									center.getZ() - z), new BlockPos(center.getX() - x, center.getY() + height - 1,
									center.getZ() - z)), blockState);
						}
					}
				}
			}
		}
		TaskFill task = new TaskFill(regionMap, world);
		TaskManager.getInstance().addTask(task);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length >= 3 && args.length <= 4) {
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				if (selection.isOneByOne()) {
					World world = sender.getEntityWorld();
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = convertArgToBlockState(block, args[1]);
					double radius = parseInt(args[2]) + 0.5;
					int height = (args.length > 3) ? Math.max(1, parseInt(args[3])) : 1;
					Thread t = new Thread(() -> generateHCyl(world, selection.getPos(A), blockState, radius, height));
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

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}