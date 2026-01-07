package shipwrights.genesis.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import shipwrights.genesis.content.particle.GenesisParticles;

import javax.annotation.Nullable;

public class ZaplightSpindlesBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED;
    private static final VoxelShape AABB;

    public ZaplightSpindlesBlock(BlockBehaviour.Properties arg) {
        super(arg);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, true));
    }

    protected void tryScheduleDieTick(BlockState arg, LevelAccessor arg2, BlockPos arg3) {
        if (!scanForWater(arg, arg2, arg3)) {
            arg2.scheduleTick(arg3, this, 60 + arg2.getRandom().nextInt(40));
        }

    }

    protected static boolean scanForWater(BlockState arg, BlockGetter arg2, BlockPos arg3) {
        if ((Boolean)arg.getValue(WATERLOGGED)) {
            return true;
        } else {
            for(Direction direction : Direction.values()) {
                if (arg2.getFluidState(arg3.relative(direction)).is(FluidTags.WATER)) {
                    return true;
                }
            }

            return false;
        }
    }



    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext arg) {
        FluidState fluidState = arg.getLevel().getFluidState(arg.getClickedPos());
        return (BlockState)this.defaultBlockState().setValue(WATERLOGGED, fluidState.is(FluidTags.WATER) && fluidState.getAmount() == 8);
    }

    public VoxelShape getShape(BlockState arg, BlockGetter arg2, BlockPos arg3, CollisionContext arg4) {
        return AABB;
    }

    public BlockState updateShape(BlockState arg, Direction arg2, BlockState arg3, LevelAccessor arg4, BlockPos arg5, BlockPos arg6) {
        if ((Boolean)arg.getValue(WATERLOGGED)) {
            arg4.scheduleTick(arg5, Fluids.WATER, Fluids.WATER.getTickDelay(arg4));
        }

        return arg2 == Direction.DOWN && !this.canSurvive(arg, arg4, arg5) ? Blocks.AIR.defaultBlockState() : super.updateShape(arg, arg2, arg3, arg4, arg5, arg6);
    }

    public void animateTick(BlockState arg, Level arg2, BlockPos arg3, RandomSource arg4) {
        int i = arg3.getX();
        int j = arg3.getY();
        int k = arg3.getZ();
        double d = (double)i + arg4.nextDouble();
        double e = (double)j + arg4.nextDouble();
        double f = (double)k + arg4.nextDouble();

        if (arg4.nextInt(5) == 0) {
            arg2.addParticle(GenesisParticles.ZAP_BUBBLE_PARTICLES.get(), d, e, f, (double) 0.0F, (double) 0.0F, (double) 0.0F);
        }

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        if (arg4.nextInt(10) == 0) {
            for (int l = 0; l < 14; ++l) {
                mutableBlockPos.set(i + Mth.nextInt(arg4, -10, 10), j - arg4.nextInt(10), k + Mth.nextInt(arg4, -10, 10));
                BlockState blockState = arg2.getBlockState(mutableBlockPos);
                if (!blockState.isCollisionShapeFullBlock(arg2, mutableBlockPos)) {
                    arg2.addParticle(ParticleTypes.WARPED_SPORE, (double) mutableBlockPos.getX() + arg4.nextDouble(), (double) mutableBlockPos.getY() + arg4.nextDouble(), (double) mutableBlockPos.getZ() + arg4.nextDouble(), (double) 0.0F, (double) 0.0F, (double) 0.0F);
                }
            }
        }

    }

    public boolean canSurvive(BlockState arg, LevelReader arg2, BlockPos arg3) {
        BlockPos blockPos = arg3.below();
        return arg2.getBlockState(blockPos).isFaceSturdy(arg2, blockPos, Direction.UP);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> arg) {
        arg.add(new Property[]{WATERLOGGED});
    }

    public FluidState getFluidState(BlockState arg) {
        return (Boolean)arg.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(arg);
    }

    static {
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        AABB = Block.box((double)2.0F, (double)0.0F, (double)2.0F, (double)14.0F, (double)4.0F, (double)14.0F);
    }
}