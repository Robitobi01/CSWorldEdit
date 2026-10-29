package net.dugged.csworldedit.scan;

import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class SnapshotManager {

	static final Logger LOGGER = LogManager.getLogger("csworldedit-scan");
	static final Object NULL_WORLD = new Object();

	public enum SubmitResult {
		ACCEPTED, QUEUE_FULL, TOO_LARGE, INVALID
	}

	public static final int DEFAULT_TICK_BUDGET = 4096;
	public static final int MAX_PENDING_JOBS = 8;
	public static final long MAX_SNAPSHOT_VOLUME = 8_388_608L;
	public static final long MAX_RETAINED_VOLUME = 16_777_216L;

	private static final SnapshotManager INSTANCE = new SnapshotManager();

	private final Object lock = new Object();
	private final List<ClientJob> jobs = new ArrayList<>();
	private final AtomicLong globalEpoch = new AtomicLong(1);
	private final ConcurrentHashMap<Object, AtomicLong> worldEpochs = new ConcurrentHashMap<>();
	private long retainedVolume;
	private final ExecutorService analysisExecutor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(16), runnable -> {
		Thread thread = new Thread(runnable, "csworldedit-scan");
		thread.setDaemon(true);
		return thread;
	}, new ThreadPoolExecutor.AbortPolicy());
	private volatile int tickBudget = DEFAULT_TICK_BUDGET;

	private SnapshotManager() {
	}

	public static SnapshotManager getInstance() {
		return INSTANCE;
	}

	public SubmitResult submitRegion(World world, BlockReader reader, int minX, int minY, int minZ, int maxX,
									 int maxY, int maxZ, RegionAnalysis analysis) {
		return submitRegion(world, reader, null, minX, minY, minZ, maxX, maxY, maxZ, analysis);
	}

	public SubmitResult submitRegion(World world, BlockReader reader, BedColorReader bedColorReader, int minX, int minY,
									 int minZ, int maxX, int maxY, int maxZ, RegionAnalysis analysis) {
		if (reader == null || analysis == null || maxX < minX || maxY < minY || maxZ < minZ) {
			return SubmitResult.INVALID;
		}
		long sizeX = (long) maxX - minX + 1L;
		long sizeY = (long) maxY - minY + 1L;
		long sizeZ = (long) maxZ - minZ + 1L;
		long volume = sizeX;
		if (volume <= 0 || volume > MAX_SNAPSHOT_VOLUME / sizeY) {
			return SubmitResult.TOO_LARGE;
		}
		volume *= sizeY;
		if (volume > MAX_SNAPSHOT_VOLUME / sizeZ) {
			return SubmitResult.TOO_LARGE;
		}
		volume *= sizeZ;
		if (volume <= 0 || volume > MAX_SNAPSHOT_VOLUME) {
			return SubmitResult.TOO_LARGE;
		}
		synchronized (lock) {
			if (jobs.size() >= MAX_PENDING_JOBS) {
				return SubmitResult.QUEUE_FULL;
			}
			if (retainedVolume + volume > MAX_RETAINED_VOLUME) {
				return SubmitResult.QUEUE_FULL;
			}
			BlockSnapshot.Builder builder = new BlockSnapshot.Builder(minX, minY, minZ, (int) sizeX, (int) sizeY,
					(int) sizeZ, bedColorReader != null);
			RegionJob job = new RegionJob(this, world, reader, bedColorReader, builder, analysis);
			bind(job);
			jobs.add(job);
			retainedVolume += volume;
			return SubmitResult.ACCEPTED;
		}
	}

	public SubmitResult submitClientJob(ClientJob job) {
		if (job == null) {
			return SubmitResult.INVALID;
		}
		synchronized (lock) {
			if (jobs.size() >= MAX_PENDING_JOBS) {
				return SubmitResult.QUEUE_FULL;
			}
			bind(job);
			jobs.add(job);
			return SubmitResult.ACCEPTED;
		}
	}

	private void bind(ClientJob job) {
		Object key = job.worldKey();
		AtomicLong epoch = worldEpochs.computeIfAbsent(key, ignored -> new AtomicLong());
		job.bind(this, globalEpoch.get(), epoch.get());
	}

	public void tick() {
		synchronized (lock) {
			if (jobs.isEmpty()) {
				return;
			}
			int remaining = tickBudget;
			Iterator<ClientJob> iterator = jobs.iterator();
			while (iterator.hasNext()) {
				ClientJob job = iterator.next();
				if (!job.alive()) {
					release(job);
					iterator.remove();
					continue;
				}
				if (remaining <= 0) {
					break;
				}
				if (Thread.currentThread() != job.ownerThread()) {
					job.cancelInternal();
					release(job);
					iterator.remove();
					continue;
				}
				int used;
				try {
					used = job.step(remaining);
				} catch (Throwable throwable) {
					LOGGER.error("Client scan job failed", throwable);
					job.cancelInternal();
					release(job);
					iterator.remove();
					continue;
				}
				if (used < 0) {
					used = 0;
				}
				if (used > remaining) {
					used = remaining;
				}
				remaining -= used;
				if (job.isFinished()) {
					iterator.remove();
				} else if (used == 0) {
					break;
				}
			}
		}
	}

	public void cancelAll() {
		synchronized (lock) {
			globalEpoch.incrementAndGet();
			for (ClientJob job : jobs) {
				job.cancelInternal();
				release(job);
			}
			jobs.clear();
		}
	}

	public void cancelWorld(World world) {
		Object key = world == null ? NULL_WORLD : world;
		synchronized (lock) {
			worldEpochs.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
			Iterator<ClientJob> iterator = jobs.iterator();
			while (iterator.hasNext()) {
				ClientJob job = iterator.next();
				if (job.worldKey() == key) {
					job.cancelInternal();
					release(job);
					iterator.remove();
				}
			}
		}
	}

	public int tickBudget() {
		return tickBudget;
	}

	public void setTickBudget(int tickBudget) {
		if (tickBudget > 0) {
			this.tickBudget = tickBudget;
		}
	}

	public int pendingJobs() {
		synchronized (lock) {
			return jobs.size();
		}
	}

	long globalEpoch() {
		return globalEpoch.get();
	}

	long worldEpoch(Object key) {
		AtomicLong epoch = worldEpochs.get(key);
		return epoch == null ? 0L : epoch.get();
	}

	void dispatch(RegionJob job) {
		if (!job.alive()) {
			release(job);
			return;
		}
		try {
			analysisExecutor.execute(job::runAnalysis);
		} catch (RejectedExecutionException exception) {
			job.cancelInternal();
			release(job);
			LOGGER.warn("Dropped region analysis because the scan executor queue is full");
		}
	}

	void release(ClientJob job) {
		if (job instanceof RegionJob) {
			((RegionJob) job).releaseRetention();
		}
	}

	void releaseVolume(long volume) {
		synchronized (lock) {
			retainedVolume -= volume;
			if (retainedVolume < 0) {
				retainedVolume = 0;
			}
		}
	}

	void postClient(RegionJob job, ClientAction action) {
		if (!job.alive()) {
			return;
		}
		TaskManager.getInstance().scheduleClient(() -> {
			if (!job.alive()) {
				return;
			}
			try {
				action.accept(job.world());
			} catch (Throwable throwable) {
				LOGGER.error("Failed to apply scan result", throwable);
			}
		});
	}
}
