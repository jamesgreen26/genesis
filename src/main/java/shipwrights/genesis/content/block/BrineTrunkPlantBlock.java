//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import shipwrights.genesis.content.GenesisTags;

public class BrineTrunkPlantBlock extends PipeBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED;

    public BrineTrunkPlantBlock(BlockBehaviour.Properties arg) {
        super(0.3125F, arg);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false).setValue(WATERLOGGED, false));
    }

    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        return this.getStateForPlacement(arg.getLevel(), arg.getClickedPos());
    }

    public BlockState getStateForPlacement(BlockGetter arg, BlockPos arg2) {
        FluidState fluidState = arg.getFluidState(arg2);
        BlockState blockstate = arg.getBlockState(arg2.below());
        BlockState blockstate1 = arg.getBlockState(arg2.above());
        BlockState blockstate2 = arg.getBlockState(arg2.north());
        BlockState blockstate3 = arg.getBlockState(arg2.east());
        BlockState blockstate4 = arg.getBlockState(arg2.south());
        BlockState blockstate5 = arg.getBlockState(arg2.west());
        return this.defaultBlockState().setValue(DOWN, blockstate.is(this) || blockstate.is(GenesisBlocks.BRINE_FLOWER.get()) || blockstate.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE))
        .setValue(UP, blockstate1.is(this) || blockstate1.is(GenesisBlocks.BRINE_FLOWER.get()))
        .setValue(NORTH, blockstate2.is(this) || blockstate2.is(GenesisBlocks.BRINE_FLOWER.get()))
        .setValue(EAST, blockstate3.is(this) || blockstate3.is(GenesisBlocks.BRINE_FLOWER.get()))
        .setValue(SOUTH, blockstate4.is(this) || blockstate4.is(GenesisBlocks.BRINE_FLOWER.get()))
        .setValue(WEST, blockstate5.is(this) || blockstate5.is(GenesisBlocks.BRINE_FLOWER.get()))
        .setValue(WATERLOGGED, fluidState.is(FluidTags.WATER) && fluidState.getAmount() == 8);
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        if (!arg.canSurvive(arg4, arg5)) {
            arg4.scheduleTick(arg5, this, 1);
            return super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
        } else {
            boolean flag = arg3.is(this) || arg3.is(GenesisBlocks.BRINE_FLOWER.get()) || arg2 == Direction.DOWN && arg3.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE);
            return arg.setValue((Property)PROPERTY_BY_DIRECTION.get(arg2), flag);
        }
    }

    public void tick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        if (!arg.canSurvive(arg2, arg3)) {
            arg2.destroyBlock(arg3, true);
        }

    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        BlockState blockstate = arg2.getBlockState(arg3.below());
        boolean flag = !arg2.getBlockState(arg3.above()).isAir() && !blockstate.isAir();

        for(Direction direction : Plane.HORIZONTAL) {
            BlockPos blockpos = arg3.relative(direction);
            BlockState blockstate1 = arg2.getBlockState(blockpos);
            if (blockstate1.is(this)) {
                if (flag) {
                    return false;
                }

                BlockState blockstate2 = arg2.getBlockState(blockpos.below());
                if (blockstate2.is(this) || blockstate2.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE)) {
                    return true;
                }
            }
        }

        return blockstate.is(this) || blockstate.is(GenesisTags.Blocks.BRINE_TRUNK_PLANTABLE);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED});
    }

    public boolean isPathfindable(BlockState arg, BlockGetter arg2, BlockPos arg3, PathComputationType arg4) {
        return false;
    }

    public FluidState getFluidState(BlockState arg) {
        return (Boolean)arg.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(arg);
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
    }
}