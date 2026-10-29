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
	private static final int[] TUNNEL_WIDTHS = new int[]{13, 15, 15, 15, 21, 25, 25, 27, 27, 27, 25, 13, 9};

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

		int wdx = config[0];
		int wdz = config[1];
		boolean breakAlongZ = wdx != wdz;

		int dx;
		int dz;

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

		int diffX = inputPos.getX() - startPos.getX();
		int diffZ = inputPos.getZ() - startPos.getZ();

		int stepsX = -1;
		if (diffX == 0) {
			stepsX = 0;
		} else if (Integer.signum(diffX) == Integer.signum(dx)) {
			stepsX = Math.abs(diffX);
		}

		int stepsZ = -1;
		if (diffZ == 0) {
			stepsZ = 0;
		} else if (Integer.signum(diffZ) == Integer.signum(dz)) {
			stepsZ = Math.abs(diffZ);
		}

		int maxSteps;
		if (stepsX >= 0 && stepsZ >= 0) {
			maxSteps = Math.max(stepsX, stepsZ);
		} else if (stepsX >= 0) {
			maxSteps = stepsX;
		} else {
			maxSteps = stepsZ;
		}

		BlockPos current = startPos;
		for (int i = 0; i <= maxSteps; i++) {
			setBlocks(blockState, blocksToPlace, current, breakAlongZ);
			if (Thread.interrupted()) {
				return;
			}
			if (i < maxSteps) {
				current = current.add(dx, 0, dz);
			}
		}

		if (blocksToPlace.isEmpty()) {
			return;
		}

		removeMainDiagonalOverlap(blocksToPlace, startPos, config);

		BlockPos selectedCenter = new BlockPos(inputPos.getX(), 10, inputPos.getZ());
		blocksToPlace.put(selectedCenter, blockState);

		if (!blocksToPlace.isEmpty()) {
			TaskManager.getInstance().scheduleClient(
					() -> TaskManager.getInstance().addTask(new TaskSetBlock(blocksToPlace, world)));
		}
	}


	private void setBlocks(IBlockState blockState, Map<BlockPos, IBlockState> blocksToPlace, BlockPos current,
						   boolean breakAlongZ) {
		int baseY = 7;
		int centerY = 10;

		for (int i = 0; i < TUNNEL_WIDTHS.length; i++) {
			int y = baseY + i;
			int width = TUNNEL_WIDTHS[i];
			int half = width / 2;

			for (int o = -half; o <= half; o++) {
				int x = current.getX();
				int z = current.getZ();

				if (breakAlongZ) {
					z += o;
				} else {
					x += o;
				}

				BlockPos pos = new BlockPos(x, y, z);
				if (y == centerY && o == 0) {
					blocksToPlace.put(pos, blockState);
				} else {
					blocksToPlace.put(pos, Blocks.AIR.getDefaultState());
				}
			}
		}
	}

	private void removeMainDiagonalOverlap(Map<BlockPos, IBlockState> blocksToPlace, BlockPos startPos, int[] config) {
		if (blocksToPlace.isEmpty()) {
			return;
		}

		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;

		for (BlockPos pos : blocksToPlace.keySet()) {
			int x = pos.getX();
			int z = pos.getZ();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (z < minZ) minZ = z;
			if (z > maxZ) maxZ = z;
		}

		int pad = 13;
		int wdx = config[0];
		int wdz = config[1];
		boolean breakAlongZ = wdx != wdz;

		int t = 0;
		while (true) {
			BlockPos center = startPos.add(wdx * t, 0, wdz * t);
			if (!isCenterWithinBounds(center, minX, maxX, minZ, maxZ, pad)) {
				break;
			}
			removeTunnelSlice(blocksToPlace, center, breakAlongZ);
			t++;
		}

		t = -1;
		while (true) {
			BlockPos center = startPos.add(wdx * t, 0, wdz * t);
			if (!isCenterWithinBounds(center, minX, maxX, minZ, maxZ, pad)) {
				break;
			}
			removeTunnelSlice(blocksToPlace, center, breakAlongZ);
			t--;
		}
	}

	private boolean isCenterWithinBounds(BlockPos center, int minX, int maxX, int minZ, int maxZ, int pad) {
		int x = center.getX();
		int z = center.getZ();
		return x >= minX - pad && x <= maxX + pad && z >= minZ - pad && z <= maxZ + pad;
	}

	private void removeTunnelSlice(Map<BlockPos, IBlockState> blocksToPlace, BlockPos current, boolean breakAlongZ) {
		int baseY = 7;

		for (int i = 0; i < TUNNEL_WIDTHS.length; i++) {
			int y = baseY + i;
			int width = TUNNEL_WIDTHS[i];
			int half = width / 2;

			for (int o = -half; o <= half; o++) {
				int x = current.getX();
				int z = current.getZ();

				if (breakAlongZ) {
					z += o;
				} else {
					x += o;
				}

				BlockPos pos = new BlockPos(x, y, z);
				blocksToPlace.remove(pos);
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