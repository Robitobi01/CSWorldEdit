package net.dugged.csworldedit.scan;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public final class BlockSnapshot {

	private final int minX;
	private final int minY;
	private final int minZ;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final IBlockState[] states;
	private final int[] bedColorValues;
	private final int missing;

	private BlockSnapshot(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, IBlockState[] states,
						  int[] bedColorValues) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.sizeZ = sizeZ;
		this.states = states;
		this.bedColorValues = bedColorValues;
		int absent = 0;
		for (IBlockState state : states) {
			if (state == null) {
				absent++;
			}
		}
		this.missing = absent;
	}

	public IBlockState get(int x, int y, int z) {
		if (!contains(x, y, z)) {
			return null;
		}
		return states[index(x, y, z)];
	}

	public IBlockState get(BlockPos pos) {
		return get(pos.getX(), pos.getY(), pos.getZ());
	}

	public int getBedColorValue(int x, int y, int z) {
		if (bedColorValues == null || !contains(x, y, z)) {
			return -1;
		}
		return bedColorValues[index(x, y, z)];
	}

	public boolean hasBedColorMetadata() {
		return bedColorValues != null;
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x < minX + sizeX && y >= minY && y < minY + sizeY && z >= minZ && z < minZ + sizeZ;
	}

	private int index(int x, int y, int z) {
		return ((y - minY) * sizeZ + (z - minZ)) * sizeX + (x - minX);
	}

	public int minX() {
		return minX;
	}

	public int minY() {
		return minY;
	}

	public int minZ() {
		return minZ;
	}

	public int sizeX() {
		return sizeX;
	}

	public int sizeY() {
		return sizeY;
	}

	public int sizeZ() {
		return sizeZ;
	}

	public long volume() {
		return (long) sizeX * sizeY * sizeZ;
	}

	public int missingCount() {
		return missing;
	}

	public long estimatedHeapBytes() {
		long bytes = 24L + 8L * states.length;
		if (bedColorValues != null) {
			bytes += 24L + 4L * bedColorValues.length;
		}
		return bytes;
	}

	public static long volumeOf(int sizeX, int sizeY, int sizeZ) {
		return (long) sizeX * sizeY * sizeZ;
	}

	public static final class Builder {

		private final int minX;
		private final int minY;
		private final int minZ;
		private final int sizeX;
		private final int sizeY;
		private final int sizeZ;
		private final IBlockState[] states;
		private final int[] bedColorValues;

		public Builder(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
			this(minX, minY, minZ, sizeX, sizeY, sizeZ, false);
		}

		public Builder(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, boolean captureBedColors) {
			this.minX = minX;
			this.minY = minY;
			this.minZ = minZ;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
			this.states = new IBlockState[sizeX * sizeY * sizeZ];
			if (captureBedColors) {
				this.bedColorValues = new int[sizeX * sizeY * sizeZ];
				Arrays.fill(this.bedColorValues, -1);
			} else {
				this.bedColorValues = null;
			}
		}

		public int sizeX() {
			return sizeX;
		}

		public int sizeY() {
			return sizeY;
		}

		public int sizeZ() {
			return sizeZ;
		}

		public int minX() {
			return minX;
		}

		public int minY() {
			return minY;
		}

		public int minZ() {
			return minZ;
		}

		public int length() {
			return states.length;
		}

		public void setIndex(int index, IBlockState state) {
			states[index] = state;
		}

		public void setBedColorValue(int index, int colorValue) {
			if (bedColorValues != null) {
				bedColorValues[index] = colorValue;
			}
		}

		public BlockSnapshot build() {
			return new BlockSnapshot(minX, minY, minZ, sizeX, sizeY, sizeZ, states, bedColorValues);
		}
	}
}
