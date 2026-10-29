package net.dugged.csworldedit.scan;

import net.minecraft.block.material.MapColor;
import net.minecraft.item.EnumDyeColor;

public final class BedColorMapping {

	private BedColorMapping() {
	}

	public static int closestDyeMetadata(int blockColor) {
		float r = ((blockColor >> 16) & 0xFF) / 255f;
		float g = ((blockColor >> 8) & 0xFF) / 255f;
		float b = (blockColor & 0xFF) / 255f;
		EnumDyeColor closestDye = null;
		float smallestDistance = Float.MAX_VALUE;
		for (EnumDyeColor dye : EnumDyeColor.values()) {
			int dyeColorInt = dye.getColorValue();
			float dyeR = ((dyeColorInt >> 16) & 0xFF) / 255f;
			float dyeG = ((dyeColorInt >> 8) & 0xFF) / 255f;
			float dyeB = (dyeColorInt & 0xFF) / 255f;
			float distance = (r - dyeR) * (r - dyeR) + (g - dyeG) * (g - dyeG) + (b - dyeB) * (b - dyeB);
			if (distance < smallestDistance) {
				smallestDistance = distance;
				closestDye = dye;
			}
		}
		return closestDye.getMetadata();
	}

	public static int bedWoolMetadata(boolean foot, int capturedColorValue) {
		int colorValue = foot && capturedColorValue >= 0 ? capturedColorValue : MapColor.CLOTH.colorValue;
		return closestDyeMetadata(colorValue);
	}
}
