package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandErode extends ClientCommand {

	private static final int[][] OFFSETS = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0,
			-1}};

	@Override
	public String getName() {
		return "erode";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.erode.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection().copy();
				final World world = sender.getEntityWorld();
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world,
						new ClientWorldView(world), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(),
						max.getZ(), (snapshot, context) -> {
							Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
							int work = 0;
							for (int y = snapshot.minY(); y < snapshot.minY() + snapshot.sizeY(); y++) {
								for (int z = snapshot.minZ(); z < snapshot.minZ() + snapshot.sizeZ(); z++) {
									for (int x = snapshot.minX(); x < snapshot.minX() + snapshot.sizeX(); x++) {
										if ((work++ & 255) == 0 && context.isCancelled()) {
											return;
										}
										IBlockState state = snapshot.get(x, y, z);
										if (state == null || state.getMaterial() == Material.AIR) {
											continue;
										}
										boolean surrounded = true;
										for (int[] offset : OFFSETS) {
											int nx = x + offset[0];
											int ny = y + offset[1];
											int nz = z + offset[2];
											if (nx < min.getX() || nx > max.getX() || ny < min.getY() || ny > max.getY()
													|| nz < min.getZ() || nz > max.getZ()) {
												continue;
											}
											IBlockState neighbor = snapshot.get(nx, ny, nz);
											if (neighbor == null || !neighbor.isFullBlock()) {
												surrounded = false;
												break;
											}
										}
										if (surrounded) {
											blocksToPlace.put(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
										}
									}
								}
							}
							if (!blocksToPlace.isEmpty()) {
								context.postClient(clientWorld -> TaskManager.getInstance()
										.addTask(new TaskSetBlock(blocksToPlace, clientWorld)));
							}
						});
				ScanFeedback.report(result, selection);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(getUsage(sender));
		}
	}
}
