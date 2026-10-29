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

public class CommandSwap extends ClientCommand {

	@Override
	public String getName() {
		return "swap";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.swap.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			Selection selection = Manager.getCurrentSelection().copy();
			int len = args.length;
			if (len < 2 || len > 4) {
				Manager.sendMessage(getUsage(sender));
				return;
			}
			final World world = sender.getEntityWorld();
			Block block1 = getBlockByText(sender, args[0]);
			IBlockState state1;
			boolean ignoreBlockState1;
			boolean preserveMeta;
			if (len >= 3) {
				if (args[1].equals("*")) {
					state1 = block1.getDefaultState();
					ignoreBlockState1 = true;
					preserveMeta = false;
				} else if (args[1].equals("#")) {
					state1 = block1.getDefaultState();
					ignoreBlockState1 = true;
					preserveMeta = true;
				} else {
					state1 = convertArgToBlockState(block1, args[1]);
					ignoreBlockState1 = false;
					preserveMeta = false;
				}
			} else {
				preserveMeta = false;
				ignoreBlockState1 = false;
				state1 = block1.getDefaultState();
			}
			Block block2;
			IBlockState state2;
			boolean ignoreBlockState2;
			if (len == 2) {
				ignoreBlockState2 = false;
				block2 = getBlockByText(sender, args[1]);
				state2 = block2.getDefaultState();
			} else if (len == 3) {
				ignoreBlockState2 = false;
				block2 = getBlockByText(sender, args[2]);
				state2 = block2.getDefaultState();
			} else {
				block2 = getBlockByText(sender, args[2]);
				if (args[3].equals("*")) {
					state2 = block2.getDefaultState();
					ignoreBlockState2 = true;
				} else {
					state2 = convertArgToBlockState(block2, args[3]);
					ignoreBlockState2 = false;
				}
			}
			final IBlockState firstState = state1;
			final IBlockState secondState = state2;
			final boolean ignoreFirst = ignoreBlockState1;
			final boolean ignoreSecond = ignoreBlockState2;
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
									IBlockState currentState = snapshot.get(x, y, z);
									if (currentState == null) {
										continue;
									}
									boolean isState1 = ignoreFirst ? currentState.getBlock() == firstState.getBlock() :
											currentState.equals(firstState);
									boolean isState2 = ignoreSecond ? currentState.getBlock() == secondState.getBlock() :
											currentState.equals(secondState);
									BlockPos pos = new BlockPos(x, y, z);
									if (isState1) {
										if (keepMeta) {
											int meta = currentState.getBlock().getMetaFromState(currentState);
											blocksToPlace.put(pos, secondState.getBlock().getStateFromMeta(meta));
										} else {
											blocksToPlace.put(pos, secondState);
										}
									} else if (isState2) {
										if (keepMeta) {
											int meta = currentState.getBlock().getMetaFromState(currentState);
											blocksToPlace.put(pos, firstState.getBlock().getStateFromMeta(meta));
										} else {
											blocksToPlace.put(pos, firstState);
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
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1 || args.length == 3) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else if (args.length == 2) {
			List<Object> options = new java.util.ArrayList<>();
			options.addAll(Block.REGISTRY.getKeys());
			options.add("*");
			options.add("#");
			return getListOfStringsMatchingLastWord(args, options);
		} else if (args.length == 4) {
			return getListOfStringsMatchingLastWord(args, "*");
		} else {
			return Collections.emptyList();
		}
	}
}
