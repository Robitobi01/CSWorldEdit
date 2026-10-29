package net.dugged.csworldedit.scan;

public interface ScanContext {

	boolean isCancelled();

	void postClient(ClientAction action);
}
