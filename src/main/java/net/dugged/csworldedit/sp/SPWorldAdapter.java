package net.dugged.csworldedit.sp;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public final class SPWorldAdapter implements SPEditWorld {
	private final WorldServer world;

	public SPWorldAdapter(WorldServer world) {
		this.world = world;
	}

	@Override
	public int dimension() {
		return world.provider.getDimension();
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		return world.getBlockState(pos);
	}

	@Override
	public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
		return world.setBlockState(pos, state, flags);
	}

	@Override
	public boolean isOutsideBuildHeight(BlockPos pos) {
		return world.isOutsideBuildHeight(pos);
	}

	@Override
	public void removeTileEntity(BlockPos pos) {
		world.removeTileEntity(pos);
	}

	@Override
	public NBTTagCompound getTileEntityNbt(BlockPos pos) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity == null) {
			return null;
		}
		NBTTagCompound nbt = new NBTTagCompound();
		tileEntity.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public boolean setTileEntityNbt(BlockPos pos, NBTTagCompound nbt) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity == null) {
			IBlockState state = world.getBlockState(pos);
			tileEntity = state.getBlock().createTileEntity(world, state);
			if (tileEntity == null) {
				return false;
			}
			tileEntity.setWorld(world);
			tileEntity.setPos(pos);
			world.setTileEntity(pos, tileEntity);
		}
		NBTTagCompound copy = nbt.copy();
		copy.setInteger("x", pos.getX());
		copy.setInteger("y", pos.getY());
		copy.setInteger("z", pos.getZ());
		tileEntity.readFromNBT(copy);
		tileEntity.markDirty();
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 2);
		return true;
	}

	@Override
	public int inventorySize(BlockPos pos) {
		TileEntity tileEntity = world.getTileEntity(pos);
		return tileEntity instanceof IInventory ? ((IInventory) tileEntity).getSizeInventory() : 0;
	}

	@Override
	public ItemStack getStack(BlockPos pos, int slot) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof IInventory)) {
			return ItemStack.EMPTY;
		}
		IInventory inventory = (IInventory) tileEntity;
		if (slot < 0 || slot >= inventory.getSizeInventory()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = inventory.getStackInSlot(slot);
		return stack == null ? ItemStack.EMPTY : stack;
	}

	@Override
	public boolean setStack(BlockPos pos, int slot, ItemStack stack) {
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof IInventory)) {
			return false;
		}
		IInventory inventory = (IInventory) tileEntity;
		if (slot < 0 || slot >= inventory.getSizeInventory()) {
			return false;
		}
		inventory.setInventorySlotContents(slot, stack);
		inventory.markDirty();
		return true;
	}
}
