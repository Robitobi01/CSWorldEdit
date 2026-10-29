package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.brushes.BrushBase;
import net.dugged.csworldedit.scan.ClientJob;
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
import java.util.*;

public class CommandRunBrush extends ClientCommand {

	@Override
	public String getName() {
		return "runbrush";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.runbrush.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			if (args.length > 3 && args.length < 7) {
				Block maskBlock = getBlockByText(sender, args[0]);
				IBlockState maskBlockState = convertArgToBlockState(maskBlock, args[1]);
				BrushBase brush;
				try {
					brush = Manager.getBrush(args[2]).getClass().newInstance();
				} catch (Exception e) {
					brush = null;
				}
				if (brush != null) {
					Block block = getBlockByText(sender, args[3]);
					IBlockState blockState = block.getDefaultState();
					if (args.length > 4) {
						blockState = convertArgToBlockState(block, args[4]);
					}
					brush.setBlockState(blockState);
					brush.setRadius(3);
					if (args.length > 5) {
						brush.setRadius(parseInt(args[5], 1));
					}
					BrushBase finalBrush = brush;
					World world = sender.getEntityWorld();
					Selection selection = Manager.getCurrentSelection().copy();
					SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitClientJob(new ClientJob(world) {
						private final ClientWorldView view = new ClientWorldView(world);
						private final int minX = selection.minPos().getX();
						private final int minY = selection.minPos().getY();
						private final int minZ = selection.minPos().getZ();
						private final int widthX = selection.widthX();
						private final int widthY = selection.widthY();
						private final int widthZ = selection.widthZ();
						private final long volume = selection.volume();
						private final List<BlockPos> positions = new ArrayList<>();
						private long cursor;

						@Override
						public int step(int budget) {
							int count = Math.min(Math.min(budget, 64), (int) Math.min(Integer.MAX_VALUE, volume - cursor));
							for (int i = 0; i < count; i++) {
								long index = cursor + i;
								int x = (int) (index % widthX);
								long rest = index / widthX;
								int z = (int) (rest % widthZ);
								int y = (int) (rest / widthZ);
								BlockPos pos = new BlockPos(minX + x, minY + y, minZ + z);
								IBlockState state = view.getBlock(pos.getX(), pos.getY(), pos.getZ());
								if (state != null && state.equals(maskBlockState)) {
									positions.addAll(finalBrush.run(world, pos));
								}
							}
							cursor += count;
							if (cursor >= volume) {
								markFinished();
								if (!positions.isEmpty()) {
									Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
									for (BlockPos pos : positions) {
										blocksToPlace.put(pos, finalBrush.getBlockState());
									}
									TaskManager.getInstance().scheduleClient(() -> TaskManager.getInstance()
											.addTask(new TaskSetBlock(blocksToPlace, world)));
								}
							}
							return count;
						}
					});
					ScanFeedback.report(result);
				} else {
					Manager.sendMessage(new TextComponentTranslation("text.worldedit.brush.notFound", args[2]));
				}
			} else {
				Manager.sendMessage(getUsage(sender));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		List<String> result = new ArrayList<>();
		if (args.length == 3) {
			for (BrushBase brush : Manager.brushes) {
				result.add(brush.getName());
			}
			return getListOfStringsMatchingLastWord(args, result);
		} else if (args.length == 1 || args.length == 4) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}
