package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.scan.ClientJob;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.*;

public class CommandDrain extends ClientCommand {

	@Override
	public String getName() {
		return "drain";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.drain.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0 || args.length == 1) {
			World world = sender.getEntityWorld();
			BlockPos pos = Manager.playerPos();
			ClientWorldView view = new ClientWorldView(world);
			IBlockState initial = view.getBlock(pos.getX(), pos.getY(), pos.getZ());
			if (initial != null && initial.getBlock() instanceof BlockLiquid) {
				int radius = args.length == 1 ? parseInt(args[0]) : 100;
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
									addNeighbor(current.up());
									addNeighbor(current.down());
									addNeighbor(current.north());
									addNeighbor(current.east());
									addNeighbor(current.south());
									addNeighbor(current.west());
									blocksToPlace.put(current, Blocks.AIR.getDefaultState());
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
								if (state == null || !(state.getBlock() instanceof BlockLiquid)) {
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
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.drain.notInWater"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.drain.usage"));
		}
	}
}
