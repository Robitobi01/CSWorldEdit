package net.dugged.csworldedit.scan;

import net.dugged.csworldedit.Manager;
import net.dugged.csworldedit.Selection;
import net.minecraft.util.text.TextComponentTranslation;

public final class ScanFeedback {

	private ScanFeedback() {
	}

	public static void report(SnapshotManager.SubmitResult result, Selection selection) {
		if (result == SnapshotManager.SubmitResult.ACCEPTED || result == SnapshotManager.SubmitResult.INVALID) {
			return;
		}
		if (result == SnapshotManager.SubmitResult.TOO_LARGE) {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.scan.tooLarge",
					selection == null ? 0L : selection.volume(), SnapshotManager.MAX_SNAPSHOT_VOLUME));
		} else {
			Manager.sendMessage(new TextComponentTranslation("text.worldedit.scan.queueFull"));
		}
	}

	public static void report(SnapshotManager.SubmitResult result) {
		report(result, null);
	}
}
