package net.dugged.csworldedit.tasks;

import net.dugged.csworldedit.CSWorldEdit;
import net.dugged.csworldedit.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class TaskRenderer {

	public static void renderTasks(float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if (player == null || mc.world == null) {
			return;
		}
		List<TaskChunk> tasks = TaskManager.getInstance().getTasks();
		if (tasks.isEmpty()) {
			return;
		}
		float pulse = 0.6F + 0.4F * (float) Math.sin(CSWorldEdit.tickCounter * 0.5F);
		final double d1 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		final double d2 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		final double d3 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.glLineWidth(3F);
		GlStateManager.depthFunc(GL11.GL_ALWAYS);
		GlStateManager.depthMask(false);
		try {
			for (TaskChunk task : tasks) {
				for (ChunkPos cpDest : new ArrayList<>(task.getPendingChunks())) {
					AxisAlignedBB bb = new AxisAlignedBB(cpDest.getXStart(), 0, cpDest.getZStart(),
							cpDest.getXEnd() + 1, mc.world.getHeight(), cpDest.getZEnd() + 1);
					AxisAlignedBB offsetBB = bb.offset(-d1, -d2, -d3);
					Renderer.renderOutline(offsetBB, 1F, 0F, 0F, pulse);
				}

				if (task instanceof TaskClone && !task.isComplete()) {
					AxisAlignedBB offsetBB = ((TaskClone) task).getSourceRegion().offset(-d1, -d2, -d3);
					Renderer.renderOutline(offsetBB, 1F, 1F, 0F, 1F);
				}
			}
		} finally {
			GlStateManager.depthFunc(GL11.GL_LEQUAL);
			GlStateManager.depthMask(true);
			GlStateManager.disableBlend();
			GlStateManager.enableCull();
			GlStateManager.disableLighting();
			GlStateManager.enableTexture2D();
			GlStateManager.glLineWidth(1F);
		}
	}
}
