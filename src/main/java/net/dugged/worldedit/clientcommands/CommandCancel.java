package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.tasks.TaskChunk;
import net.dugged.worldedit.tasks.TaskManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Iterator;

public class CommandCancel extends ClientCommand {

	public CommandCancel() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "cancel";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation(
				"text.worldedit.cancel.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			Iterator<TaskChunk> it = TaskManager.getInstance().getTasks().iterator();
			while (it.hasNext()) {
				it.remove();
			}
			for (Thread thread : TaskManager.getInstance().threads) {
				thread.interrupt();
			}
			WorldEditManager.sendMessage(new TextComponentTranslation(
					"text.worldedit.cancel.cancelledAll"));
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}
}
