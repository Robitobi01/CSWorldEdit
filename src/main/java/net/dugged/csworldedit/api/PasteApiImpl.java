package net.dugged.csworldedit.api;

import net.dugged.csworldedit.tasks.TaskManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class PasteApiImpl implements PasteService {
	static final PasteApiImpl INSTANCE = new PasteApiImpl();

	private final Map<String, PasteSessionImpl> sessions = new LinkedHashMap<>();

	private PasteApiImpl() {
	}

	@Override
	public boolean isAvailable() {
		Minecraft mc = Minecraft.getMinecraft();
		return mc != null && mc.world != null && mc.player != null;
	}

	@Override
	public synchronized PasteSession begin(PasteOptions options) {
		requireClientThread();
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.world == null || mc.player == null) {
			throw new IllegalStateException("No client world");
		}
		if (sessions.size() >= PasteApi.MAX_SESSIONS) {
			evictFinished();
			if (sessions.size() >= PasteApi.MAX_SESSIONS) {
				return null;
			}
		}
		PasteSessionImpl session = new PasteSessionImpl(mc.world.provider.getDimension(),
				TaskManager.getInstance().getGeneration(), options == null ? PasteOptions.DEFAULT : options);
		sessions.put(session.id(), session);
		return session;
	}

	@Override
	public synchronized PasteSession session(String sessionId) {
		return sessions.get(sessionId);
	}

	@Override
	public synchronized PasteProgress progress(String sessionId) {
		PasteSessionImpl session = sessions.get(sessionId);
		return session == null ? null : session.progress();
	}

	@Override
	public synchronized boolean cancel(String sessionId, CancelReason reason) {
		requireClientThread();
		PasteSessionImpl session = sessions.get(sessionId);
		return session != null && session.cancel(reason);
	}

	synchronized void tick() {
		long current = TaskManager.getInstance().getGeneration();
		for (PasteSessionImpl session : new ArrayList<>(sessions.values())) {
			if (session.generation() != current) {
				session.cancel(CancelReason.CANCELLED_EXTERNALLY);
			} else {
				session.tick();
			}
		}
	}

	synchronized void worldChanged() {
		cancelAll(CancelReason.WORLD_CHANGED);
	}

	private void cancelAll(CancelReason reason) {
		for (PasteSessionImpl session : sessions.values()) {
			session.cancel(reason);
		}
	}

	private void evictFinished() {
		Iterator<Map.Entry<String, PasteSessionImpl>> iterator = sessions.entrySet().iterator();
		while (iterator.hasNext()) {
			PasteSessionImpl session = iterator.next().getValue();
			if (session.state() == PasteState.COMPLETED || session.state() == PasteState.CANCELLED
					|| session.state() == PasteState.FAILED) {
				iterator.remove();
				return;
			}
		}
	}

	static void requireClientThread() {
		if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
			throw new IllegalStateException("Paste API calls must run on the Minecraft client thread");
		}
	}
}
