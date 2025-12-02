package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
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

public class CommandDiagLine extends ClientCommand {
	@Override
	public String getName() {
		return "diagline";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.diagline.usage").getUnformattedText();
	}

	private int[] getQuadConfig(int x, int z) {
		if (x > 0 && z <= 0) {
			// north east (+x, -z)
			return new int[]{1, -1, 13, -9};
		} else if (x <= 0 && z < 0) {
			// north west (-x, -z)
			return new int[]{-1, -1, -9, -13};
		} else if (x < 0) {
			// south west (-x, +z)
			return new int[]{-1, 1, -13, 9};
		} else {
			// south east (+x, +z)
			return new int[]{1, 1, 9, 13};
		}
	}

	private BlockPos getSnappedStart(BlockPos target, int[] config) {
		int dx = config[0];
		int dz = config[1];
		int rx = config[2];
		int rz = config[3];
		double dist;
		if (dx == dz) {
			dist = (target.getX() - rx + target.getZ() - rz) / 2.0;
		} else {
			dist = (target.getX() - rx - (target.getZ() - rz)) / 2.0;
		}
		if (dx < 0) {
			dist = -dist;
		}
		long steps = Math.round(dist / 10.0);
		return new BlockPos(rx + steps * 10 * dx, target.getY(), rz + steps * 10 * dz);
	}

	private BlockPos getIntersection(BlockPos pos, int[] config) {
		int dx = config[0];
		int dz = config[1];
		int rx = config[2];
		int rz = config[3];
		int px = pos.getX();
		int pz = pos.getZ();
		long ix, iz;
		if (dx == dz) {
			long cw = rz - rx;
			long k = pz + px;
			ix = (k - cw) / 2;
			iz = ix + cw;
		} else {
			long cw = rz + rx;
			long k = pz - px;
			ix = (cw - k) / 2;
			iz = -ix + cw;
		}
		return new BlockPos((int) ix, pos.getY(), (int) iz);
	}

	private void generateDiagLine(World world, BlockPos inputPos, IBlockState blockState) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		int[] config = getQuadConfig(inputPos.getX(), inputPos.getZ());
		BlockPos intersection = getIntersection(inputPos, config);
		BlockPos startPos = getSnappedStart(intersection, config);

		int dx, dz;
		int wdx = config[0];
		int wdz = config[1];

		if (wdx == wdz) {
			if (inputPos.getX() < startPos.getX()) {
				dx = -1;
				dz = 1;
			} else {
				dx = 1;
				dz = -1;
			}
		} else {
			if (inputPos.getX() < startPos.getX()) {
				dx = -1;
				dz = -1;
			} else {
				dx = 1;
				dz = 1;
			}
		}

		BlockPos current = startPos;
		double currentDistSq = current.distanceSq(inputPos);

		while (true) {
			setBlocks(blockState, blocksToPlace, current);
			BlockPos next = current.add(dx, 0, dz);
			double nextDistSq = next.distanceSq(inputPos);
			if (nextDistSq >= currentDistSq) {
				break;
			}
			current = next;
			currentDistSq = nextDistSq;
			if (Thread.interrupted()) {
				return;
			}
		}
		if (!blocksToPlace.isEmpty()) {
			TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
			TaskManager.getInstance().addTask(task);
		}
	}

	private void setBlocks(IBlockState blockState, Map<BlockPos, IBlockState> blocksToPlace, BlockPos current) {
		for (int ox = -1; ox <= 1; ox++) {
			for (int oy = -1; oy <= 1; oy++) {
				BlockPos pos = new BlockPos(current.getX() + ox, current.getY() + oy, current.getZ());
				if (ox == 0 && oy == 0) {
					blocksToPlace.put(pos, blockState);
				} else {
					blocksToPlace.put(pos, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1 || args.length == 2) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				if (selection.isOneByOne()) {
					World world = sender.getEntityWorld();
					Block block = getBlockByText(sender, args[0]);
					IBlockState blockState = (args.length == 2) ? convertArgToBlockState(block, args[1]) :
							block.getDefaultState();
					BlockPos start = selection.getPos(A);
					Thread t = new Thread(() -> generateDiagLine(world, start, blockState));
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