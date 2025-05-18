package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.tasks.TaskClone;
import net.dugged.worldedit.tasks.TaskManager;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

public class CommandMove extends ClientCommand {

	@Override
	public String getName() {
		return "move";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.move.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (WorldEditManager.hasCurrentSelection()) {
			WorldEditSelection selection = WorldEditManager.getCurrentSelection();
			if (args.length == 0 || args.length == 1) {
				World world = sender.getEntityWorld();
				int distance = 0;
				if (args.length == 1) {
					distance = parseInt(args[0]);
				}
				BlockPos dstOrigin;
				if (distance > 0) {
					dstOrigin = WorldEditManager.offsetFacingDirection(selection.minPos(), distance);
				} else {
					dstOrigin = WorldEditManager.playerPos();
				}
				TaskClone.Mode mode = (distance > 0) ? TaskClone.Mode.MOVE : TaskClone.Mode.FORCE;
				TaskClone task = new TaskClone(selection.minPos(), selection.maxPos(), dstOrigin, world, mode);
				Thread t = new Thread(() -> TaskManager.getInstance().addTask(task));
				t.start();
				TaskManager.getInstance().threads.add(t);
				selection.setPos(A, dstOrigin.add(selection.widthX() - 1, selection.widthY() - 1,
						selection.widthZ() - 1));
				selection.setPos(B, dstOrigin);
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.move.usage"));
			}
		} else {
			WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}
}