package net.dugged.csworldedit;

import net.dugged.csworldedit.brushes.BrushBase;
import net.dugged.csworldedit.clientcommands.ClientCommandHandler;
import net.dugged.csworldedit.mixins.IItemSword;
import net.dugged.csworldedit.tasks.TaskClone;
import net.dugged.csworldedit.tasks.TaskFill;
import net.dugged.csworldedit.tasks.TaskManager;
import net.dugged.csworldedit.tasks.TaskRenderer;
import net.dugged.csworldedit.tasks.TaskReplaceItem;
import net.dugged.csworldedit.tasks.TaskSetBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, clientSideOnly = true,
		dependencies = "required-after:cleanroom@[" + Tags.CLEANROOM_VERSION + ",)")
public class CSWorldEdit {

	public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
	private static final Minecraft mc = Minecraft.getMinecraft();
	public static long tickCounter = 0;
	private static long swordCooldown = 0;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		ConfigCleanup.prune(Tags.MOD_ID);
	}

	@Mod.EventHandler
	public void init(final FMLInitializationEvent event) {
		ClientCommandHandler.getInstance().init();
	}

	@SubscribeEvent
	public void onConfigChangedEvent(final ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(Tags.MOD_ID)) {
			ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
			ConfigCleanup.prune(Tags.MOD_ID);
			if (Configuration.general.enabled) {
				ClientCommandHandler.updatePermissions();
			}
		}
	}

	@SubscribeEvent
	public void onClientChat(ClientChatEvent event) {
		if (!Configuration.general.enabled || mc.player == null || mc.world == null) {
			return;
		}
		UndoManager.getInstance().saveCmdUndo(event.getMessage(), mc.world, mc.player);
	}

	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
		if (!event.getWorld().isRemote) {
			return;
		}
		TaskManager.getInstance().resetForWorldChange();
		UndoManager.getInstance().clearHistory();
		Manager.clearAllSelections();
		TaskSetBlock.setblockPermission = false;
		TaskFill.fillPermission = false;
		TaskClone.clonePermission = false;
		TaskReplaceItem.replaceItemPermission = false;
		TaskManager.gamerulePermission = false;
	}

	@SubscribeEvent
	public void onTick(final TickEvent.ClientTickEvent event) {
		if (!Configuration.general.enabled || event.phase != TickEvent.Phase.END) {
			return;
		}
		ClientCommandHandler.getInstance().tick();
		tickCounter++;
		swordCooldown--;
	}

	@SubscribeEvent
	public void render(final RenderWorldLastEvent event) {
		if (!Configuration.general.enabled) {
			return;
		}
		Renderer.render(event.getPartialTicks());
		TaskRenderer.renderTasks(event.getPartialTicks());
	}

	@SubscribeEvent
	public void onLeftClickBlock(final PlayerInteractEvent.LeftClickBlock event) {
		if (!Configuration.general.enabled || mc.player == null || !(event.getEntityPlayer() instanceof EntityPlayerSP)
				|| event.getHand() != EnumHand.MAIN_HAND) {
			return;
		}
		Item itemInHand = mc.player.getHeldItemMainhand().getItem();
		if (mc.player.isCreative() && itemInHand instanceof ItemSword) {
			Item.ToolMaterial material = ((IItemSword) itemInHand).getMaterial();
			if (swordCooldown > tickCounter) {
				event.setCanceled(true);
			} else if (mc.player instanceof EntityPlayerSP) {
				if (Manager.getPos(material, A) == null || !Manager.getPos(material, A).equals(event.getPos())) {
					Manager.setPos(material, A, event.getPos());
				} else {
					Manager.setPos(material, A, null);
				}
				swordCooldown = tickCounter + 10;
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
		if (!Configuration.general.enabled || mc.player == null || !(event.getEntityPlayer() instanceof EntityPlayerSP)
				|| event.getHand() != EnumHand.MAIN_HAND) {
			return;
		}
		Item itemInHand = mc.player.getHeldItemMainhand().getItem();
		if (mc.player.isCreative() && itemInHand instanceof ItemSword) {
			Item.ToolMaterial material = ((IItemSword) itemInHand).getMaterial();
			if (swordCooldown > tickCounter) {
				event.setCanceled(true);
			} else if (mc.player != null) {
				if (Manager.getPos(material, B) == null || !Manager.getPos(material, B).equals(event.getPos())) {
					Manager.setPos(material, B, event.getPos());
				} else {
					Manager.setPos(material, B, null);
				}
				swordCooldown = tickCounter + 10;
				event.setCanceled(true);
			}
		} else if (mc.player.isCreative() && Manager.currentBrushes.containsKey(itemInHand)) {
			BrushBase brush = Manager.currentBrushes.get(itemInHand);
			if (brush.getUseCooldown() <= tickCounter) {
				brush.execute(mc.world, event.getPos());
				brush.setUseCooldown(tickCounter + 2);
			}
			event.setCanceled(true);
		}
	}
}
