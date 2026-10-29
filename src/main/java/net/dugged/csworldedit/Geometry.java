package net.dugged.csworldedit;

public final class Geometry {

	private Geometry() {
	}

	public static int sphereYMax(int centerY, int worldHeight, double radius) {
		return Math.max(0, (int) Math.min(radius, worldHeight - 1 - centerY));
	}

	public static int cylinderHeight(int centerY, int worldHeight, int requestedHeight) {
		return Math.max(1, Math.min(requestedHeight, worldHeight - centerY));
	}

	public static boolean isDistinctCylinderMirror(int x) {
		return x != 0;
	}

	public static int stackEnd(int origin, int width) {
		return origin + Math.max(1, width) - 1;
	}

	public static int[][] hollowFaces(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
									   int thicknessX, int thicknessY, int thicknessZ) {
		int tx = Math.max(0, Math.min(thicknessX, maxX - minX));
		int ty = Math.max(0, Math.min(thicknessY, maxY - minY));
		int tz = Math.max(0, Math.min(thicknessZ, maxZ - minZ));
		return new int[][]{
				{minX, minY, minZ, maxX, maxY, minZ + tz},
				{minX, minY, maxZ - tz, maxX, maxY, maxZ},
				{minX, minY, minZ, minX + tx, maxY, maxZ},
				{maxX - tx, minY, minZ, maxX, maxY, maxZ},
				{minX, minY, minZ, maxX, minY + ty, maxZ},
				{minX, maxY - ty, minZ, maxX, maxY, maxZ}
		};
	}
}
