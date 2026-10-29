package net.dugged.csworldedit.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Immutable block change with an optional, defensively copied tile-entity payload.
 */
public final class BlockChange {
	private final BlockPos pos;
	private final IBlockState state;
	private final NBTTagCompound tileEntity;

	public BlockChange(BlockPos pos, IBlockState state) {
		this(pos, state, null);
	}

	public BlockChange(BlockPos pos, IBlockState state, NBTTagCompound tileEntity) {
		this.pos = new BlockPos(Objects.requireNonNull(pos, "pos"));
		this.state = Objects.requireNonNull(state, "state");
		this.tileEntity = tileEntity == null ? null : tileEntity.copy();
	}

	public BlockPos pos() {
		return pos;
	}

	public IBlockState state() {
		return state;
	}

	public NBTTagCompound tileEntityNbt() {
		return tileEntity == null ? null : tileEntity.copy();
	}

	NBTTagCompound rawTileEntityNbt() {
		return tileEntity;
	}
}
