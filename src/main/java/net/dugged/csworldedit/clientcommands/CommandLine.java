package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

public class CommandLine extends ClientCommand {

	@Override
	public String getName() {
		return "line";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.line.usage").getUnformattedText();
	}

	private void placeLine(World world, Selection selection, IBlockState blockState) {
		Map<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		int x1 = selection.getPos(A).getX();
		int y1 = selection.getPos(A).getY();
		int z1 = selection.getPos(A).getZ();
		int x2 = selection.getPos(B).getX();
		int y2 = selection.getPos(B).getY();
		int z2 = selection.getPos(B).getZ();
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int dz = Math.abs(z2 - z1);
		int xs = (x2 > x1) ? 1 : -1;
		int ys = (y2 > y1) ? 1 : -1;
		int zs = (z2 > z1) ? 1 : -1;
		blocksToPlace.put(new BlockPos(x1, y1, z1), blockState);
		if (dx >= dy && dx >= dz) {
			int p1 = 2 * dy - dx;
			int p2 = 2 * dz - dx;
			while (x1 != x2) {
				if (Thread.interrupted()) {
					return;
				}
				x1 += xs;
				if (p1 >= 0) {
					y1 += ys;
					p1 -= 2 * dx;
				}
				if (p2 >= 0) {
					z1 += zs;
					p2 -= 2 * dx;
				}
				p1 += 2 * dy;
				p2 += 2 * dz;
				blocksToPlace.put(new BlockPos(x1, y1, z1), blockState);
			}
		} else if (dy >= dx && dy >= dz) {
			int p1 = 2 * dx - dy;
			int p2 = 2 * dz - dy;
			while (y1 != y2) {
				if (Thread.interrupted()) {
					return;
				}
				y1 += ys;
				if (p1 >= 0) {
					x1 += xs;
					p1 -= 2 * dy;
				}
				if (p2 >= 0) {
					z1 += zs;
					p2 -= 2 * dy;
				}
				p1 += 2 * dx;
				p2 += 2 * dz;
				blocksToPlace.put(new BlockPos(x1, y1, z1), blockState);
			}
		} else {
			int p1 = 2 * dy - dz;
			int p2 = 2 * dx - dz;
			while (z1 != z2) {
				if (Thread.interrupted()) {
					return;
				}
				z1 += zs;
				if (p1 >= 0) {
					y1 += ys;
					p1 -= 2 * dz;
				}
				if (p2 >= 0) {
					x1 += xs;
					p2 -= 2 * dz;
				}
				p1 += 2 * dy;
				p2 += 2 * dx;
				blocksToPlace.put(new BlockPos(x1, y1, z1), blockState);
			}
		}
		TaskManager.getInstance().scheduleClient(
				() -> TaskManager.getInstance().addTask(new TaskSetBlock(blocksToPlace, world)));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length <= 2) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				Block block = Blocks.GLOWSTONE;
				if (args.length > 0) {
					block = getBlockByText(sender, args[0]);
				}
				IBlockState blockState;
				if (args.length == 2) {
					blockState = convertArgToBlockState(block, args[1]);
				} else {
					blockState = block.getDefaultState();
				}
				World world = sender.getEntityWorld();
				Selection snapshot = selection.copy();
				Thread t = new Thread(() -> placeLine(world, snapshot, blockState));
				t.start();
				TaskManager.getInstance().threads.add(t);
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
			return getListOfStringsMatchingLastWord(args, Block.REGISTRY.getKeys());
		} else {
			return Collections.emptyList();
		}
	}
}