package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.dugged.csworldedit.tasks.TaskClone;
import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

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
		if (Manager.hasCurrentSelection()) {
			Selection selection = Manager.getCurrentSelection();
			if (args.length == 0 || args.length == 1) {
				World world = sender.getEntityWorld();
				int distance = 0;
				if (args.length == 1) {
					distance = parseInt(args[0]);
				}
				BlockPos dstOrigin;
				if (distance > 0) {
					dstOrigin = Manager.offsetFacingDirection(selection.minPos(), distance);
				} else {
					dstOrigin = Manager.playerPos();
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
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.move.usage"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}
}