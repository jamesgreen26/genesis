package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.content.blockentity.VoidCoreBlockEntity;

public class VoidCorePanelBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;

    public VoidCorePanelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ATTACHED, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isCrouching()) {
            facing = facing.getOpposite();
        }
        BlockPos frontPos = context.getClickedPos().relative(facing);

        BlockState frontState = context.getLevel().getBlockState(frontPos);
        if (frontState.isAir()) {
            return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(ATTACHED, false);
        }


        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ATTACHED, true);
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);

        if (!fromPos.equals(pos.relative(facing))) return;

        boolean blocked = !level.getBlockState(fromPos).isAir();

        if (state.getValue(ATTACHED) != blocked) {
            level.setBlock(pos, state.setValue(ATTACHED, blocked), 3);
        }
    }

    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        if (blockState.getValue(ATTACHED)){
            return SQUARE_COLLIDER;
        }

        if (blockState.getValue(FACING) == Direction.NORTH) {
            return NORTH_COLLIDER;
        } else if (blockState.getValue(FACING) == Direction.SOUTH) {
            return SOUTH_COLLIDER;
        } else if (blockState.getValue(FACING) == Direction.EAST) {
            return EAST_COLLIDER;
        } else if (blockState.getValue(FACING) == Direction.WEST) {
            return WEST_COLLIDER;
        } else if (blockState.getValue(FACING) == Direction.UP) {
            return UP_COLLIDER;
        } else if (blockState.getValue(FACING) == Direction.DOWN) {
            return DOWN_COLLIDER;
        }

        return UP_COLLIDER;
    }

    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        VoidCoreBlockEntity.updateVoidCore(pos, level);
    }

    @Override
    public void onRemove(BlockState arg, Level arg2, BlockPos arg3, BlockState arg4, boolean bl) {
        super.onRemove(arg, arg2, arg3, arg4, bl);
        VoidCoreBlockEntity.updateVoidCore(arg3, arg2);
    }

    private static final VoxelShape SQUARE_COLLIDER = Block.box(0.0F,0.0F,0.0F,16.0F,16.0F,16.0F);
    private static final VoxelShape NORTH_COLLIDER = Block.box(0.0F,0.0F,3.0F,16.0F,16.0F,16.0F);
    private static final VoxelShape SOUTH_COLLIDER = Block.box(0.0F,0.0F,0.0F,16.0F,16.0F,13.0F);
    private static final VoxelShape EAST_COLLIDER = Block.box(0.0F,0.0F,0.0F,13.0F,16.0F,16.0F);
    private static final VoxelShape WEST_COLLIDER = Block.box(3.0F,0.0F,0.0F,16.0F,16.0F,16.0F);
    private static final VoxelShape UP_COLLIDER = Block.box(0.0F,0.0F,0.0F,16.0F,13.0F,16.0F);
    private static final VoxelShape DOWN_COLLIDER = Block.box(0.0F,3.0F,0.0F,16.0F,16.0F,16.0F);



}
