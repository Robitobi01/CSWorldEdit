package net.dugged.csworldedit.api;

/**
 * Creates bounded paste sessions. Calls run on the Minecraft client thread.
 */
public interface PasteService {
	boolean isAvailable();

	PasteSession begin(PasteOptions options);

	PasteSession session(String sessionId);

	PasteProgress progress(String sessionId);

	boolean cancel(String sessionId, CancelReason reason);
}
