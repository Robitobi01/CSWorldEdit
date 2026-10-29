package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.scan.ClientWorldView;
import net.dugged.csworldedit.scan.ScanFeedback;
import net.dugged.csworldedit.scan.SnapshotManager;
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

	private static int directionState(float yaw, float pitch) {
		final int tolerance = 15;
		if (yaw >= 45 - tolerance && yaw <= 45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
			return 1;
		}
		if (yaw >= 135 - tolerance && yaw <= 135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
			return 1;
		}
		if (yaw >= -45 - tolerance && yaw <= -45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
			return 1;
		}
		if (yaw >= -135 - tolerance && yaw <= -135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
			return 1;
		}
		if (yaw >= -tolerance && yaw <= tolerance) {
			return (pitch >= 45 - tolerance && pitch <= 45 + tolerance)
					|| (pitch >= -45 - tolerance && pitch <= -45 + tolerance) ? 1 : 0;
		}
		if (yaw >= 90 - tolerance && yaw <= 90 + tolerance) {
			return (pitch >= 45 - tolerance && pitch <= 45 + tolerance)
					|| (pitch >= -45 - tolerance && pitch <= -45 + tolerance) ? 1 : 0;
		}
		if (yaw >= -90 - tolerance && yaw <= -90 + tolerance) {
			return (pitch >= 45 - tolerance && pitch <= 45 + tolerance)
					|| (pitch >= -45 - tolerance && pitch <= -45 + tolerance) ? 1 : 0;
		}
		if (yaw >= 180 - tolerance || yaw <= -180 + tolerance) {
			return (pitch >= 45 - tolerance && pitch <= 45 + tolerance)
					|| (pitch >= -45 - tolerance && pitch <= -45 + tolerance) ? 1 : 0;
		}
		return -1;
	}

	@Override
	public void execute(MinecraftServer server, net.minecraft.command.ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0 || args.length == 1) {
			int spacing = (args.length == 1) ? parseInt(args[0]) : 0;
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection().copy();
				World world = sender.getEntityWorld();
				float yaw = MathHelper.wrapDegrees(mc.getRenderViewEntity().rotationYaw);
				float pitch = MathHelper.wrapDegrees(mc.getRenderViewEntity().rotationPitch);
				int state = directionState(yaw, pitch);
				if (state < 0) {
					Manager.sendMessage(new TextComponentTranslation("text.worldedit.stackquarter.invalidDirection"));
					return;
				}
				if (state == 0) {
					return;
				}
				BlockPos min = selection.minPos();
				BlockPos max = selection.maxPos();
				final int widthX = selection.widthX();
				final int widthY = selection.widthY();
				final int widthZ = selection.widthZ();
				SnapshotManager.SubmitResult result = SnapshotManager.getInstance().submitRegion(world,
						new ClientWorldView(world), min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(),
						max.getZ(), (snapshot, context) -> {
							final int tolerance = 15;
							Map<BlockPos, IBlockState> blockList = new HashMap<>();
							int work = 0;
							for (int x = 0; x < widthX; x++) {
								for (int y = 0; y < widthY; y++) {
									for (int z = 0; z < widthZ; z++) {
										if ((work++ & 1023) == 0 && context.isCancelled()) {
											return;
										}
										IBlockState blockState = snapshot.get(min.add(x, y, z));
										if (blockState == null) {
											continue;
										}
										if (yaw >= 45 - tolerance && yaw <= 45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
											blockList.put(min.add(-x - 1 - spacing, y, z),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
											blockList.put(min.add(x, y, widthZ - z - 1 + widthZ + spacing),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
											blockList.put(min.add(-x - 1 - spacing, y, widthZ - z - 1 + widthZ + spacing),
													Manager.flipBlockstate(Manager.flipBlockstate(blockState,
															net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
										} else if (yaw >= 135 - tolerance && yaw <= 135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
											blockList.put(min.add(-x - 1 - spacing, y, z),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
											blockList.put(min.add(x, y, -z - 1 - spacing),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
											blockList.put(min.add(-x - 1 - spacing, y, -z - 1 - spacing),
													Manager.flipBlockstate(Manager.flipBlockstate(blockState,
															net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
										} else if (yaw >= -45 - tolerance && yaw <= -45 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
											blockList.put(min.add(widthX - x - 1 + widthX + spacing, y, z),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
											blockList.put(min.add(x, y, widthZ - z - 1 + widthZ + spacing),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
											blockList.put(min.add(widthX - x - 1 + widthX + spacing, y,
													widthZ - z - 1 + widthZ + spacing),
													Manager.flipBlockstate(Manager.flipBlockstate(blockState,
															net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
										} else if (yaw >= -135 - tolerance && yaw <= -135 + tolerance && pitch >= -tolerance && pitch <= tolerance) {
											blockList.put(min.add(widthX - x - 1 + widthX + spacing, y, z),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
											blockList.put(min.add(x, y, -z - 1 - spacing),
													Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
											blockList.put(min.add(widthX - x - 1 + widthX + spacing, y, -z - 1 - spacing),
													Manager.flipBlockstate(Manager.flipBlockstate(blockState,
															net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Z));
										} else if (yaw >= -tolerance && yaw <= tolerance) {
											if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
												blockList.put(min.add(x, y, widthZ - z - 1 + widthZ + spacing),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
												blockList.put(min.add(x, -y - 1 - spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(x, -y - 1 - spacing,
														widthZ - z - 1 + widthZ + spacing),
														Manager.flipBlockstate(Manager.flipBlockstate(blockState,
																net.minecraft.util.EnumFacing.Axis.Z),
																net.minecraft.util.EnumFacing.Axis.Y));
											} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
												blockList.put(min.add(x, y, widthZ - z - 1 + widthZ + spacing),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing,
														widthZ - z - 1 + widthZ + spacing),
														Manager.flipBlockstate(Manager.flipBlockstate(blockState,
																net.minecraft.util.EnumFacing.Axis.Z),
																net.minecraft.util.EnumFacing.Axis.Y));
											}
										} else if (yaw >= 90 - tolerance && yaw <= 90 + tolerance) {
											if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
												blockList.put(min.add(-x - 1 - spacing, y, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
												blockList.put(min.add(x, -y - 1 - spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
												blockList.put(min.add(-x - 1 - spacing, -y - 1 - spacing, z),
														Manager.flipBlockstate(Manager.flipBlockstate(blockState,
																net.minecraft.util.EnumFacing.Axis.X),
																net.minecraft.util.EnumFacing.Axis.Y));
											} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
												blockList.put(min.add(-x - 1 - spacing, y, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(-x - 1 - spacing, widthY - y - 1 + widthY + spacing,
														z), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
														net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Y));
											}
										} else if (yaw >= -90 - tolerance && yaw <= -90 + tolerance) {
											if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
												blockList.put(min.add(widthX - x - 1 + widthX + spacing, y, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
												blockList.put(min.add(x, -y - 1 - spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(widthX - x - 1 + widthX + spacing, -y - 1 - spacing,
														z), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
														net.minecraft.util.EnumFacing.Axis.X), net.minecraft.util.EnumFacing.Axis.Y));
											} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
												blockList.put(min.add(widthX - x - 1 + widthX + spacing, y, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.X));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(widthX - x - 1 + widthX + spacing,
														widthY - y - 1 + widthY + spacing, z),
														Manager.flipBlockstate(Manager.flipBlockstate(blockState,
																net.minecraft.util.EnumFacing.Axis.X),
																net.minecraft.util.EnumFacing.Axis.Y));
											}
										} else if (yaw >= 180 - tolerance || yaw <= -180 + tolerance) {
											if (pitch >= 45 - tolerance && pitch <= 45 + tolerance) {
												blockList.put(min.add(x, y, -z - 1 - spacing),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
												blockList.put(min.add(x, -y - 1 - spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(x, -y - 1 - spacing, -z - 1 - spacing),
														Manager.flipBlockstate(Manager.flipBlockstate(blockState,
																net.minecraft.util.EnumFacing.Axis.Z),
																net.minecraft.util.EnumFacing.Axis.Y));
											} else if (pitch >= -45 - tolerance && pitch <= -45 + tolerance) {
												blockList.put(min.add(x, y, -z - 1 - spacing),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Z));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing, z),
														Manager.flipBlockstate(blockState, net.minecraft.util.EnumFacing.Axis.Y));
												blockList.put(min.add(x, widthY - y - 1 + widthY + spacing,
														-z - 1 - spacing), Manager.flipBlockstate(Manager.flipBlockstate(blockState,
														net.minecraft.util.EnumFacing.Axis.Z), net.minecraft.util.EnumFacing.Axis.Y));
											}
										}
									}
								}
							}
							context.postClient(clientWorld -> TaskManager.getInstance()
									.addTask(new TaskSetBlock(blockList, clientWorld)));
						});
				ScanFeedback.report(result, selection);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.stackquarter.usage"));
		}
	}
}
