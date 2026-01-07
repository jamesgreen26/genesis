package shipwrights.genesis.content.block;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

public class WallZaplightSpindlesBlock extends BaseCoralFanBlock {
    public static final DirectionProperty FACING;
    private static final Map<Direction, VoxelShape> SHAPES;

    public WallZaplightSpindlesBlock(BlockBehaviour.Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(WATERLOGGED, true));
    }

    public VoxelShape getShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
        return (VoxelShape)SHAPES.get(arg.getValue(FACING));
    }

    public BlockState rotate(BlockState arg, Rotation arg2) {
        return (BlockState)arg.setValue(FACING, arg2.rotate((Direction)arg.getValue(FACING)));
    }

    public BlockState mirror(BlockState arg, Mirror arg2) {
        return arg.rotate(arg2.getRotation((Direction)arg.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{FACING, WATERLOGGED});
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        if ((Boolean)arg.getValue(WATERLOGGED)) {
            arg4.scheduleTick(arg5, Fluids.WATER, Fluids.WATER.getTickDelay(arg4));
        }

        return arg2.getOpposite() == arg.getValue(FACING) && !arg.canSurvive(arg4, arg5) ? Blocks.AIR.defaultBlockState() : arg;
    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        Direction direction = (Direction)arg.getValue(FACING);
        BlockPos blockPos = arg3.relative(direction.getOpposite());
        BlockState blockState = arg2.getBlockState(blockPos);
        return blockState.isFaceSturdy(arg2, blockPos, direction);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        BlockState blockState = super.getStateForPlacement(arg);
        LevelReader levelReader = arg.getLevel();
        BlockPos blockPos = arg.getClickedPos();
        Direction[] directions = arg.getNearestLookingDirections();

        for(Direction direction : directions) {
            if (direction.getAxis().isHorizontal()) {
                blockState = (BlockState)blockState.setValue(FACING, direction.getOpposite());
                if (blockState.canSurvive(levelReader, blockPos)) {
                    return blockState;
                }
            }
        }

        return null;
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
        SHAPES = Maps.newEnumMap(ImmutableMap.of(
                Direction.NORTH, Block.box((double)0.0F, (double)2.0F, (double)3.0F, (double)16.0F, (double)14.0F, (double)16.0F),
                Direction.SOUTH, Block.box((double)0.0F, (double)2.0F, (double)0.0F, (double)16.0F, (double)14.0F, (double)13.0F),
                Direction.WEST, Block.box((double)3.0F, (double)2.0F, (double)0.0F, (double)16.0F, (double)14.0F, (double)16.0F),
                Direction.EAST, Block.box((double)0.0F, (double)2.0F, (double)0.0F, (double)13.0F, (double)14.0F, (double)16.0F)));
    }
}