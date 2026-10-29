package net.dugged.csworldedit.sp;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public interface SPEditWorld {
	int dimension();

	IBlockState getBlockState(BlockPos pos);

	boolean setBlockState(BlockPos pos, IBlockState state, int flags);

	boolean isOutsideBuildHeight(BlockPos pos);

	void removeTileEntity(BlockPos pos);

	NBTTagCompound getTileEntityNbt(BlockPos pos);

	boolean setTileEntityNbt(BlockPos pos, NBTTagCompound nbt);

	int inventorySize(BlockPos pos);

	ItemStack getStack(BlockPos pos, int slot);

	boolean setStack(BlockPos pos, int slot, ItemStack stack);
}
