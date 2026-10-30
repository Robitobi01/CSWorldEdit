package net.dugged.csworldedit;

import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, category = "")
public final class Configuration {
	public static final General general = new General();
	public static final Direct direct = new Direct();

	private Configuration() {
	}

	public static final class General {
		@Config.Comment("Enable CSWorldEdit client commands and overlays.")
		public boolean enabled = false;
		@Config.Comment("Maximum distance in chunks from the player at which operations may run.")
		public int operationRangeChunks = 6;
	}

	public static final class Direct {
		@Config.Comment("Apply edits through the integrated server in local singleplayer instead of sending commands. Requires creative or spectator mode and is disabled on published LAN worlds.")
		public boolean enabled = true;
		@Config.Comment("Maximum blocks applied per server tick by the direct backend (values below 1 are treated as 1).")
		public int blocksPerTick = 8192;
		@Config.Comment("Maximum direct jobs queued before new jobs wait (values below 1 are treated as 1).")
		public int queueLimit = 512;
	}
}
