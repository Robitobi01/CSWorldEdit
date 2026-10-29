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
		@Config.Comment("Maximum operation distance in chunks.")
		public int operationRangeChunks = 6;
	}

	public static final class Direct {
		@Config.Comment("Apply edits directly in singleplayer instead of using packets.")
		public boolean enabled = true;
		@Config.Comment("Direct-edit blocks applied per server tick (minimum 1).")
		public int blocksPerTick = 8192;
		@Config.Comment("Maximum queued direct-edit jobs (minimum 1).")
		public int queueLimit = 512;
	}
}
