package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

public class TallAzurePlantBlock extends AzurePlantBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF;

    public TallAzurePlantBlock(BlockBehaviour.Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(HALF, DoubleBlockHalf.LOWER));
    }
    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        DoubleBlockHalf doubleblockhalf = (DoubleBlockHalf)arg.getValue(HALF);
        if (arg2.getAxis() != Direction.Axis.Y || doubleblockhalf == DoubleBlockHalf.LOWER != (arg2 == Direction.UP) || arg3.is(this) && arg3.getValue(HALF) != doubleblockhalf) {
            return doubleblockhalf == DoubleBlockHalf.LOWER && arg2 == Direction.DOWN && !arg.canSurvive(arg4, arg5) ? Blocks.AIR.defaultBlockState() : super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        BlockPos blockpos = arg.getClickedPos();
        Level level = arg.getLevel();
        return blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(arg) ? super.getStateForPlacement(arg) : null;
    }

    public void setPlacedBy(Level arg, BlockPos arg2, BlockState arg3, LivingEntity arg4, ItemStack arg5) {
        BlockPos blockpos = arg2.above();
        arg.setBlock(blockpos, copyWaterloggedFrom(arg, blockpos, (BlockState)this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER)), 3);
    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        if (arg.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return super.canSurvive(arg, arg2, arg3);
        } else {
            BlockState blockstate = arg2.getBlockState(arg3.below());
            if (arg.getBlock() != this) {
                return super.canSurvive(arg, arg2, arg3);
            } else {
                return blockstate.is(this) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER;
            }
        }
    }

    public static void placeAt(LevelAccessor arg, BlockState arg2, BlockPos arg3, int i) {
        BlockPos blockpos = arg3.above();
        arg.setBlock(arg3, copyWaterloggedFrom(arg, arg3, (BlockState)arg2.setValue(HALF, DoubleBlockHalf.LOWER)), i);
        arg.setBlock(blockpos, copyWaterloggedFrom(arg, blockpos, (BlockState)arg2.setValue(HALF, DoubleBlockHalf.UPPER)), i);
    }

    public static BlockState copyWaterloggedFrom(LevelReader arg, BlockPos arg2, BlockState arg3) {
        return arg3.hasProperty(BlockStateProperties.WATERLOGGED) ? (BlockState)arg3.setValue(BlockStateProperties.WATERLOGGED, arg.isWaterAt(arg2)) : arg3;
    }

    public void playerWillDestroy(Level arg, BlockPos arg2, BlockState arg3, Player arg4) {
        if (!arg.isClientSide) {
            if (arg4.isCreative()) {
                preventCreativeDropFromBottomPart(arg, arg2, arg3, arg4);
            } else {
                dropResources(arg3, arg, arg2, (BlockEntity)null, arg4, arg4.getMainHandItem());
            }
        }

        super.playerWillDestroy(arg, arg2, arg3, arg4);
    }

    public void playerDestroy(Level arg, Player arg2, BlockPos arg3, BlockState arg4, @Nullable BlockEntity arg5, ItemStack arg6) {
        super.playerDestroy(arg, arg2, arg3, Blocks.AIR.defaultBlockState(), arg5, arg6);
    }

    protected static void preventCreativeDropFromBottomPart(Level arg, BlockPos arg2, BlockState arg3, Player arg4) {
        DoubleBlockHalf doubleblockhalf = (DoubleBlockHalf)arg3.getValue(HALF);
        if (doubleblockhalf == DoubleBlockHalf.UPPER) {
            BlockPos blockpos = arg2.below();
            BlockState blockstate = arg.getBlockState(blockpos);
            if (blockstate.is(arg3.getBlock()) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER) {
                BlockState blockstate1 = blockstate.getFluidState().is(Fluids.WATER) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
                arg.setBlock(blockpos, blockstate1, 35);
                arg.levelEvent(arg4, 2001, blockpos, Block.getId(blockstate));
            }
        }

    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{HALF});
    }

    public long getSeed(BlockState arg, BlockPos arg2) {
        return Mth.getSeed(arg2.getX(), arg2.below(arg.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), arg2.getZ());
    }

    static {
        HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    }
}
