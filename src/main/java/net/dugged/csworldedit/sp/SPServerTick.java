package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class SPServerTick {
	private SPServerTick() {
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			SPEditBackend.getInstance().serverTick(net.minecraftforge.fml.common.FMLCommonHandler.instance()
					.getMinecraftServerInstance());
		}
	}
}
