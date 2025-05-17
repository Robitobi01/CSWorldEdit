package net.dugged.worldedit.clientcommands;

import net.dugged.worldedit.WorldEditManager;
import net.dugged.worldedit.WorldEditRenderer;
import net.dugged.worldedit.brushes.BrushIceSpike;
import net.dugged.worldedit.brushes.BrushPerimeterWall;
import net.dugged.worldedit.brushes.BrushPlaceTop;
import net.dugged.worldedit.brushes.BrushRemoveColumn;
import net.dugged.worldedit.tasks.TaskManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.stream.IntStream;

import static net.minecraft.util.text.TextFormatting.GRAY;
import static net.minecraft.util.text.TextFormatting.RESET;

public class ClientCommandHandler extends CommandHandler {

	private static final ClientCommandHandler instance = new ClientCommandHandler();
	public static int PACKET_LIMIT = 5000;
	private final Minecraft mc = Minecraft.getMinecraft();
	public String[] latestAutoComplete = null;
	private long tick = 0;

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

		WorldEditManager.brushes.add(new BrushIceSpike());
		WorldEditManager.brushes.add(new BrushRemoveColumn());
		WorldEditManager.brushes.add(new BrushPerimeterWall());
		WorldEditManager.brushes.add(new BrushPlaceTop());
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
		if (command instanceof ClientCommand && ((ClientCommand) command).creativeOnly && !(
				mc.player.isCreative() || mc.player.isSpectator())) {
			final TextComponentTranslation error = new TextComponentTranslation(
					"text.worldedit.wrongGamemode");
			error.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(error);
			return -1;
		}
		if (command == null) {
			return 0;
		}

		try {
			this.tryExecute(sender, args, command, message);
		} catch (Throwable t) {
			final TextComponentTranslation error = new TextComponentTranslation(
					"commands.generic.exception");
			error.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(error);
		}
		return -1;
	}

	public void autoComplete(String leftOfCursor) {
		this.latestAutoComplete = null;
		if (leftOfCursor.charAt(0) == '/') {
			leftOfCursor = leftOfCursor.substring(1);
			if (mc.currentScreen instanceof GuiChat) {
				final List<String> commands = this.getTabCompletions(mc.player, leftOfCursor,
						mc.player.getPosition());
				if (!commands.isEmpty()) {
					if (leftOfCursor.indexOf(' ') == -1) {
						IntStream.range(0, commands.size())
								.forEach(s -> commands.set(s, GRAY + "/" + commands.get(s) + RESET));
					} else {
						IntStream.range(0, commands.size())
								.forEach(s -> commands.set(s, GRAY + commands.get(s) + RESET));
					}
					this.latestAutoComplete = commands.toArray(new String[0]);
				}
			}
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
		if (tick % 36000 == 0 && !mc.ingameGUI.getChatGUI().getChatOpen()) {
			updatePermissions();
		}
		TaskManager.getInstance().tick();
		WorldEditRenderer.update();
		tick++;
	}

	public static class PlayerPos {

		public BlockPos position;
		public int dimension;

		PlayerPos(BlockPos pos, int dim) {
			update(pos, dim);
		}

		public void update(BlockPos pos, int dim) {
			dimension = dim;
			position = pos;
		}
	}
}