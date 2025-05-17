package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.mixins.IItemSword;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemSword;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import static net.dugged.worldedit.WorldEditSelection.Position.A;
import static net.dugged.worldedit.WorldEditSelection.Position.B;

public class CommandPos extends ClientCommand {

	public CommandPos() {
		creativeOnly = false;
	}

	@Override
	public String getName() {
		return "pos";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return new TextComponentTranslation(
				"text.worldedit.pos.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args)
			throws CommandException {
		if (args.length == 1) {
			Item itemInHand = mc.player.getHeldItemMainhand().getItem();
			ToolMaterial material =
					itemInHand instanceof ItemSword ? ((IItemSword) itemInHand).getMaterial()
							: ToolMaterial.GOLD;
			if (parseInt(args[0]) == 0) {
				if (WorldEditManager.getPos(material, A) == null || !WorldEditManager.getPos(material, A)
						.equals(WorldEditManager.playerPos())) {
					WorldEditManager.setPos(material, A, WorldEditManager.playerPos());
				} else {
					WorldEditManager.setPos(material, A, null);
				}
			} else if (parseInt(args[0]) == 1) {
				if (WorldEditManager.getPos(material, B) == null || !WorldEditManager.getPos(material, B)
						.equals(WorldEditManager.playerPos())) {
					WorldEditManager.setPos(material, B, WorldEditManager.playerPos());
				} else {
					WorldEditManager.setPos(material, B, null);
				}
			} else {
				WorldEditManager.sendMessage(new TextComponentTranslation(
						"text.worldedit.pos.invalidPosition"));
			}
		} else {
			WorldEditManager.sendMessage(getUsage(sender));
		}
	}
}
