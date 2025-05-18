package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

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
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.size.response",
						selection.getPos(A).getX(), selection.getPos(A).getY(), selection.getPos(A).getZ(),
						selection.getPos(B).getX(), selection.getPos(B).getY(), selection.getPos(B).getZ(),
						selection.widthX(), selection.widthY(), selection.widthZ(), selection.volume()));
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}
}
