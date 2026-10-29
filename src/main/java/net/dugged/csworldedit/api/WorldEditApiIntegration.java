package net.dugged.csworldedit.api;

import net.minecraftforge.fml.common.event.FMLInterModComms;

public final class WorldEditApiIntegration {
	private WorldEditApiIntegration() {
	}

	public static void register() {
		FMLInterModComms.sendFunctionMessage(PasteApi.TARGET_MOD, PasteApi.IMC_KEY,
				WorldEditApiFunction.class.getName());
	}
}
