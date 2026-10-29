package net.dugged.csworldedit.api;

/**
 * A begin/submit/commit or begin/submit/cancel paste lifecycle. One session produces one undo group.
 */
public interface PasteSession {
	String id();

	PasteState state();

	SubmitResult submit(Iterable<BlockChange> changes);

	boolean commit();

	boolean cancel(CancelReason reason);

	PasteProgress progress();
}
