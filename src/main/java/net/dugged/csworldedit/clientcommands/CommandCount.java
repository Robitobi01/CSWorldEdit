package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.scan.SnapshotManager.SubmitResult;
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
import java.util.List;

public class CommandCount extends ClientCommand {

	public CommandCount() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "count";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.count.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 2 || args.length == 3) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection().copy();
				boolean exclusive = parseBoolean(args[0]);
				Block block = getBlockByText(sender, args[1]);
				IBlockState blockState;
				boolean compareStates;
				if (args.length == 3) {
					blockState = convertArgToBlockState(block, args[2]);
					compareStates = true;
				} else {
					blockState = block.getDefaultState();
					compareStates = false;
				}
				World world = sender.getEntityWorld();
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				SubmitResult result = SnapshotManager.getInstance().submitRegion(world, new ClientWorldView(world),
						min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), (snapshot, context) -> {
							int count = 0;
							int work = 0;
							for (int y = snapshot.minY(); y < snapshot.minY() + snapshot.sizeY(); y++) {
								for (int z = snapshot.minZ(); z < snapshot.minZ() + snapshot.sizeZ(); z++) {
									for (int x = snapshot.minX(); x < snapshot.minX() + snapshot.sizeX(); x++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										IBlockState current = snapshot.get(x, y, z);
										if (current == null) {
											continue;
										}
										boolean matches = compareStates ? current == blockState : current.getBlock() == blockState.getBlock();
										if (exclusive ? !matches : matches) {
											count++;
										}
									}
								}
							}
							final int total = count;
							context.postClient(clientWorld -> {
								if (exclusive) {
									Manager.sendMessage(new TextComponentTranslation("text.worldedit.count.responseInclusive", total));
								} else {
									Manager.sendMessage(new TextComponentTranslation("text.worldedit.count.responseExclusive", total));
								}
							});
						});
				ScanFeedback.report(result, selection);
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
			return getListOfStringsMatchingLastWord(args, "true", "false");
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}
