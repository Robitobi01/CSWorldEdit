package net.dugged.csworldedit;

import net.dugged.csworldedit.sp.SPEditBackend;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Deque;
import java.util.LinkedList;

public class UndoManager {

	private static final UndoManager INSTANCE = new UndoManager();
	private static final int MAX_HISTORY = 10;
	private static volatile boolean isUndoing = false;
	public static volatile boolean savingUndo = true;
	private final Deque<UndoSnapshot> undoHistory = new LinkedList<>();

	public static UndoManager getInstance() {
		return INSTANCE;
	}

	private static double parseCoordinate(double base, String s, int min, int max, boolean centerBlock) {
		boolean rel = s.startsWith("~");
		String t = rel ? s.substring(1) : s;
		double d = 0.0;
		if (!rel || !t.isEmpty()) {
			boolean hasDot = t.indexOf('.') >= 0;
			if (!t.isEmpty()) d += Double.parseDouble(t);
			if (!hasDot && !rel && centerBlock) d += 0.5;
		}
		double r = d + (rel ? base : 0.0);
		if (min != 0 || max != 0) {
			if (r < min) r = min;
			if (r > max) r = max;
		}
		return r;
	}

	private static BlockPos parseBlockPos(String[] args, int startIndex,
										  net.minecraft.client.entity.EntityPlayerSP player) {
		double x = parseCoordinate(player.posX, args[startIndex], -30000000, 30000000, false);
		double y = parseCoordinate(player.posY, args[startIndex + 1], 0, 255, false);
		double z = parseCoordinate(player.posZ, args[startIndex + 2], -30000000, 30000000, false);
		return new BlockPos(x, y, z);
	}

	public synchronized void pushSnapshot(UndoSnapshot snapshot) {
		if (snapshot == null || (snapshot.isEmpty() && !snapshot.isPending())
				|| TaskManager.getInstance().isCancelledThread(Thread.currentThread())) {
			return;
		}
		if (savingUndo && !isUndoing) {
			snapshot.clearPending();
			undoHistory.push(snapshot);
			while (undoHistory.size() > MAX_HISTORY) {
				undoHistory.removeLast();
			}
		}
	}

	public synchronized void clearHistory() {
		undoHistory.clear();
	}

	public synchronized void undoLast(World world) {
		if (!undoHistory.isEmpty()) {
			isUndoing = true;
			try {
				TaskManager.getInstance().cancelAllTasks();
				UndoSnapshot snapshot = undoHistory.pop();
				if (SPEditBackend.isAvailable(world)) {
					if (!SPEditBackend.getInstance().submitUndo(snapshot, world)) {
						undoHistory.push(snapshot);
					}
				} else {
					TaskSetBlock task = new TaskSetBlock(snapshot.getUndoData(), world);
					TaskManager.getInstance().addTask(task);
				}
			} finally {
				isUndoing = false;
			}
		}
	}

	public void saveCmdUndo(String msg, World world, net.minecraft.client.entity.EntityPlayerSP player) {
		if (!savingUndo) {
			return;
		}
		String[] temp = msg.split(" ");
		if (temp.length < 4) {
			return;
		}
		String[] args = new String[temp.length - 1];
		System.arraycopy(temp, 1, args, 0, args.length);
		try {
			if (msg.startsWith("/fill") && args.length >= 7) {
				BlockPos pos1 = parseBlockPos(args, 0, player);
				BlockPos pos2 = parseBlockPos(args, 3, player);
				UndoSnapshot snapshot = new UndoSnapshot();
				snapshot.recordRegion(pos1, pos2, world);
				pushSnapshot(snapshot);
			} else if (msg.startsWith("/clone") && args.length >= 9) {
				BlockPos pos1 = parseBlockPos(args, 0, player);
				BlockPos pos2 = parseBlockPos(args, 3, player);
				BlockPos pos3 = parseBlockPos(args, 6, player);
				BlockPos pos4 = pos3.add(Math.max(pos1.getX(), pos2.getX()) - Math.min(pos1.getX(), pos2.getX()),
						Math.max(pos1.getY(), pos2.getY()) - Math.min(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(),
								pos2.getZ()) - Math.min(pos1.getZ(), pos2.getZ()));
				UndoSnapshot snapshot = new UndoSnapshot();
				snapshot.recordRegion(pos3, pos4, world);
				pushSnapshot(snapshot);
			}
		} catch (Exception ignored) {
		}
	}
}