package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.scan.ClientJob;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class CommandFloodFill extends ClientCommand {

	@Override
	public String getName() {
		return "floodfill";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.floodfill.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length >= 1 && args.length <= 3) {
			World world = sender.getEntityWorld();
			BlockPos pos = Manager.playerPos();
			ClientWorldView view = new ClientWorldView(world);
			IBlockState initial = view.getBlock(pos.getX(), pos.getY(), pos.getZ());
			if (initial != null && initial.getBlock() instanceof BlockAir) {
				Block block = getBlockByText(sender, args[0]);
				IBlockState blockState = args.length >= 2 ? convertArgToBlockState(block, args[1]) :
						block.getDefaultState();
				int radius = args.length == 3 ? parseInt(args[2]) : 100;
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitClientJob(
						new ClientJob(world) {
							private final ClientWorldView reader = new ClientWorldView(world);
							private final Set<BlockPos> checkedBlocks = new HashSet<>();
							private final Queue<BlockPos> blocksToCheck = new ArrayDeque<>();
							private final Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();

							{
								checkedBlocks.add(pos);
								blocksToCheck.add(pos);
							}

							@Override
							public int step(int budget) {
								int count = Math.min(budget, 256);
								for (int i = 0; i < count && !blocksToCheck.isEmpty(); i++) {
									BlockPos current = blocksToCheck.poll();
									BlockPos up = current.up();
									if (up.getY() <= pos.getY()) {
										addNeighbor(up);
									}
									addNeighbor(current.down());
									addNeighbor(current.north());
									addNeighbor(current.east());
									addNeighbor(current.south());
									addNeighbor(current.west());
									blocksToPlace.put(current, blockState);
								}
								if (blocksToCheck.isEmpty()) {
									markFinished();
									if (!blocksToPlace.isEmpty()) {
										TaskManager.getInstance().scheduleClient(() -> TaskManager.getInstance()
												.addTask(new TaskSetBlock(blocksToPlace, world)));
									}
								}
								return count;
							}

							private void addNeighbor(BlockPos neighbor) {
								IBlockState state = reader.getBlock(neighbor.getX(), neighbor.getY(), neighbor.getZ());
								if (state == null || !(state.getBlock() instanceof BlockAir)) {
									return;
								}
								if (!Manager.checkCircle(neighbor.getX() - pos.getX(), neighbor.getZ() - pos.getZ(), radius)) {
									return;
								}
								if (checkedBlocks.add(neighbor)) {
									blocksToCheck.add(neighbor);
								}
							}
						});
				ScanFeedback.report(result);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.floodfill.noSpaceToFlood"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.floodfill.usage"));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		}
		return Collections.emptyList();
	}
}
