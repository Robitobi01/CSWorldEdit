package net.dugged.csworldedit.scan;

import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.MapColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public final class ClientWorldView implements BlockReader, BedColorReader {

	private final World world;
	private Chunk cachedChunk;
	private int cachedChunkX = Integer.MIN_VALUE;
	private int cachedChunkZ = Integer.MIN_VALUE;

	public ClientWorldView(World world) {
		this.world = world;
	}

	@Override
	public IBlockState getBlock(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return null;
		}
		Chunk chunk = chunkAt(x >> 4, z >> 4);
		if (chunk == null) {
			return null;
		}
		return chunk.getBlockState(x, y, z);
	}

	public boolean isLoaded(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return false;
		}
		return chunkAt(x >> 4, z >> 4) != null;
	}

	@Override
	public int getBedColorValue(int x, int y, int z) {
		if (y < 0 || y > 255) {
			return -1;
		}
		Chunk chunk = chunkAt(x >> 4, z >> 4);
		if (chunk == null) {
			return -1;
		}
		TileEntity tileEntity = chunk.getTileEntity(new BlockPos(x, y, z), Chunk.EnumCreateEntityType.CHECK);
		if (tileEntity instanceof TileEntityBed) {
			return MapColor.getBlockColor(((TileEntityBed) tileEntity).getColor()).colorValue;
		}
		return -1;
	}

	private Chunk chunkAt(int chunkX, int chunkZ) {
		if (cachedChunk == null || chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
			cachedChunk = world.getChunkProvider().getLoadedChunk(chunkX, chunkZ);
			cachedChunkX = chunkX;
			cachedChunkZ = chunkZ;
		}
		return cachedChunk;
	}
}
