package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.tasks.TaskManager;
import net.dugged.worldedit.tasks.TaskSetBlock;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.HashMap;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

public class CommandWoolify extends ClientCommand {

	public static int getClosestDyeMetadata(IBlockState state, IBlockAccess world, BlockPos pos) {
		MapColor blockMapColor = state.getBlock().getMapColor(state, world, pos);
		int blockColor = blockMapColor.colorValue;
		float r = ((blockColor >> 16) & 0xFF) / 255f;
		float g = ((blockColor >> 8) & 0xFF) / 255f;
		float b = (blockColor & 0xFF) / 255f;
		EnumDyeColor closestDye = null;
		float smallestDistance = Float.MAX_VALUE;
		for (EnumDyeColor dye : EnumDyeColor.values()) {
			int dyeColorInt = dye.getColorValue();
			float dyeR = ((dyeColorInt >> 16) & 0xFF) / 255f;
			float dyeG = ((dyeColorInt >> 8) & 0xFF) / 255f;
			float dyeB = (dyeColorInt & 0xFF) / 255f;
			float distance = (r - dyeR) * (r - dyeR) + (g - dyeG) * (g - dyeG) + (b - dyeB) * (b - dyeB);
			if (distance < smallestDistance) {
				smallestDistance = distance;
				closestDye = dye;
			}
		}
		return closestDye.getMetadata();
	}

	@Override
	public String getName() {
		return "woolify";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.size.usage").getUnformattedText();
	}

	public void woolify(World world, WorldEditSelection selection) {
		HashMap<BlockPos, IBlockState> blocksToPlace = new HashMap<>();
		for (BlockPos pos : BlockPos.MutableBlockPos.getAllInBox(selection.getPos(A), selection.getPos(B))) {
			if (Thread.interrupted()) {
				return;
			}
			IBlockState blockState = world.getBlockState(pos);
			if ((!blockState.isFullBlock() && !(blockState.getBlock() instanceof BlockLiquid)) || blockState.getBlock().equals(Blocks.WOOL)) {
				continue;
			}
			int metadata = getClosestDyeMetadata(blockState, world, pos);
			blocksToPlace.put(pos, Blocks.WOOL.getStateFromMeta(metadata));
		}
		TaskSetBlock task = new TaskSetBlock(blocksToPlace, world);
		TaskManager.getInstance().addTask(task);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				World world = sender.getEntityWorld();
				Thread t = new Thread(() -> woolify(world, selection));
				t.start();
				TaskManager.getInstance().threads.add(t);
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}
}