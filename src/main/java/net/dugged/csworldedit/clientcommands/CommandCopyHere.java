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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandCopyHere extends ClientCommand {

	@Override
	public String getName() {
		return "copyhere";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.copyhere.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (Manager.hasCurrentSelection()) {
			if (args.length == 0 || args.length == 1) {
				Selection selection = Manager.getCurrentSelection();
				boolean moveSelection = false;
				if (args.length == 1) {
					moveSelection = parseBoolean(args[0]);
				}
				TaskClone.Mode mode = moveSelection ? TaskClone.Mode.MOVE : TaskClone.Mode.FORCE;
				TaskClone task = new TaskClone(selection.minPos(), selection.maxPos(), Manager.playerPos(),
						sender.getEntityWorld(), mode);
				Thread t = new Thread(() -> TaskManager.getInstance().addTask(task));
				t.start();
				TaskManager.getInstance().threads.add(t);
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.copyhere.usage"));
			}
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "true", "false");
		} else {
			return Collections.emptyList();
		}
	}
}