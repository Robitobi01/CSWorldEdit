package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandFlip extends ClientCommand {

	@Override
	public String getName() {
		return "flip";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.flip.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection().copy();
				World world = sender.getEntityWorld();
				EnumFacing direction = Manager.getFacingDirection();
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				final int widthX = selection.widthX();
				final int widthY = selection.widthY();
				final int widthZ = selection.widthZ();
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world,
						new ClientWorldView(world), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(),
						max.getZ(), (snapshot, context) -> {
							Map<BlockPos, IBlockState> blockList = new HashMap<>();
							int work = 0;
							for (int x = 0; x < widthX; x++) {
								for (int y = 0; y < widthY; y++) {
									for (int z = 0; z < widthZ; z++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										BlockPos source = min.add(x, y, z);
										IBlockState blockState = snapshot.get(source);
										if (blockState == null) {
											continue;
										}
										BlockPos target;
										if (direction.getAxis() == EnumFacing.Axis.Y) {
											target = min.add(x, widthY - y - 1, z);
										} else if (direction.getAxis() == EnumFacing.Axis.Z) {
											target = min.add(x, y, widthZ - z - 1);
										} else {
											target = min.add(widthX - x - 1, y, z);
										}
										blockList.put(target, Manager.flipBlockstate(blockState, direction.getAxis()));
									}
								}
							}
							context.postClient(clientWorld -> TaskManager.getInstance()
									.addTask(new TaskSetBlock(blockList, clientWorld)));
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
