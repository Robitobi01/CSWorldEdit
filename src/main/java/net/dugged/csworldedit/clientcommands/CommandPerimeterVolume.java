package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.scan.ClientJob;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class CommandPerimeterVolume extends ClientCommand {

	public CommandPerimeterVolume() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "perimetervolume";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.perimetervolume.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0 || args.length == 1 || args.length == 2) {
			World world = sender.getEntityWorld();
			BlockPos pos = Manager.playerPos();
			ClientWorldView view = new ClientWorldView(world);
			IBlockState initial = view.getBlock(pos.getX(), pos.getY(), pos.getZ());
			if (initial != null && initial.getBlock() instanceof BlockAir) {
				int radius = Math.max(0, (args.length >= 1) ? parseInt(args[0]) : 100);
				int minY = (args.length == 2) ? parseInt(args[1]) : 0;
				minY = Math.max(0, Math.min(minY, world.getHeight() - 1));
				int finalMinY = minY;
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitClientJob(
						new ClientJob(world) {
							private final ClientWorldView reader = new ClientWorldView(world);
							private final Set<BlockPos> checkedBlocks = new HashSet<>();
							private final Queue<BlockPos> blocksToCheck = new ArrayDeque<>();
							private int count;

							{
								checkedBlocks.add(pos);
								blocksToCheck.add(pos);
							}

							@Override
							public int step(int budget) {
								int processed = Math.min(budget, 64);
								for (int i = 0; i < processed && !blocksToCheck.isEmpty(); i++) {
									BlockPos current = blocksToCheck.poll();
									addNeighbor(current.north());
									addNeighbor(current.east());
									addNeighbor(current.south());
									addNeighbor(current.west());
									BlockPos scan = current;
									while (scan.getY() >= finalMinY) {
										IBlockState block = reader.getBlock(scan.getX(), scan.getY(), scan.getZ());
										if (block != null && block.isNormalCube() && !block.getBlock().equals(Blocks.BEDROCK)) {
											count++;
										}
										scan = scan.down();
									}
								}
								if (blocksToCheck.isEmpty()) {
									markFinished();
									final int total = count;
									net.dugged.csworldedit.tasks.TaskManager.getInstance().scheduleClient(() -> Manager
											.sendMessage(new TextComponentTranslation("text.worldedit.perimetervolume.response", total)));
								}
								return processed;
							}

							private void addNeighbor(BlockPos neighbor) {
								IBlockState state = reader.getBlock(neighbor.getX(), neighbor.getY(), neighbor.getZ());
								if (state != null && state.getBlock() instanceof BlockAir
										&& Manager.checkCircle(neighbor.getX() - pos.getX(), neighbor.getZ() - pos.getZ(), radius)) {
									if (checkedBlocks.add(neighbor)) {
										blocksToCheck.add(neighbor);
									}
								}
							}
						});
				ScanFeedback.report(result);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.floodfill.noSpaceToFlood"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.perimetervolume.usage"));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		return Collections.emptyList();
	}
}
