package net.dugged.csworldedit.api;

import net.dugged.csworldedit.tasks.TaskChunk;
import net.minecraft.util.math.ChunkPos;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ApiCommandTask extends TaskChunk {
	private final Map<ChunkPos, Deque<PasteCommandPlanner.Command>> ops;
	private final int total;
	private int processed;
	private boolean cancelled;

	ApiCommandTask(List<BlockChange> changes) {
		this.ops = PasteCommandPlanner.plan(changes);
		this.total = changes.size();
	}

	@Override
	public int processChunk(ChunkPos pos, int maxPackets) {
		if (cancelled) {
			return 0;
		}
		Deque<PasteCommandPlanner.Command> queue = ops.get(pos);
		if (queue == null) {
			return 0;
		}
		int commands = 0;
		while (commands < maxPackets && !queue.isEmpty()) {
			PasteCommandPlanner.Command command = queue.poll();
			if (command.single()) {
				sendCommand("/setblock " + command.x1 + " " + command.y1 + " " + command.z1 + " "
						+ command.state.getBlock().getRegistryName() + " "
						+ command.state.getBlock().getMetaFromState(command.state));
			} else {
				sendCommand("/fill " + command.x1 + " " + command.y1 + " " + command.z1 + " " + command.x2 + " "
						+ command.y2 + " " + command.z2 + " " + command.state.getBlock().getRegistryName() + " "
						+ command.state.getBlock().getMetaFromState(command.state));
			}
			processed += command.count;
			commands++;
		}
		if (queue.isEmpty()) {
			ops.remove(pos);
		}
		return commands;
	}

	@Override
	public int getTotalOperations() {
		return total;
	}

	@Override
	public int getProcessedOperations() {
		return processed;
	}

	@Override
	public Set<ChunkPos> getPendingChunks() {
		return new HashSet<>(ops.keySet());
	}

	@Override
	public boolean isComplete() {
		return cancelled || ops.isEmpty();
	}

	boolean isCancelled() {
		return cancelled;
	}

	void cancel() {
		cancelled = true;
		ops.clear();
	}
}
