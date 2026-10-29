package net.dugged.csworldedit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.util.Map;

import static net.dugged.csworldedit.Selection.Position.A;
import static net.dugged.csworldedit.Selection.Position.B;

public class Renderer {

	public static void render(float partialTicks) {
		final EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null || Minecraft.getMinecraft().world == null) {
			return;
		}
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
			for (Map.Entry<Selection.SelectionType, Selection> entry : Manager.selections.entrySet()) {
				Selection.SelectionType type = entry.getKey();
				Selection selection = entry.getValue();
				if (selection.getPos(A) != null || selection.getPos(B) != null) {
					GlStateManager.depthFunc(GL11.GL_LEQUAL);
					if (selection.isCompleted()) {
						AxisAlignedBB posBB = selection.getBB().offset(-d1, -d2, -d3).expand(1, 1, 1).grow(0.0075F);
						RenderGlobal.drawSelectionBoundingBox(posBB, type.getColor().getRed() / 255.0F,
								type.getColor().getGreen() / 255.0F, type.getColor().getBlue() / 255.0F, 0.6F);
					}
					GlStateManager.depthFunc(GL11.GL_ALWAYS);
					if (selection.getPos(A) != null) {
						AxisAlignedBB posBB = new AxisAlignedBB(selection.getPos(A)).offset(-d1, -d2, -d3);
						RenderGlobal.drawSelectionBoundingBox(posBB, A.getColor().getRed() / 255.0F,
								A.getColor().getGreen() / 255.0F, A.getColor().getBlue() / 255.0F, 0.6F);
					}
					if (selection.getPos(B) != null) {
						AxisAlignedBB posBB = new AxisAlignedBB(selection.getPos(B)).offset(-d1, -d2, -d3);
						RenderGlobal.drawSelectionBoundingBox(posBB, B.getColor().getRed() / 255.0F,
								B.getColor().getGreen() / 255.0F, B.getColor().getBlue() / 255.0F, 0.6F);
					}
					GlStateManager.depthFunc(GL11.GL_LEQUAL);
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

	public static void renderOutline(AxisAlignedBB box, float red, float green, float blue, float alpha) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		try {
			buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
			buffer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.maxX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.maxX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.minX, box.minY, box.maxZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.minX, box.minY, box.minZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.maxX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.maxX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.minX, box.maxY, box.maxZ).color(red, green, blue, alpha).endVertex();
			buffer.pos(box.minX, box.maxY, box.minZ).color(red, green, blue, alpha).endVertex();
			tessellator.draw();
		} catch (RuntimeException e) {
			buffer.reset();
			throw e;
		}
	}
}
