package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.BedColorMapping;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandWoolify extends ClientCommand {

	public static int getClosestDyeMetadata(IBlockState state) {
		MapColor blockMapColor;
		try {
			blockMapColor = state.getBlock().getMapColor(state, null, BlockPos.ORIGIN);
		} catch (RuntimeException exception) {
			blockMapColor = state.getMaterial().getMaterialMapColor();
		}
		return BedColorMapping.closestDyeMetadata(blockMapColor.colorValue);
	}

	@Override
	public String getName() {
		return "woolify";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.woolify.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection().copy();
				World world = sender.getEntityWorld();
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				ClientWorldView view = new ClientWorldView(world);
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world, view, view,
						min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), (snapshot, context) -> {
							Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
							int work = 0;
							for (int y = snapshot.minY(); y < snapshot.minY() + snapshot.sizeY(); y++) {
								for (int z = snapshot.minZ(); z < snapshot.minZ() + snapshot.sizeZ(); z++) {
									for (int x = snapshot.minX(); x < snapshot.minX() + snapshot.sizeX(); x++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										IBlockState blockState = snapshot.get(x, y, z);
										if (blockState == null) {
											continue;
										}
										if ((!blockState.isFullBlock() && !(blockState.getBlock() instanceof BlockLiquid))
												|| blockState.getBlock().equals(Blocks.WOOL)) {
											continue;
										}
										int metadata;
										if (blockState.getBlock() instanceof BlockBed) {
											boolean foot = blockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT;
											metadata = BedColorMapping.bedWoolMetadata(foot, snapshot.getBedColorValue(x, y, z));
										} else {
											metadata = getClosestDyeMetadata(blockState);
										}
										blocksToPlace.put(new BlockPos(x, y, z), Blocks.WOOL.getStateFromMeta(metadata));
									}
								}
							}
							context.postClient(clientWorld -> TaskManager.getInstance()
									.addTask(new TaskSetBlock(blocksToPlace, clientWorld)));
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
