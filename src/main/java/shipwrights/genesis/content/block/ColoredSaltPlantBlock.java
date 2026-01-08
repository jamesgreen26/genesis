package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import shipwrights.genesis.content.GenesisTags;


public class ColoredSaltPlantBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED;
    private static final VoxelShape BOX_COLLIDER;
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 4);;

    public ColoredSaltPlantBlock(BlockBehaviour.Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false).setValue(COLOR, 0));
    }

    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        return this.getStateForPlacement(arg.getLevel(), arg.getClickedPos());
    }

    public BlockState getStateForPlacement(BlockGetter arg, BlockPos arg2) {
        FluidState fluidState = arg.getFluidState(arg2);
        BlockState below_blockstate = arg.getBlockState(arg2.below());
        if (below_blockstate.is(GenesisBlocks.SALT.get()) || below_blockstate.is(GenesisBlocks.CRACKED_SALT.get())) {
            return this.defaultBlockState().setValue(COLOR, 0);
        } else if (below_blockstate.is(GenesisBlocks.PALE_RED_SALT.get()) || below_blockstate.is(GenesisBlocks.CRACKED_PALE_RED_SALT.get())) {
            return this.defaultBlockState().setValue(COLOR, 1);
        } else if (below_blockstate.is(GenesisBlocks.RED_SALT.get()) || below_blockstate.is(GenesisBlocks.CRACKED_RED_SALT.get())) {
            return this.defaultBlockState().setValue(COLOR, 2);
        } else if (below_blockstate.is(GenesisBlocks.TURQUOISE_SALT.get()) || below_blockstate.is(GenesisBlocks.CRACKED_TURQUOISE_SALT.get())) {
            return this.defaultBlockState().setValue(COLOR, 3);
        } else if (below_blockstate.is(GenesisBlocks.CYAN_SALT.get()) || below_blockstate.is(GenesisBlocks.CRACKED_CYAN_SALT.get())) {
            return this.defaultBlockState().setValue(COLOR, 4);
        }
        return this.defaultBlockState().setValue(COLOR, 2);
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        if (!arg.canSurvive(arg4, arg5)) {
            arg4.scheduleTick(arg5, this, 1);
        }
        return super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
    }

    public void tick(BlockState arg, ServerLevel arg2, BlockPos arg3, RandomSource arg4) {
        if (!arg.canSurvive(arg2, arg3)) {
            arg2.destroyBlock(arg3, true);
        }

    }


    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        BlockState below_block = arg2.getBlockState(arg3.below());
        return below_block.is(GenesisTags.Blocks.SALT_PLANTABLE);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{WATERLOGGED, COLOR});
    }

    public FluidState getFluidState(BlockState arg) {
        return (Boolean)arg.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(arg);
    }
    public VoxelShape getShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
        return BOX_COLLIDER;
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        BOX_COLLIDER = Block.box((double)2.0F, (double)0.0F, (double)2.0F, (double)14.0F, (double)14.0F, (double)14.0F);
    }
}
