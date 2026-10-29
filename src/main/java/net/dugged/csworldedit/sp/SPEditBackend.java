package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.Configuration;
import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SPEditBackend {
	private static final SPEditBackend INSTANCE = new SPEditBackend();
	private final Object lock = new Object();
	private final ArrayDeque<SPEditJob> queue = new ArrayDeque<>();
	private volatile long generation;
	private SPEditJob active;

	SPEditBackend() {
	}

	public static SPEditBackend getInstance() {
		return INSTANCE;
	}

	public static boolean isAvailable(World world) {
		if (!Configuration.direct.enabled || !(world instanceof WorldClient)) {
			return false;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.player == null || mc.world != world) {
			return false;
		}
		MinecraftServer server = mc.getIntegratedServer();
		return server != null && server.isSinglePlayer()
				&& !(server instanceof IntegratedServer && ((IntegratedServer) server).getPublic())
				&& server.getWorld(world.provider.getDimension()) != null;
	}

	public SPEditJob createJob(World world, List<SPOperation> operations, boolean recordUndo) {
		return createJob(world, operations, recordUndo, null);
	}

	public SPEditJob createJob(World world, List<SPOperation> operations, boolean recordUndo,
							   UndoSnapshot sharedUndo) {
		if (!isAvailable(world)) {
			return null;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.player == null) {
			return null;
		}
		UUID playerId = mc.player.getUniqueID();
		return new SPEditJob(playerId, world.provider.getDimension(), generation, operations, recordUndo,
				sharedUndo);
	}

	public boolean submit(SPEditJob job) {
		if (job == null) {
			return false;
		}
		synchronized (lock) {
			if (job.generation() != generation) {
				job.cancel();
				return true;
			}
			int limit = Math.max(1, Configuration.direct.queueLimit);
			if (queue.size() >= limit) {
				return false;
			}
			queue.add(job);
			return true;
		}
	}

	public boolean submitUndo(UndoSnapshot snapshot, World world) {
		if (snapshot == null || snapshot.isEmpty()) {
			return true;
		}
		List<SPOperation> operations = new ArrayList<>(1);
		operations.add(new SPUndoOperation(snapshot));
		SPEditJob job = createJob(world, operations, false);
		return job != null && submit(job);
	}

	public long getGeneration() {
		return generation;
	}

	public int getQueueDepth() {
		synchronized (lock) {
			return queue.size() + (active == null ? 0 : 1);
		}
	}

	public boolean isActive() {
		synchronized (lock) {
			if (active != null && !active.isDone()) {
				return true;
			}
			for (SPEditJob job : queue) {
				if (!job.isDone()) {
					return true;
				}
			}
			return false;
		}
	}

	public void worldChanged() {
		synchronized (lock) {
			generation++;
			if (active != null) {
				active.discardUndo();
				active = null;
			}
			for (SPEditJob job : queue) {
				job.discardUndo();
			}
			queue.clear();
		}
	}

	public void cancelAll() {
		synchronized (lock) {
			generation++;
			if (active != null) {
				active.cancel();
			}
			for (SPEditJob job : queue) {
				job.cancel();
			}
			queue.clear();
		}
	}

	public int process(SPEditWorld world, boolean canEdit, int budget) {
		SPEditJob job = current();
		if (job == null) {
			return 0;
		}
		if (job.generation() != generation || job.dimension() != world.dimension() || !canEdit) {
			job.cancel();
			job.finish();
			clear(job);
			return 0;
		}
		int used = job.apply(world, Math.max(1, budget));
		if (job.isDone()) {
			job.finish();
			clear(job);
		}
		return used;
	}

	public void serverTick(MinecraftServer server) {
		if (server == null || !server.isSinglePlayer() || !server.isCallingFromMinecraftThread()) {
			return;
		}
		if (server instanceof IntegratedServer && ((IntegratedServer) server).getPublic()) {
			if (isActive()) {
				cancelAll();
			}
			return;
		}
		if (!Configuration.direct.enabled) {
			return;
		}
		SPEditJob job = current();
		if (job == null) {
			return;
		}
		if (job.generation() != generation) {
			job.cancel();
			job.finish();
			clear(job);
			return;
		}
		WorldServer world = server.getWorld(job.dimension());
		EntityPlayerMP player = world == null ? null : server.getPlayerList().getPlayerByUUID(job.playerId());
		if (world == null || player == null) {
			job.cancel();
			job.finish();
			clear(job);
			return;
		}
		boolean canEdit = player.isCreative() || player.isSpectator();
		int budget = Math.max(1, Configuration.direct.blocksPerTick);
		int used = job.apply(new SPWorldAdapter(world), budget);
		if (job.isDone()) {
			job.finish();
			clear(job);
		} else if (used <= 0) {
			job.cancel();
			job.finish();
			clear(job);
		}
	}

	private SPEditJob current() {
		synchronized (lock) {
			if (active != null) {
				return active;
			}
			SPEditJob job;
			while ((job = queue.poll()) != null) {
				if (job.generation() != generation || job.isDone()) {
					job.cancel();
					continue;
				}
				active = job;
				return job;
			}
			return null;
		}
	}

	private void clear(SPEditJob job) {
		synchronized (lock) {
			if (active == job) {
				active = null;
			}
		}
	}
}
