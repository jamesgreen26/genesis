package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import shipwrights.genesis.content.particle.GenesisParticles;

public class VerditeCrystalBlock extends AmethystBlock {
    public static final int GROWTH_CHANCE = 10;
    private static final Direction[] DIRECTIONS = Direction.values();

    public VerditeCrystalBlock(BlockBehaviour.Properties arg) {
        super(arg);
    }

    public void randomTick(BlockState arg, ServerLevel level, BlockPos arg3, RandomSource arg4) {
        if (level.dayTime() > 0 && level.dayTime() < 12000) {
            if (arg4.nextInt(GROWTH_CHANCE) == 0) {
                Direction direction = DIRECTIONS[arg4.nextInt(DIRECTIONS.length)];
                BlockPos blockPos = arg3.relative(direction);
                BlockState blockState = level.getBlockState(blockPos);
                Block block = null;
                if (canClusterGrowAtState(blockState)) {
                    block = GenesisBlocks.SMALL_VERDITE_BUD.get();
                } else if (blockState.is(GenesisBlocks.SMALL_VERDITE_BUD.get()) && blockState.getValue(AmethystClusterBlock.FACING) == direction) {
                    block = GenesisBlocks.MEDIUM_VERDITE_BUD.get();
                } else if (blockState.is(GenesisBlocks.MEDIUM_VERDITE_BUD.get()) && blockState.getValue(AmethystClusterBlock.FACING) == direction) {
                    block = GenesisBlocks.LARGE_VERDITE_BUD.get();
                } else if (blockState.is(GenesisBlocks.LARGE_VERDITE_BUD.get()) && blockState.getValue(AmethystClusterBlock.FACING) == direction) {
                    block = GenesisBlocks.VERDITE_CLUSTER.get();
                }

                if (block != null) {
                    BlockState blockState2 = (BlockState) ((BlockState) block.defaultBlockState().setValue(AmethystClusterBlock.FACING, direction)).setValue(AmethystClusterBlock.WATERLOGGED, blockState.getFluidState().getType() == Fluids.WATER);
                    level.setBlockAndUpdate(blockPos, blockState2);
                }

            } else if (arg4.nextInt(GROWTH_CHANCE) == 1) {

                Direction direction = DIRECTIONS[arg4.nextInt(DIRECTIONS.length)];
                BlockPos blockPos = arg3.relative(direction);
                BlockState blockState = level.getBlockState(blockPos);
                Block block = null;
                if (blockState.is(GenesisBlocks.RIFTROCK.get())) {
                    block = GenesisBlocks.WARPSTONE.get();
                    level.playSound((Player) null, blockPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
                } else if (blockState.is(GenesisBlocks.WARPSTONE.get())) {
                    block = GenesisBlocks.VERDITE_ORE.get();
                    level.playSound((Player) null, blockPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
                } else if (blockState.is(GenesisBlocks.VERDITE_ORE.get())) {
                    block = GenesisBlocks.VERDITE_CRYSTAL_BLOCK.get();
                    level.playSound((Player) null, blockPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
                }
                if (block != null) {
                    level.setBlockAndUpdate(blockPos, block.defaultBlockState());
                }
            }
        }
    }

    public void animateTick(BlockState arg, Level arg2, BlockPos arg3, RandomSource arg4) {
        if (arg4.nextInt(6) == 0) {
            spawnParticles(arg2, arg3);
        }

    }

    private static void spawnParticles(Level arg, BlockPos arg2) {
        double d0 = (double)0.5625F;
        RandomSource randomsource = arg.random;

        for(Direction direction : Direction.values()) {
            BlockPos blockpos = arg2.relative(direction);
            if (!arg.getBlockState(blockpos).isSolidRender(arg, blockpos)) {
                Direction.Axis direction$axis = direction.getAxis();
                double d1 = direction$axis == Direction.Axis.X ? (double)0.5F + (double)0.5625F * (double)direction.getStepX() : (double)randomsource.nextFloat();
                double d2 = direction$axis == Direction.Axis.Y ? (double)0.5F + (double)0.5625F * (double)direction.getStepY() : (double)randomsource.nextFloat();
                double d3 = direction$axis == Direction.Axis.Z ? (double)0.5F + (double)0.5625F * (double)direction.getStepZ() : (double)randomsource.nextFloat();
                arg.addParticle(GenesisParticles.VERDITE_PARTICLES.get(), (double)arg2.getX() + d1, (double)arg2.getY() + d2, (double)arg2.getZ() + d3, (double)0.0F, (double)0.0F, (double)0.0F);
            }
        }

    }


    public static boolean canClusterGrowAtState(BlockState arg) {
        return arg.isAir() || arg.is(Blocks.WATER) && arg.getFluidState().getAmount() == 8;
    }
}