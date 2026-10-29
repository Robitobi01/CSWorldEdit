package net.dugged.csworldedit.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.dugged.csworldedit.sp.SPEditContext;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Chunk.class)
public class MixinChunkSPEdit {

	@WrapOperation(method = "setBlockState", require = 1,
			at = @At(value = "INVOKE", remap = true,
					target = "Lnet/minecraft/block/Block;breakBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V"))
	private void csworldedit$breakBlock(Block block, World world, BlockPos pos, IBlockState state,
										Operation<Void> original) {
		if (!SPEditContext.isEditing()) {
			original.call(block, world, pos, state);
		}
	}

	@WrapOperation(method = "setBlockState", require = 1,
			at = @At(value = "INVOKE", remap = true,
					target = "Lnet/minecraft/block/Block;onBlockAdded(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V"))
	private void csworldedit$blockAdded(Block block, World world, BlockPos pos, IBlockState state,
										Operation<Void> original) {
		if (!SPEditContext.isEditing()) {
			original.call(block, world, pos, state);
		}
	}
}
