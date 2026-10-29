package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.CSWorldEdit;
import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.clientcommands.ClientCommandHandler;
import net.dugged.csworldedit.sp.SPEditBackend;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskManager {
	private static final TaskManager instance = new TaskManager();
	public static boolean gamerulePermission = false;
	public final List<Thread> threads = new CopyOnWriteArrayList<>();
	private final List<TaskChunk> tasks = new ArrayList<>();
	private final List<Runnable> pendingClientWork = new ArrayList<>();
	private final Object tasksLock = new Object();
	private final Set<ChunkPos> currentlyProcessing = new HashSet<>();
	private final Set<Thread> cancelledThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private boolean gamerulesSuppressed = false;

	public static TaskManager getInstance() {
		return instance;
	}

	public void addTask(TaskChunk task) {
		if (task == null) {
			return;
		}
		synchronized (tasksLock) {
			if (cancelledThreads.contains(Thread.currentThread()) || Thread.currentThread().isInterrupted()) {
				return;
			}
			tasks.add(task);
		}
	}

	public void scheduleClient(Runnable work) {
		if (work == null) {
			return;
		}
		synchronized (tasksLock) {
			if (cancelledThreads.contains(Thread.currentThread()) || Thread.currentThread().isInterrupted()) {
				return;
			}
			pendingClientWork.add(work);
		}
	}

	public void clearAllTasks() {
		synchronized (tasksLock) {
			for (TaskChunk task : tasks) {
				task.cancelDirect();
			}
			SPEditBackend.getInstance().cancelAll();
			tasks.clear();
			pendingClientWork.clear();
			currentlyProcessing.clear();
		}
	}

	public void cancelAllTasks() {
		synchronized (tasksLock) {
			for (Thread thread : threads) {
				cancelledThreads.add(thread);
				thread.interrupt();
			}
			for (TaskChunk task : tasks) {
				task.cancelDirect();
			}
			SPEditBackend.getInstance().cancelAll();
			tasks.clear();
			pendingClientWork.clear();
			currentlyProcessing.clear();
		}
	}

	public void resetForWorldChange() {
		synchronized (tasksLock) {
			for (Thread thread : threads) {
				cancelledThreads.add(thread);
				thread.interrupt();
			}
			threads.clear();
			tasks.clear();
			pendingClientWork.clear();
			currentlyProcessing.clear();
			gamerulesSuppressed = false;
		}
		SPEditBackend.getInstance().worldChanged();
	}

	public List<TaskChunk> getTasks() {
		synchronized (tasksLock) {
			return new ArrayList<>(tasks);
		}
	}

	public float getProgress() {
		synchronized (tasksLock) {
			long total = 0;
			long processed = 0;
			for (TaskChunk task : tasks) {
				total += task.getTotalOperations();
				processed += task.getProcessedOperations();
			}
			return total == 0 ? 0 : (float) ((double) processed / total);
		}
	}

	public boolean isWaitingForPlayer() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null) {
			return false;
		}
		ChunkPos playerChunk = new ChunkPos(mc.player.getPosition());

		synchronized (tasksLock) {
			for (TaskChunk task : tasks) {
				if (task.isDirectMode()) {
					continue;
				}
				if (task instanceof TaskClone && ((TaskClone) task).isOverlapSensitive()) {
					TaskClone cloneTask = (TaskClone) task;
					List<ChunkPos> orderedChunks = cloneTask.getOrderedDestinationChunks();
					for (ChunkPos cpNext : orderedChunks) {
						if (cloneTask.getPendingChunks().contains(cpNext)) {
							if (Math.abs(cpNext.x - playerChunk.x) > Configuration.general.operationRangeChunks || Math.abs(cpNext.z - playerChunk.z) > Configuration.general.operationRangeChunks) {
								return true;
							}
							break;
						}
					}
				} else {
					for (ChunkPos cp : new HashSet<>(task.getPendingChunks())) {
						if (Math.abs(cp.x - playerChunk.x) > Configuration.general.operationRangeChunks || Math.abs(cp.z - playerChunk.z) > Configuration.general.operationRangeChunks) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean isActive() {
		synchronized (tasksLock) {
			return !tasks.isEmpty() || !pendingClientWork.isEmpty()
					|| SPEditBackend.getInstance().isActive();
		}
	}

	public boolean isCancelledThread(Thread thread) {
		return cancelledThreads.contains(thread);
	}

	public boolean isCancelledProducer(Thread thread) {
		return isCancelledThread(thread);
	}

	public boolean isCancelledProducer() {
		return isCancelledThread(Thread.currentThread());
	}

	public long getGeneration() {
		return SPEditBackend.getInstance().getGeneration();
	}

	public void tick() {
		currentlyProcessing.clear();
		int packetsThisTick = 0;
		Minecraft mc = Minecraft.getMinecraft();

		threads.removeIf(thread -> thread.isInterrupted() || !thread.isAlive());
		synchronized (tasksLock) {
			cancelledThreads.removeIf(thread -> !thread.isAlive());
		}

		if (mc.player == null || mc.world == null) return;
		ChunkPos playerChunk = new ChunkPos(mc.player.getPosition());

		synchronized (tasksLock) {
			drainClientWork();

			if (!tasks.isEmpty() && !gamerulesSuppressed && gamerulePermission) {
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule sendCommandFeedback false"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule logAdminCommands false"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule doTileDrops false"));
				gamerulesSuppressed = true;
			}

			Iterator<TaskChunk> taskIterator = tasks.iterator();
			while (taskIterator.hasNext() && packetsThisTick < ClientCommandHandler.PACKET_LIMIT) {
				TaskChunk task = taskIterator.next();

				if (task.isDirectMode()) {
					task.trySubmitDirect();
					if (task.isComplete()) {
						taskIterator.remove();
					}
					continue;
				}

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
						if (Math.abs(nextChunkToProcessInOrder.x - playerChunk.x) <= Configuration.general.operationRangeChunks && Math.abs(nextChunkToProcessInOrder.z - playerChunk.z) <= Configuration.general.operationRangeChunks) {

							int processedInChunk = cloneTask.processChunk(nextChunkToProcessInOrder,
									ClientCommandHandler.PACKET_LIMIT - packetsThisTick);
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
						if (Math.abs(cpToProcess.x - playerChunk.x) <= Configuration.general.operationRangeChunks && Math.abs(cpToProcess.z - playerChunk.z) <= Configuration.general.operationRangeChunks) {
							int processedInChunk = task.processChunk(cpToProcess,
									ClientCommandHandler.PACKET_LIMIT - packetsThisTick);
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

			if (tasks.isEmpty() && pendingClientWork.isEmpty() && gamerulesSuppressed) {
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule doTileDrops true"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule logAdminCommands true"));
				mc.player.connection.sendPacket(new CPacketChatMessage("/gamerule sendCommandFeedback true"));
				gamerulesSuppressed = false;
			}
		}
	}

	private void drainClientWork() {
		if (pendingClientWork.isEmpty()) {
			return;
		}
		List<Runnable> work = new ArrayList<>(pendingClientWork);
		pendingClientWork.clear();
		for (Runnable runnable : work) {
			try {
				runnable.run();
			} catch (Throwable t) {
				CSWorldEdit.LOGGER.error("Failed to schedule client-side task work", t);
			}
		}
	}
}
