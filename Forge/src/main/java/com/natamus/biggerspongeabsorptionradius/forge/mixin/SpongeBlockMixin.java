package com.natamus.biggerspongeabsorptionradius.forge.mixin;

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
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;

@Mixin(value = SpongeBlock.class, priority = 999)
public class SpongeBlockMixin extends Block {
	private static final List<MapColor> spongematerials = Arrays.asList(MapColor.COLOR_YELLOW);

	public SpongeBlockMixin(Properties p_49795_) {
		super(p_49795_);
	}

	/**
	 * @author Rick South
	 * @reason Unable to accomplish with a simple injection.
	 */
	@Overwrite
	private boolean removeWaterBreadthFirstSearch(Level level, BlockPos pos) {
		List<BlockPos> spongepositions = BlockPosFunctions.getBlocksNextToEachOtherMaterial(level, pos, spongematerials);
		int spongecount = spongepositions.size();

		int absorpdistance = 6 * spongecount; // default 6
		int maxcount = 64 * spongecount; // default 64

		Queue<Tuple<BlockPos, Integer>> queue = Lists.newLinkedList();
		queue.add(new Tuple<>(pos, 0));
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
						if (j < absorpdistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
					else if (block instanceof LiquidBlock) {
						level.setBlock(blockpos1, Blocks.AIR.defaultBlockState(), 3);
						++i;
						if (j < absorpdistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
					else if (blockstate.is(Blocks.KELP) || blockstate.is(Blocks.KELP_PLANT) || blockstate.is(Blocks.SEAGRASS) || blockstate.is(Blocks.TALL_SEAGRASS)) {
						BlockEntity tileentity = blockstate.hasBlockEntity() ? level.getBlockEntity(blockpos1) : null;
						dropResources(blockstate, level, blockpos1, tileentity);
						level.setBlock(blockpos1, Blocks.AIR.defaultBlockState(), 3);
						++i;
						if (j < absorpdistance) {
							queue.add(new Tuple<>(blockpos1, j + 1));
						}
					}
				}
			}

			if (i > maxcount) {
				break;
			}
		}

		if (i > 0) {
			for (BlockPos spongepos : spongepositions) {
				Block block = level.getBlockState(spongepos).getBlock();
				if (block instanceof SpongeBlock || block instanceof WetSpongeBlock) {
					level.setBlockAndUpdate(spongepos, Blocks.WET_SPONGE.defaultBlockState());
				}
			}
			return true;
		}
		return false;
	}
}
