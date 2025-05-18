package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.UndoManager;
import net.dugged.worldedit.WorldEditManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandUndo extends ClientCommand {

	private static final Minecraft mc = Minecraft.getMinecraft();

	@Override
	public String getName() {
		return "undo";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return getName();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (sender instanceof EntityPlayerSP) {
			if (mc.player.isCreative() || mc.player.isSpectator()) {
				World world = sender.getEntityWorld();
				if (args.length >= 1) {
					if (args[0].equalsIgnoreCase("toggle")) {
						UndoManager.savingUndo = !UndoManager.savingUndo;
						if (UndoManager.savingUndo) {
							WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.undo" +
									".enabledUndo"));
						} else {
							WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.undo" +
									".disabledUndo"));
						}
					} else {
						UndoManager.getInstance().undoLast(world);
					}
				} else {
					UndoManager.getInstance().undoLast(world);
				}
			} else {
				throw new CommandException("text.worldedit.wrongGamemode");
			}
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable net.minecraft.util.math.BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "toggle", "redo");
		} else {
			return Collections.emptyList();
		}
	}
}