package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
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

public class CommandReplace extends ClientCommand {

	@Override
	public String getName() {
		return "replace";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.replace.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			Selection selection = Manager.getCurrentSelection().copy();
			if (args.length == 3 || args.length == 4) {
				final World world = sender.getEntityWorld();
				Block block1 = getBlockByText(sender, args[0]);
				boolean ignoreBlockState;
				boolean preserveMeta;
				IBlockState blockState1;
				if (args[1].equals("*")) {
					preserveMeta = false;
					ignoreBlockState = true;
					blockState1 = block1.getDefaultState();
				} else if (args[1].equals("#")) {
					ignoreBlockState = true;
					preserveMeta = true;
					blockState1 = block1.getDefaultState();
				} else {
					preserveMeta = false;
					ignoreBlockState = false;
					blockState1 = convertArgToBlockState(block1, args[1]);
				}
				Block block2 = getBlockByText(sender, args[2]);
				IBlockState blockState2;
				if (args.length == 4) {
					blockState2 = convertArgToBlockState(block2, args[3]);
				} else {
					blockState2 = block2.getDefaultState();
				}
				final IBlockState stateToReplace = blockState1;
				final IBlockState replacementState = blockState2;
				final boolean ignore = ignoreBlockState;
				final boolean keepMeta = preserveMeta;
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
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										IBlockState blockState = snapshot.get(x, y, z);
										if (blockState == null) {
											continue;
										}
										if (blockState == stateToReplace || (ignore && blockState.getBlock() == stateToReplace.getBlock())) {
											BlockPos pos = new BlockPos(x, y, z);
											if (keepMeta) {
												int meta = blockState.getBlock().getMetaFromState(blockState);
												blocksToPlace.put(pos, replacementState.getBlock().getStateFromMeta(meta));
											} else {
												blocksToPlace.put(pos, replacementState);
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
				Manager.sendMessage(getUsage(sender));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1 || args.length == 3) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, "*", "#");
		} else if (args.length == 4) {
			return getListOfStringsMatchingLastWord(args, "*");
		} else {
			return Collections.emptyList();
		}
	}
}
