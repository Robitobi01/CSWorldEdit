package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

public class CommandSize extends ClientCommand {

	public CommandSize() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "size";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.size.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
		if (args.length == 0) {
			if (Manager.hasCurrentSelection()) {
				Selection selection = Manager.getCurrentSelection();
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.size.response",
						selection.getPos(A).getX(), selection.getPos(A).getY(), selection.getPos(A).getZ(),
						selection.getPos(B).getX(), selection.getPos(B).getY(), selection.getPos(B).getZ(),
						selection.widthX(), selection.widthY(), selection.widthZ(), selection.volume()));
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			Manager.sendMessage(getUsage(sender));
		}
	}
}
