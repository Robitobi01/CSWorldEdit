package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandUpscale extends ClientCommand {

	@Override
	public String getName() {
		return "upscale";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.upscale.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			Selection selection = Manager.getCurrentSelection().copy();
			if (args.length == 1) {
				int factor = Math.max(1, parseInt(args[0]));
				World world = sender.getEntityWorld();
				BlockPos base = selection.minPos();
				int widthX = selection.widthX();
				int widthY = selection.widthY();
				int widthZ = selection.widthZ();
				long sizeX = (long) widthX * factor;
				long sizeY = (long) widthY * factor;
				long sizeZ = (long) widthZ * factor;
				long maxX = (long) base.getX() + sizeX - 1L;
				long maxY = (long) base.getY() + sizeY - 1L;
				long maxZ = (long) base.getZ() + sizeZ - 1L;
				long volume = sizeX;
				boolean tooLarge = sizeX > SnapshotManager.MAX_SNAPSHOT_VOLUME
						|| sizeY > SnapshotManager.MAX_SNAPSHOT_VOLUME || sizeZ > SnapshotManager.MAX_SNAPSHOT_VOLUME
						|| sizeY <= 0 || sizeZ <= 0
						|| volume > SnapshotManager.MAX_SNAPSHOT_VOLUME / sizeY;
				if (!tooLarge) {
					volume *= sizeY;
					tooLarge = volume > SnapshotManager.MAX_SNAPSHOT_VOLUME / sizeZ;
				}
				if (!tooLarge) {
					volume *= sizeZ;
					tooLarge = volume <= 0 || volume > SnapshotManager.MAX_SNAPSHOT_VOLUME;
				}
				if (tooLarge || maxX > Integer.MAX_VALUE || maxY > Integer.MAX_VALUE || maxZ > Integer.MAX_VALUE
						|| maxX < Integer.MIN_VALUE || maxY < Integer.MIN_VALUE || maxZ < Integer.MIN_VALUE) {
					Manager.sendMessage(new TextComponentTranslation("text.worldedit.scan.tooLarge", volume,
							SnapshotManager.MAX_SNAPSHOT_VOLUME));
					return;
				}
				final int finalFactor = factor;
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world,
						new ClientWorldView(world), base.getX(), base.getY(), base.getZ(), (int) maxX, (int) maxY,
						(int) maxZ, (snapshot, context) -> {
							Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
							int work = 0;
							for (int x = 0; x < widthX; x++) {
								for (int y = 0; y < widthY; y++) {
									for (int z = 0; z < widthZ; z++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										BlockPos source = base.add(x, y, z);
										IBlockState blockState = snapshot.get(source);
										if (blockState == null) {
											continue;
										}
										BlockPos scaledMin = new BlockPos(base.getX() + x * finalFactor,
												base.getY() + y * finalFactor, base.getZ() + z * finalFactor);
										boolean skip = true;
										for (int sx = 0; sx < finalFactor && skip; sx++) {
											for (int sy = 0; sy < finalFactor && skip; sy++) {
												for (int sz = 0; sz < finalFactor; sz++) {
													IBlockState scaledState = snapshot.get(scaledMin.getX() + sx,
															scaledMin.getY() + sy, scaledMin.getZ() + sz);
													if (scaledState == null || !scaledState.equals(blockState)) {
														skip = false;
														break;
													}
												}
											}
										}
										if (!skip) {
											for (int sx = 0; sx < finalFactor; sx++) {
												for (int sy = 0; sy < finalFactor; sy++) {
													for (int sz = 0; sz < finalFactor; sz++) {
														blocksToPlace.put(new BlockPos(scaledMin.getX() + sx,
																scaledMin.getY() + sy, scaledMin.getZ() + sz), blockState);
													}
												}
											}
										}
									}
								}
							}
							context.postClient(clientWorld -> TaskManager.getInstance()
									.addTask(new TaskSetBlock(blocksToPlace, clientWorld)));
						});
				ScanFeedback.report(result, selection);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.upscale.usage"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}
}
