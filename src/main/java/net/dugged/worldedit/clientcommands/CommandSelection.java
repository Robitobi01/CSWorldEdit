package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditSelection;
import net.dugged.worldedit.WorldEditSelection.Position;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

public class CommandSelection extends ClientCommand {

	public CommandSelection() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "selection";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation("text.worldedit.selection.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1 && (args[0].toLowerCase().matches("clear") || args[0].toLowerCase().matches("cl"))) {
			WorldEditManager.clearAllSelections();
			WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.selection.cleared"));
		} else if (args.length == 1 && (args[0].toLowerCase().matches("center") || args[0].toLowerCase().matches("ce"))) {
			WorldEditSelection selection = WorldEditManager.getCurrentSelection();
			BlockPos center1 = new BlockPos(selection.minPos().getX() + selection.widthX() / 2,
					selection.minPos().getY(), selection.minPos().getZ() + selection.widthZ() / 2);
			BlockPos center2 =
					center1.up(selection.widthY() - 1).west(selection.widthZ() % 2 == 0 ? 1 : 0).north(selection.widthX() % 2 == 0 ? 1 : 0);
			selection.setPos(Position.A, center1);
			selection.setPos(Position.B, center2);
			WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.selection.centered"));
		} else if ((args.length == 2 || args.length == 3) && (args[0].toLowerCase().matches("expand") || args[0].toLowerCase().matches("move"))) {
			if (WorldEditManager.hasCurrentSelection()) {
				WorldEditSelection selection = WorldEditManager.getCurrentSelection();
				int amount = args.length == 3 ? parseInt(args[2]) : 1;
				if (args[1].toLowerCase().matches("up") || args[1].toLowerCase().matches("u")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getY() > selection.getPos(B).getY() ? A : B,
								selection.getPos(selection.getPos(A).getY() > selection.getPos(B).getY() ? A : B).up(amount));
					} else {
						selection.setPos(A, selection.getPos(A).up(amount));
						selection.setPos(B, selection.getPos(B).up(amount));
					}
				} else if (args[1].toLowerCase().matches("down") || args[1].toLowerCase().matches("d")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getY() < selection.getPos(B).getY() ? A : B,
								selection.getPos(selection.getPos(A).getY() < selection.getPos(B).getY() ? A : B).down(amount));
					} else {
						selection.setPos(A, selection.getPos(A).down(amount));
						selection.setPos(B, selection.getPos(B).down(amount));
					}
				} else if (args[1].toLowerCase().matches("north") || args[1].toLowerCase().matches("n")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getZ() < selection.getPos(B).getZ() ? A : B,
								selection.getPos(selection.getPos(A).getZ() < selection.getPos(B).getZ() ? A : B).north(amount));
					} else {
						selection.setPos(A, selection.getPos(A).north(amount));
						selection.setPos(B, selection.getPos(B).north(amount));
					}
				} else if (args[1].toLowerCase().matches("east") || args[1].toLowerCase().matches("e")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getX() > selection.getPos(B).getX() ? A : B,
								selection.getPos(selection.getPos(A).getX() > selection.getPos(B).getX() ? A : B).east(amount));
					} else {
						selection.setPos(A, selection.getPos(A).east(amount));
						selection.setPos(B, selection.getPos(B).east(amount));
					}
				} else if (args[1].toLowerCase().matches("south") || args[1].toLowerCase().matches("s")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getZ() > selection.getPos(B).getZ() ? A : B,
								selection.getPos(selection.getPos(A).getZ() > selection.getPos(B).getZ() ? A : B).south(amount));
					} else {
						selection.setPos(A, selection.getPos(A).south(amount));
						selection.setPos(B, selection.getPos(B).south(amount));
					}
				} else if (args[1].toLowerCase().matches("west") || args[1].toLowerCase().matches("w")) {
					if (args[0].toLowerCase().matches("expand")) {
						selection.setPos(selection.getPos(A).getX() < selection.getPos(B).getX() ? A : B,
								selection.getPos(selection.getPos(A).getX() < selection.getPos(B).getX() ? A : B).west(amount));
					} else {
						selection.setPos(A, selection.getPos(A).west(amount));
						selection.setPos(B, selection.getPos(B).west(amount));
					}
				} else {
					WorldEditManager.sendMessage(getUsage(sender));
				}
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
			}
		} else if ((args.length == 1 || args.length == 2) && (args[0].toLowerCase().matches("grow") || args[0].toLowerCase().matches("shrink"))) {
			if (!WorldEditManager.hasCurrentSelection()) {
				WorldEditManager.sendMessage(new TextComponentTranslation("text.worldedit.noAreaSelected"));
				return;
			}
			int amount = args.length == 2 ? parseInt(args[1]) : 1;
			WorldEditSelection selection = WorldEditManager.getCurrentSelection();
			BlockPos min = selection.minPos();
			BlockPos max = selection.maxPos();
			if (args[0].toLowerCase().matches("grow")) {
				selection.setPos(Position.A, min.add(-amount, -amount, -amount));
				selection.setPos(Position.B, max.add(amount, amount, amount));
			} else {
				if (selection.widthX() <= 2 * amount || selection.widthY() <= 2 * amount || selection.widthZ() <= 2 * amount) {
					return;
				}
				selection.setPos(Position.A, min.add(amount, amount, amount));
				selection.setPos(Position.B, max.add(-amount, -amount, -amount));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}

	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
										  @Nullable BlockPos pos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "expand", "move", "clear", "center", "grow", "shrink");
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, "up", "down", "north", "east", "south", "west");
		} else {
			return Collections.emptyList();
		}
	}
}