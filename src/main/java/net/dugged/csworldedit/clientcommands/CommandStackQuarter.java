package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CommandStackQuarter extends ClientCommand {

	@Override
	public String getName() {
		return "stackquarter";
	}

	@Override
	public String getUsage(net.minecraft.command.ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.stackquarter.usage").getUnformattedText();
	}

	private void stackQuarter(World world, Selection selection, final int spacing) {
		final int tolerance = 15;
		final float yaw = MathHelper.wrapDegrees(mc.getRenderViewEntity().rotationYaw);
		final float pitch = MathHelper.wrapDegrees(mc.getRenderViewEntity().rotationPitch);
		final int widthX = selection.widthX();
		final int widthY = selection.widthY();
		final int widthZ = selection.widthZ();
		Map<BlockPos, IBlockState> blockList = new HashMap<>();
		for (int x = 0; x < selection.widthX(); x++) {
			for (int y = 0; y < selection.widthY(); y++) {
				for (int z = 0; z < selection.widthZ(); z++) {
					if (Thread.interrupted()) {
						return;
					}
					IBlockState blockState = world.getBlockState(selection.minPos().add(x, y, z));
					if (yaw >= 45 - tolerance && yaw <= 45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
						blockList.put(selection.minPos().add(-x - 1 - spacing, y, z),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
						blockList.put(selection.minPos().add(x, y, widthZ - z - 1 + widthZ + spacing),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
						blockList.put(selection.minPos().add(-x - 1 - spacing, y, widthZ - z - 1 + widthZ + spacing),
								Manager.flipBlockstate(Manager.flipBlockstate(blockState,
										net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
					} else if (yaw >= 135 - tolerance && yaw <= 135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
						blockList.put(selection.minPos().add(-x - 1 - spacing, y, z),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
						blockList.put(selection.minPos().add(x, y, -z - 1 - spacing),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
						blockList.put(selection.minPos().add(-x - 1 - spacing, y, -z - 1 - spacing),
								Manager.flipBlockstate(Manager.flipBlockstate(blockState,
										net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
					} else if (yaw >= -45 - tolerance && yaw <= -45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
						blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y, z),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
						blockList.put(selection.minPos().add(x, y, widthZ - z - 1 + widthZ + spacing),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
						blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y,
								widthZ - z - 1 + widthZ + spacing),
								Manager.flipBlockstate(Manager.flipBlockstate(blockState,
										net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
					} else if (yaw >= -135 - tolerance && yaw <= -135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
						blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y, z),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
						blockList.put(selection.minPos().add(x, y, -z - 1 - spacing),
								Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
						blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y, -z - 1 - spacing),
								Manager.flipBlockstate(Manager.flipBlockstate(blockState,
										net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
					} else if (yaw >= -tolerance && yaw <= tolerance) {
						if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
							blockList.put(selection.minPos().add(x, y, widthZ - z - 1 + widthZ + spacing),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing,
									widthZ - z - 1 + widthZ + spacing),
									Manager.flipBlockstate(Manager.flipBlockstate(blockState,
											net.minecraft.util.EnumFacing.Axis.Z),
											net.minecraft.util.EnumFacing.Axis.Y));
						} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
							blockList.put(selection.minPos().add(x, y, widthZ - z - 1 + widthZ + spacing),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing,
									widthZ - z - 1 + widthZ + spacing),
									Manager.flipBlockstate(Manager.flipBlockstate(blockState,
											net.minecraft.util.EnumFacing.Axis.Z),
											net.minecraft.util.EnumFacing.Axis.Y));
						}
					} else if (yaw >= 90 - tolerance && yaw <= 90 + tolerance) {
						if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
							blockList.put(selection.minPos().add(-x - 1 - spacing, y, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
							blockList.put(selection.minPos().add(-x - 1 - spacing, -y - 1 - spacing, z),
									Manager.flipBlockstate(Manager.flipBlockstate(blockState,
											net.minecraft.util.EnumFacing.Axis.X),
											net.minecraft.util.EnumFacing.Axis.Y));
						} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
							blockList.put(selection.minPos().add(-x - 1 - spacing, y, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(-x - 1 - spacing, widthY - y - 1 + widthY + spacing,
									z), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
									net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Y));
						}
					} else if (yaw >= -90 - tolerance && yaw <= -90 + tolerance) {
						if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
							blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, -y - 1 - spacing,
									z), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
									net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Y));
						} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
							blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing, y, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(widthX - x - 1 + widthX + spacing,
									widthY - y - 1 + widthY + spacing, z),
									Manager.flipBlockstate(Manager.flipBlockstate(blockState,
											net.minecraft.util.EnumFacing.Axis.X),
											net.minecraft.util.EnumFacing.Axis.Y));
						}
					} else if (yaw >= 180 - tolerance || yaw <= -180 + tolerance) {
						if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
							blockList.put(selection.minPos().add(x, y, -z - 1 - spacing),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(x, -y - 1 - spacing, -z - 1 - spacing),
									Manager.flipBlockstate(Manager.flipBlockstate(blockState,
											net.minecraft.util.EnumFacing.Axis.Z),
											net.minecraft.util.EnumFacing.Axis.Y));
						} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
							blockList.put(selection.minPos().add(x, y, -z - 1 - spacing),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing, z),
									Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
							blockList.put(selection.minPos().add(x, widthY - y - 1 + widthY + spacing,
									-z - 1 - spacing), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
									net.minecraft.util.EnumFacing.Axis.Z), net.minecraft.util.EnumFacing.Axis.Y));
						}
					} else {
						Manager.sendMessage(new TextComponentTranslation("text.worldedit.stackquarter" +
								".invalidDirection"));
						return;
					}
				}
			}
		}
		TaskSetBlock task = new TaskSetBlock(blockList, world);
		TaskManager.getInstance().addTask(task);
	}

	@Override
	public void execute(MinecraftServer server, net.minecraft.command.ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0 || args.length == 1) {
			int spacing = (args.length == 1) ? parseInt(args[0]) : 0;
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				World world = sender.getEntityWorld();
				Thread t = new Thread(() -> stackQuarter(world, selection, spacing));
				t.start();
				TaskManager.getInstance().threads.add(t);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.outlinefill.usage"));
		}
	}
}