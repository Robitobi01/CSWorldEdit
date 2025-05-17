package net.dugged.worldedit.tasks;

import net.dugged.worldedit.Configuration;
import net.dugged.worldedit.clientcommands.ClientCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.math.ChunkPos;

import java.util.*;

public class TaskManager {
	private static final TaskManager instance = new TaskManager();
	public static boolean gamerulePermission = false;
	public final List<Thread> threads = new LinkedList<>();
	private final List<TaskChunk> tasks = new LinkedList<>();
	private final Set<ChunkPos> currentlyProcessing = new HashSet<>();
	private boolean processing = false;

	public static TaskManager getInstance() {
		return instance;
	}

	public void addTask(TaskChunk task) {
		tasks.add(task);
	}

	public List<TaskChunk> getTasks() {
		return Collections.unmodifiableList(tasks);
	}

	public float getProgress() {
		int total = 0;
		int processed = 0;
		for (TaskChunk task : tasks) {
			total += task.getTotalOperations();
			processed += task.getProcessedOperations();
		}
		return total == 0 ? 0 : (float) processed / total;
	}

	public boolean isWaitingForPlayer() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null) {
			return false;
		}
		ChunkPos playerChunk = new ChunkPos(mc.player.getPosition());

		for (TaskChunk task : tasks) {
			if (task instanceof TaskClone && ((TaskClone) task).isOverlapSensitive()) {
				TaskClone cloneTask = (TaskClone) task;
				List<ChunkPos> orderedChunks = cloneTask.getOrderedDestinationChunks();
				for (ChunkPos cpNext : orderedChunks) {
					if (cloneTask.getPendingChunks().contains(cpNext)) {
						if (Math.abs(cpNext.x - playerChunk.x) > Configuration.operationRange || Math.abs(cpNext.z - playerChunk.z) > Configuration.operationRange) {
							return true;
						}
						break;
					}
				}
			} else {
				for (ChunkPos cp : new HashSet<>(task.getPendingChunks())) {
					if (Math.abs(cp.x - playerChunk.x) > Configuration.operationRange || Math.abs(cp.z - playerChunk.z) > Configuration.operationRange) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isActive() {
		return !tasks.isEmpty();
	}

	public Set<ChunkPos> getCurrentlyProcessingChunks() {
		return currentlyProcessing;
	}

	public void tick() {
		currentlyProcessing.clear();
		int packetsThisTick = 0;
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null || mc.world == null) return;
		ChunkPos playerChunk = new ChunkPos(mc.player.getPosition());

		boolean wasProcessing = processing;
		processing = !tasks.isEmpty();

		if (processing && !wasProcessing) {
			if (gamerulePermission) {
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule logAdminCommands false"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule doTileDrops false"));
			}
		}

		Iterator<TaskChunk> taskIterator = tasks.iterator();
		while (taskIterator.hasNext() && packetsThisTick < ClientCommandHandler.PACKET_LIMIT) {
			TaskChunk task = taskIterator.next();

			if (task instanceof TaskClone && ((TaskClone) task).isOverlapSensitive()) {
				TaskClone cloneTask = (TaskClone) task;
				List<ChunkPos> orderedChunks = cloneTask.getOrderedDestinationChunks();
				ChunkPos nextChunkToProcessInOrder = null;

				for (ChunkPos orderedCp : orderedChunks) {
					if (cloneTask.getPendingChunks().contains(orderedCp)) {
						nextChunkToProcessInOrder = orderedCp;
						break;
					}
				}

				if (nextChunkToProcessInOrder != null) {
					if (Math.abs(nextChunkToProcessInOrder.x - playerChunk.x) <= Configuration.operationRange &&
							Math.abs(nextChunkToProcessInOrder.z - playerChunk.z) <= Configuration.operationRange) {

						int processedInChunk = cloneTask.processChunk(nextChunkToProcessInOrder, ClientCommandHandler.PACKET_LIMIT - packetsThisTick);
						if (processedInChunk > 0) {
							currentlyProcessing.add(nextChunkToProcessInOrder);
							packetsThisTick += processedInChunk;
						}
					}
				}
			} else {
				List<ChunkPos> pending = new ArrayList<>(task.getPendingChunks());
				for (ChunkPos cpToProcess : pending) {
					if (packetsThisTick >= ClientCommandHandler.PACKET_LIMIT) {
						break;
					}
					if (Math.abs(cpToProcess.x - playerChunk.x) <= Configuration.operationRange &&
							Math.abs(cpToProcess.z - playerChunk.z) <= Configuration.operationRange) {
						int processedInChunk = task.processChunk(cpToProcess, ClientCommandHandler.PACKET_LIMIT - packetsThisTick);
						if (processedInChunk > 0) {
							currentlyProcessing.add(cpToProcess);
							packetsThisTick += processedInChunk;
						}
					}
				}
			}

			if (task.isComplete()) {
				taskIterator.remove();
			}
		}

		if (wasProcessing && tasks.isEmpty()) {
			processing = false;
			if (gamerulePermission) {
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule doTileDrops true"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule logAdminCommands true"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
			}
		}
		threads.removeIf(thread -> thread.isInterrupted() || !thread.isAlive());
	}
}