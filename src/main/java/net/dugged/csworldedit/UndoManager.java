package net.dugged.csworldedit;

import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Deque;
import java.util.LinkedList;

public class UndoManager {

	private static final UndoManager INSTANCE = new UndoManager();
	private static final int MAX_HISTORY = 10;
	public static boolean isUndoing = false;
	public static boolean savingUndo = true;
	private final Deque<UndoSnapshot> undoHistory = new LinkedList<>();

	public static UndoManager getInstance() {
		return INSTANCE;
	}

	private static BlockPos parseBlockPos(String[] args, int startIndex,
										  net.minecraft.client.entity.EntityPlayerSP player) throws NumberFormatException {
		BlockPos base = player.getPosition();
		double x = Double.parseDouble(args[startIndex]);
		double y = Double.parseDouble(args[startIndex + 1]);
		double z = Double.parseDouble(args[startIndex + 2]);
		return new BlockPos(base.getX() + x, Math.max(0, Math.min(base.getY() + y, 255)), base.getZ() + z);
	}

	public void pushSnapshot(UndoSnapshot snapshot) {
		if (savingUndo && !isUndoing) {
			undoHistory.push(snapshot);
			if (undoHistory.size() > MAX_HISTORY) {
				undoHistory.removeLast();
			}
		}
	}

	public void undoLast(World world) {
		if (!undoHistory.isEmpty()) {
			isUndoing = true;
			UndoSnapshot snapshot = undoHistory.pop();
			TaskSetBlock task = new TaskSetBlock(snapshot.getUndoData(), world);
			TaskManager.getInstance().addTask(task);
			isUndoing = false;
		}
	}

	public boolean saveHistory(String msg, World world, net.minecraft.client.entity.EntityPlayerSP player) {
		if (!savingUndo) {
			return false;
		}
		String[] temp = msg.split(" ");
		if (temp.length < 4) {
			return false;
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
				return true;
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
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}
}