package net.dugged.csworldedit.api;

/**
 * Versioned entry point for client-side paste operations.
 */
public final class PasteApi {
	public static final int VERSION = 1;
	public static final String SCHEMA = "csworldedit.paste.v1";
	public static final String IMC_KEY = "csworldedit_paste";
	public static final String TARGET_MOD = "duggedschematica";
	public static final int MAX_BATCH_CHANGES = 4096;
	public static final int MAX_QUEUED_CHANGES = 131072;
	public static final int MAX_SESSIONS = 4;

	private PasteApi() {
	}

	public static PasteService service() {
		return PasteApiImpl.INSTANCE;
	}
}
