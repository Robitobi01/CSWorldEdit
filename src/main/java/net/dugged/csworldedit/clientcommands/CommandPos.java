package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.mixins.IItemSword;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemSword;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

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
		return new TextComponentTranslation("text.worldedit.pos.usage").getUnformattedText();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 1) {
			Item itemInHand = mc.player.getHeldItemMainhand().getItem();
			ToolMaterial material = itemInHand instanceof ItemSword ? ((IItemSword) itemInHand).getMaterial() :
					ToolMaterial.GOLD;
			if (parseInt(args[0]) == 0) {
				if (Manager.getPos(material, A) == null || !Manager.getPos(material, A).equals(Manager.playerPos())) {
					Manager.setPos(material, A, Manager.playerPos());
				} else {
					Manager.setPos(material, A, null);
				}
			} else if (parseInt(args[0]) == 1) {
				if (Manager.getPos(material, B) == null || !Manager.getPos(material, B).equals(Manager.playerPos())) {
					Manager.setPos(material, B, Manager.playerPos());
				} else {
					Manager.setPos(material, B, null);
				}
			} else {
				Manager.sendMessage(new TextComponentTranslation("text.worldedit.pos.invalidPosition"));
			}
		} else {
			Manager.sendMessage(getUsage(sender));
		}
	}
}
