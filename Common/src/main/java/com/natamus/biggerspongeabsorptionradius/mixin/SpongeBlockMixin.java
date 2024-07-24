package com.natamus.biggerspongeabsorptionradius.mixin;

import com.google.common.collect.Lists;
import com.natamus.collective.functions.BlockPosFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

@Mixin(value = SpongeBlock.class, priority = 1001)
public class SpongeBlockMixin extends Block {
	@Unique private static final List<MapColor> spongematerials = Arrays.asList(MapColor.COLOR_YELLOW);

	public SpongeBlockMixin(Properties p_49795_) {
		super(p_49795_);
	}

	@Inject(method = "removeWaterBreadthFirstSearch(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", at = @At(value = "HEAD"), cancellable = true)
	private void removeWaterBreadthFirstSearch(Level level, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
		List<BlockPos> spongePositions = BlockPosFunctions.getBlocksNextToEachOtherMaterial(level, blockPos, spongematerials);
		int spongeCount = spongePositions.size();

		int absorbDistance = 6 * spongeCount; // default 6
		int maxCount = 64 * spongeCount; // default 64

		Queue<Tuple<BlockPos, Integer>> queue = Lists.newLinkedList();
		queue.add(new Tuple<>(blockPos, 0));
		int i = 0;

		while(!queue.isEmpty()) {
			Tuple<BlockPos, Integer> tuple = queue.poll();
			BlockPos blockpos = tuple.getA();
			int j = tuple.getB();

			for(Direction direction : Direction.values()) {
				BlockPos blockpos1 = blockpos.relative(direction);
				BlockState blockstate = level.getBlockState(blockpos1);
				Block block = blockstate.getBlock();
				FluidState ifluidstate = level.getFluidState(blockpos1);
				MapColor material = blockstate.getMapColor(level, blockpos1);
				if (ifluidstate.is(FluidTags.WATER) || block instanceof SpongeBlock || block instanceof WetSpongeBlock) {
					if (blockstate.getBlock() instanceof BucketPickup && !((BucketPickup)blockstate.getBlock()).pickupBlock(level, blockpos1, blockstate).isEmpty()) {
						++i;
						if (j < absorbDistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
					else if (block instanceof LiquidBlock) {
						level.setBlock(blockpos1, Blocks.AIR.defaultBlockState(), 3);
						++i;
						if (j < absorbDistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
					else if (blockstate.is(Blocks.KELP) || blockstate.is(Blocks.KELP_PLANT) || blockstate.is(Blocks.SEAGRASS) || blockstate.is(Blocks.TALL_SEAGRASS)) {
						BlockEntity tileentity = blockstate.hasBlockEntity() ? level.getBlockEntity(blockpos1) : null;
						dropResources(blockstate, level, blockpos1, tileentity);
						level.setBlock(blockpos1, Blocks.AIR.defaultBlockState(), 3);
						++i;
						if (j < absorbDistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
				}
			}

			if (i > maxCount) {
				break;
			}
		}

		if (i > 0) {
			for (BlockPos spongepos : spongePositions) {
				Block block = level.getBlockState(spongepos).getBlock();
				if (block instanceof SpongeBlock || block instanceof WetSpongeBlock) {
					level.setBlockAndUpdate(spongepos, Blocks.WET_SPONGE.defaultBlockState());
				}
			}

			cir.setReturnValue(true);
		}

		cir.setReturnValue(false);
	}
}
