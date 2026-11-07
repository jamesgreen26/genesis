package shipwrights.genesis.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/// An icicle hanging from a ceiling, several blocks long, thinning from its base down to its tip. Like vanilla's
/// pointed dripstone, but it only hangs, and it falls when the light around it rises above 4, as well as when it loses
/// its support. A falling icicle hurts what it lands on and shatters.
public class IcicleBlock extends Block implements Fallable {
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;

    /// The light level an icicle can stand; any brighter and it falls
    private static final int MAX_LIGHT = 4;

    private static final VoxelShape TIP_SHAPE = Block.box(5, 5, 5, 11, 16, 11);
    private static final VoxelShape FRUSTUM_SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape MIDDLE_SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape BASE_SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public IcicleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(THICKNESS, DripstoneThickness.TIP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(THICKNESS);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState support = level.getBlockState(above);
        return support.is(this) || support.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        if (!canSurvive(state, context.getLevel(), context.getClickedPos())) return null;
        return state.setValue(THICKNESS, thickness(context.getLevel(), context.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction != Direction.UP && direction != Direction.DOWN) return state;
        // Already falling
        if (level.getBlockTicks().hasScheduledTick(pos, this)) return state;
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 2);
            return state;
        }
        return state.setValue(THICKNESS, thickness(level, pos));
    }

    /// As vanilla works out a stalactite's: a tip at the bottom, then a frustum, then middles, and a base at the top
    private DripstoneThickness thickness(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.is(this)) return DripstoneThickness.TIP;
        if (below.getValue(THICKNESS) == DripstoneThickness.TIP) return DripstoneThickness.FRUSTUM;
        return level.getBlockState(pos.above()).is(this) ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE;
    }

    /// Scheduled when the icicle loses its support
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        fall(level, pos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos) > MAX_LIGHT) {
            fall(level, pos);
        }
    }

    /// Drops the icicle from {@code pos} down to its tip, which hurts what it lands on
    private void fall(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos piece = pos.mutable();
        for (BlockState state = level.getBlockState(piece); state.is(this); state = level.getBlockState(piece)) {
            FallingBlockEntity falling = FallingBlockEntity.fall(level, piece, state);
            falling.disableDrop();
            if (state.getValue(THICKNESS) == DripstoneThickness.TIP) {
                // As vanilla's stalactites: the longer the icicle, the harder it hits
                falling.setHurtsEntities(Math.max(1 + pos.getY() - piece.getY(), 6), 40);
                break;
            }
            piece.move(Direction.DOWN);
        }
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            // Shatters, with the block's breaking particles and sound
            level.levelEvent(2001, pos, Block.getId(fallingBlock.getBlockState()));
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(GenesisDamageTypes.FALLING_ICICLE), entity);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(THICKNESS)) {
            case TIP, TIP_MERGE -> TIP_SHAPE;
            case FRUSTUM -> FRUSTUM_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case BASE -> BASE_SHAPE;
        };
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
