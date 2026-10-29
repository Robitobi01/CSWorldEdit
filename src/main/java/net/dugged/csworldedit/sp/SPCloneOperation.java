package net.dugged.csworldedit.sp;

import net.dugged.csworldedit.UndoSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class SPCloneOperation implements SPOperation {
	private final int sourceMinX;
	private final int sourceMinY;
	private final int sourceMinZ;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final int volume;
	private final BlockPos destination;
	private final Mode mode;
	private final Set<ChunkPos> chunks;
	private final IBlockState[] sourceStates;
	private final boolean[] copy;
	private final Map<Integer, NBTTagCompound> sourceNbt = new HashMap<>();
	private final long total;
	private int prepareIndex;
	private int copyIndex;
	private int clearIndex;
	private long processed;

	public SPCloneOperation(BlockPos source1, BlockPos source2, BlockPos destination, Mode mode) {
		BlockPos min = new BlockPos(Math.min(source1.getX(), source2.getX()), Math.min(source1.getY(), source2.getY()),
				Math.min(source1.getZ(), source2.getZ()));
		BlockPos max = new BlockPos(Math.max(source1.getX(), source2.getX()), Math.max(source1.getY(), source2.getY()),
				Math.max(source1.getZ(), source2.getZ()));
		this.sourceMinX = min.getX();
		this.sourceMinY = min.getY();
		this.sourceMinZ = min.getZ();
		this.sizeX = max.getX() - min.getX() + 1;
		this.sizeY = max.getY() - min.getY() + 1;
		this.sizeZ = max.getZ() - min.getZ() + 1;
		this.volume = Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
		this.destination = destination;
		this.mode = mode;
		BlockPos destinationMax = destination.add(sizeX - 1, sizeY - 1, sizeZ - 1);
		this.chunks = SPChunks.region(destination, destinationMax);
		this.sourceStates = new IBlockState[volume];
		this.copy = new boolean[volume];
		this.total = (long) volume * (mode == Mode.MOVE ? 2 : 1);
	}

	@Override
	public long total() {
		return total;
	}

	@Override
	public long processed() {
		return processed;
	}

	@Override
	public Set<ChunkPos> chunks() {
		return chunks;
	}

	@Override
	public int apply(SPEditWorld world, int budget, UndoSnapshot undo, boolean recordUndo) {
		int used = 0;
		while (used < budget && prepareIndex < volume) {
			int index = prepareIndex;
			BlockPos source = sourcePos(index);
			IBlockState state = world.getBlockState(source);
			sourceStates[index] = state;
			copy[index] = mode != Mode.MASKED || state.getBlock() != Blocks.AIR;
			if (copy[index] && state.getBlock().hasTileEntity(state)) {
				NBTTagCompound nbt = world.getTileEntityNbt(source);
				if (nbt != null) {
					sourceNbt.put(index, nbt);
				}
			}
			prepareIndex++;
			used++;
		}
		while (used < budget && copyIndex < volume) {
			if (copy[copyIndex]) {
				BlockPos destinationPos = destinationPos(copyIndex);
				SPBlockEditor.setBlock(world, destinationPos, sourceStates[copyIndex], undo, recordUndo);
				NBTTagCompound nbt = sourceNbt.remove(copyIndex);
				if (nbt != null) {
					world.setTileEntityNbt(destinationPos, nbt);
				}
			}
			processed++;
			copyIndex++;
			used++;
		}
		if (mode == Mode.MOVE) {
			while (used < budget && clearIndex < volume) {
				if (copy[clearIndex]) {
					SPBlockEditor.setBlock(world, sourcePos(clearIndex), Blocks.AIR.getDefaultState(), undo,
							recordUndo);
				}
				processed++;
				clearIndex++;
				used++;
			}
		}
		return used;
	}

	@Override
	public boolean isDone() {
		return prepareIndex >= volume && copyIndex >= volume && (mode != Mode.MOVE || clearIndex >= volume);
	}

	private BlockPos sourcePos(int index) {
		int x = index % sizeX;
		int z = (index / sizeX) % sizeZ;
		int y = index / (sizeX * sizeZ);
		return new BlockPos(sourceMinX + x, sourceMinY + y, sourceMinZ + z);
	}

	private BlockPos destinationPos(int index) {
		int x = index % sizeX;
		int z = (index / sizeX) % sizeZ;
		int y = index / (sizeX * sizeZ);
		return destination.add(x, y, z);
	}

	public enum Mode {
		FORCE, MASKED, MOVE
	}
}
