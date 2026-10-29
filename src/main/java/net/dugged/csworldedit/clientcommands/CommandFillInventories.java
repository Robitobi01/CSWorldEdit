package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskReplaceItem;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockChest;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandFillInventories extends ClientCommand {

	@Override
	public String getName() {
		return "fillinventories";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.fillinventories.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			if (args.length == 2 || args.length == 3) {
				Selection selection = Manager.getCurrentSelection().copy();
				Item item = getItemByText(sender, args[0]);
				int count = Math.min(item.getItemStackLimit(), Math.max(parseInt(args[1]), 1));
				int damage = (args.length == 3) ? parseInt(args[2]) : 0;
				ItemStack stack = new ItemStack(item, count, damage);
				World world = sender.getEntityWorld();
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world,
						new ClientWorldView(world), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(),
						max.getZ(), (snapshot, context) -> {
							Map<BlockPos, ItemStack> containerMap = new HashMap<>();
							int work = 0;
							for (int y = snapshot.minY(); y < snapshot.minY() + snapshot.sizeY(); y++) {
								for (int z = snapshot.minZ(); z < snapshot.minZ() + snapshot.sizeZ(); z++) {
									for (int x = snapshot.minX(); x < snapshot.minX() + snapshot.sizeX(); x++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										net.minecraft.block.state.IBlockState state = snapshot.get(x, y, z);
										if (state == null) {
											continue;
										}
										net.minecraft.block.Block block = state.getBlock();
										if (block instanceof BlockDispenser || block instanceof BlockChest || block instanceof BlockHopper) {
											containerMap.put(new BlockPos(x, y, z), stack);
										}
									}
								}
							}
							context.postClient(clientWorld -> TaskManager.getInstance()
									.addTask(new TaskReplaceItem(containerMap, clientWorld)));
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
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Item.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}
