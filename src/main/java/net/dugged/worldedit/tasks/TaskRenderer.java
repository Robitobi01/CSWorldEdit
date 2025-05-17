package net.dugged.worldedit.tasks;

import net.dugged.worldedit.WorldEdit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;

public class TaskRenderer {

	public static void renderTasks(float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if (player == null || mc.world == null) {
			return;
		}
		float pulse = 0.6F + 0.4F * (float) Math.sin(WorldEdit.tickCounter * 0.5F);
		final double d1 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		final double d2 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		final double d3 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.glLineWidth(15F);
		GlStateManager.depthFunc(GL11.GL_ALWAYS);

		for (TaskChunk task : TaskManager.getInstance().getTasks()) {
			for (ChunkPos cpDest : task.getPendingChunks()) {
				AxisAlignedBB bb = new AxisAlignedBB(cpDest.getXStart(), 0, cpDest.getZStart(), cpDest.getXEnd() + 1, mc.world.getHeight(), cpDest.getZEnd() + 1);
				AxisAlignedBB offsetBB = bb.offset(-d1, -d2, -d3);
				renderGlowingOutline(offsetBB, 1F, 0F, 0F, pulse);
			}

			if (task instanceof TaskClone && !task.isComplete()) {
				AxisAlignedBB offsetBB = ((TaskClone) task).getSourceRegion().offset(-d1, -d2, -d3);
				renderGlowingOutline(offsetBB, 1F, 1F, 0F, 1);
			}
		}

		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.glLineWidth(1F);
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
	}

	private static void renderGlowingOutline(AxisAlignedBB box, float r, float g, float b, float pulse) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();
		GlStateManager.pushMatrix();
		buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, pulse).endVertex();
		tess.draw();
		buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, pulse).endVertex();
		buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, pulse).endVertex();
		tess.draw();
		GlStateManager.popMatrix();
	}
}