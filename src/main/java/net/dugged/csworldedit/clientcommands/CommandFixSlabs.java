package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

public class CommandFixSlabs extends ClientCommand {

	@Override
	public String getName() {
		return "fixslabs";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.fixslabs.usage").getUnformattedText();
	}

	private void fixSlabs(World world, Selection selection) {
		int count = 0;
		Map<BlockPos, IBlockState> blockList = new HashMap<>();
		for (BlockPos pos : BlockPos.MutableBlockPos.getAllInBox(selection.getPos(A), selection.getPos(B))) {
			if (Thread.interrupted()) {
				return;
			}
			IBlockState blockState = world.getBlockState(pos);
			if (blockState.getBlock() instanceof BlockDoubleStoneSlab && blockState.getProperties().containsKey(BlockDoubleStoneSlab.VARIANT)) {
				switch (blockState.getValue(BlockDoubleStoneSlab.VARIANT)) {
					case SAND:
						blockList.put(pos, Blocks.SANDSTONE.getDefaultState());
						count++;
						break;
					case WOOD:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.OAK));
						count++;
						break;
					case COBBLESTONE:
						blockList.put(pos, Blocks.COBBLESTONE.getDefaultState());
						count++;
						break;
					case BRICK:
						blockList.put(pos, Blocks.BRICK_BLOCK.getDefaultState());
						count++;
						break;
					case SMOOTHBRICK:
						blockList.put(pos, Blocks.STONEBRICK.getDefaultState());
						count++;
						break;
					case NETHERBRICK:
						blockList.put(pos, Blocks.NETHER_BRICK.getDefaultState());
						count++;
						break;
					case QUARTZ:
						blockList.put(pos, Blocks.QUARTZ_BLOCK.getDefaultState());
						count++;
						break;
					default:
						break;
				}
			} else if (blockState.getBlock() instanceof BlockDoubleStoneSlabNew && blockState.getProperties().containsKey(BlockDoubleStoneSlabNew.VARIANT)) {
				if (blockState.getValue(BlockDoubleStoneSlabNew.VARIANT) == BlockStoneSlabNew.EnumType.RED_SANDSTONE) {
					blockList.put(pos, Blocks.RED_SANDSTONE.getDefaultState());
					count++;
				}
			} else if (blockState.getBlock() instanceof BlockDoubleWoodSlab && blockState.getProperties().containsKey(BlockDoubleWoodSlab.VARIANT)) {
				switch (blockState.getValue(BlockDoubleWoodSlab.VARIANT)) {
					case OAK:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.OAK));
						count++;
						break;
					case SPRUCE:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.SPRUCE));
						count++;
						break;
					case BIRCH:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.BIRCH));
						count++;
						break;
					case JUNGLE:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.JUNGLE));
						count++;
						break;
					case ACACIA:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.ACACIA));
						count++;
						break;
					case DARK_OAK:
						blockList.put(pos, Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
								BlockPlanks.EnumType.DARK_OAK));
						count++;
						break;
					default:
						break;
				}
			} else if (blockState.getBlock() instanceof BlockPurpurSlab && ((BlockPurpurSlab) blockState.getBlock()).isDouble()) {
				blockList.put(pos, Blocks.PURPUR_BLOCK.getDefaultState());
				count++;
			} else if (blockState.getBlock() instanceof BlockStoneSlab && blockState.getProperties().containsKey(BlockStoneSlab.VARIANT) && blockState.getValue(BlockStoneSlab.VARIANT) == BlockStoneSlab.EnumType.WOOD) {
				blockList.put(pos, Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT,
						BlockPlanks.EnumType.OAK).withProperty(BlockSlab.HALF,
						blockState.getValue(BlockStoneSlab.HALF)));
				count++;
			}
		}
		TaskSetBlock task = new TaskSetBlock(blockList, world);
		TaskManager.getInstance().addTask(task);
		Manager.sendMessage(new TextComponentTranslation("text.worldedit.fixslabs.response", count));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				World world = sender.getEntityWorld();
				Thread t = new Thread(() -> fixSlabs(world, selection));
				t.start();
				TaskManager.getInstance().threads.add(t);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.fixslabs.usage"));
		}
	}
}