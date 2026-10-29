package net.dugged.csworldedit.clientcommands;

import net.dugged.csworldedit.CSWorldEdit;
import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.brushes.BrushIceSpike;
import net.dugged.csworldedit.brushes.BrushPerimeterWall;
import net.dugged.csworldedit.brushes.BrushPlaceTop;
import net.dugged.csworldedit.brushes.BrushRemoveColumn;
import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.IClientCommand;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class ClientCommandHandler extends CommandHandler {

	public static final int PACKET_LIMIT = 5000;
	private static final ClientCommandHandler instance = new ClientCommandHandler();
	private final Minecraft mc = Minecraft.getMinecraft();
	private boolean forgeCommandsRegistered = false;

	public static ClientCommandHandler getInstance() {
		return instance;
	}

	public static void updatePermissions() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player != null && mc.player.connection != null) {
			mc.player.connection.sendPacket(new CPacketTabComplete("/setbloc", null, false));
			mc.player.connection.sendPacket(new CPacketTabComplete("/fil", null, false));
			mc.player.connection.sendPacket(new CPacketTabComplete("/clon", null, false));
			mc.player.connection.sendPacket(new CPacketTabComplete("/replaceite", null, false));
			mc.player.connection.sendPacket(new CPacketTabComplete("/gamerul", null, false));
		}
	}

	public void init() {
		instance.registerCommand(new CommandUndo());
		instance.registerCommand(new CommandSet());
		instance.registerCommand(new CommandWalls());
		instance.registerCommand(new CommandHollow());
		instance.registerCommand(new CommandCenter());
		instance.registerCommand(new CommandPos());
		instance.registerCommand(new CommandSize());
		instance.registerCommand(new CommandCyl());
		instance.registerCommand(new CommandHCyl());
		instance.registerCommand(new CommandHSphere());
		instance.registerCommand(new CommandSphere());
		instance.registerCommand(new CommandMove());
		instance.registerCommand(new CommandFlip());
		instance.registerCommand(new CommandStack());
		instance.registerCommand(new CommandLine());
		instance.registerCommand(new CommandPolygon());
		instance.registerCommand(new CommandSelection());
		instance.registerCommand(new CommandDrain());
		instance.registerCommand(new CommandFloodFill());
		instance.registerCommand(new CommandOutlineFill());
		instance.registerCommand(new CommandUpscale());
		instance.registerCommand(new CommandCount());
		instance.registerCommand(new CommandFixSlabs());
		instance.registerCommand(new CommandRandomize());
		instance.registerCommand(new CommandReplace());
		instance.registerCommand(new CommandStackQuarter());
		instance.registerCommand(new CommandCancel());
		instance.registerCommand(new CommandCopyHere());
		instance.registerCommand(new CommandFillInventories());
		instance.registerCommand(new CommandPerimeterVolume());
		instance.registerCommand(new CommandBrush());
		instance.registerCommand(new CommandRunBrush());
		instance.registerCommand(new CommandCone());
		instance.registerCommand(new CommandWoolify());
		instance.registerCommand(new CommandErode());
		instance.registerCommand(new CommandStackDiagonal());
		instance.registerCommand(new CommandSwap());
		instance.registerCommand(new CommandDiagLine());

		Manager.brushes.add(new BrushIceSpike());
		Manager.brushes.add(new BrushRemoveColumn());
		Manager.brushes.add(new BrushPerimeterWall());
		Manager.brushes.add(new BrushPlaceTop());

		registerForgeCommands();
	}

	private void registerForgeCommands() {
		if (forgeCommandsRegistered) {
			return;
		}
		forgeCommandsRegistered = true;

		final Set<ICommand> uniqueCommands = Collections.newSetFromMap(new IdentityHashMap<>());
		for (final ICommand command : getCommands().values()) {
			if (command == null || !uniqueCommands.add(command)) {
				continue;
			}
			final net.minecraftforge.client.ClientCommandHandler forgeHandler =
					net.minecraftforge.client.ClientCommandHandler.instance;
			if (forgeHandler.getCommands().containsKey(command.getName())) {
				continue;
			}
			forgeHandler.registerCommand(new ForgeCommandBridge(this, command));
		}
	}

	@Override
	public int executeCommand(ICommandSender sender, String message) {
		message = message.trim();

		if (message.startsWith("/")) {
			message = message.substring(1);
		} else {
			return 0;
		}

		final String[] temp = message.split(" ");
		final String[] args = new String[temp.length - 1];
		final String commandName = temp[0];
		System.arraycopy(temp, 1, args, 0, args.length);
		final ICommand command = getCommands().get(commandName);
		return executeClientCommand(sender, command, args, message);
	}

	private int executeClientCommand(ICommandSender sender, ICommand command, String[] args, String input) {
		if (command instanceof ClientCommand && ((ClientCommand) command).creativeOnly && !(mc.player.isCreative() || mc.player.isSpectator())) {
			final TextComponentTranslation error = new TextComponentTranslation("text.worldedit.wrongGamemode");
			error.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(error);
			return -1;
		}
		if (command == null) {
			return 0;
		}

		try {
			this.tryExecute(sender, args, command, input);
		} catch (Throwable t) {
			final TextComponentTranslation error = new TextComponentTranslation("commands.generic.exception");
			error.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(error);
		}
		return -1;
	}

	private static final class ForgeCommandBridge extends CommandBase implements IClientCommand {
		private final ClientCommandHandler owner;
		private final ICommand delegate;

		private ForgeCommandBridge(ClientCommandHandler owner, ICommand delegate) {
			this.owner = owner;
			this.delegate = delegate;
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public String getUsage(ICommandSender sender) {
			return delegate.getUsage(sender);
		}

		@Override
		public List<String> getAliases() {
			return delegate.getAliases();
		}

		@Override
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if (!Configuration.general.enabled) {
				if (sender instanceof EntityPlayerSP) {
					final StringBuilder command = new StringBuilder("/").append(delegate.getName());
					for (String arg : args) {
						command.append(' ').append(arg);
					}
					((EntityPlayerSP) sender).sendChatMessage(command.toString());
				}
				return;
			}
			StringBuilder input = new StringBuilder(delegate.getName());
			for (String arg : args) {
				input.append(' ').append(arg);
			}
			owner.executeClientCommand(sender, delegate, args, input.toString());
		}

		@Override
		public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
			return true;
		}

		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
											  BlockPos pos) {
			return delegate.getTabCompletions(server, sender, args, pos);
		}

		@Override
		public boolean isUsernameIndex(String[] args, int index) {
			return delegate.isUsernameIndex(args, index);
		}

		@Override
		public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
			return false;
		}
	}

	@Override
	protected MinecraftServer getServer() {
		return mc.getIntegratedServer();
	}

	public void tick() {
		if (mc.world == null) {
			return;
		}
		if (!mc.ingameGUI.getChatGUI().getChatOpen()
				&& (CSWorldEdit.tickCounter % 36000 == 0
				|| (TaskManager.getInstance().isActive() && CSWorldEdit.tickCounter % 600 == 0))) {
			updatePermissions();
		}
		TaskManager.getInstance().tick();
	}
}