package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/// A cluster of basalt columns rising from a geothermal pool, tallest in the middle and some breaking the surface, with
/// a crust of basalt and smooth basalt on the dry floor around it. Placed at a water block on a pool's floor, after
/// {@link GeothermalFeature}. The magma under the water is left alone, and columns stop short of
/// {@link GeothermalFeature#ICE_MIN_Y} so they stay in the stone gap.
public class BasaltFormationFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_RADIUS = 2;
    private static final int MAX_RADIUS = 4;
    private static final int MIN_PEAK = 3;
    private static final int MAX_PEAK = 8;
    private static final float COLUMN_CHANCE = 0.7F;
    /// How far past the columns the crust spreads
    private static final int CRUST_SPREAD = 2;
    private static final float CRUST_CHANCE = 0.6F;
    private static final float SMOOTH_CRUST_CHANCE = 0.3F;
    /// How far above and below the placement to look for the floor under each column
    private static final int FLOOR_SEARCH_UP = 4;
    private static final int FLOOR_SEARCH_DOWN = 6;

    public BasaltFormationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin).is(Blocks.WATER)) {
            return false;
        }

        int radius = Mth.nextInt(random, MIN_RADIUS, MAX_RADIUS);
        int peak = Mth.nextInt(random, MIN_PEAK, MAX_PEAK);
        BlockState basalt = Blocks.BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        int reach = radius + CRUST_SPREAD;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > reach + 0.5) {
                    continue;
                }
                BlockPos floor = findFloor(level, origin.offset(dx, 0, dz));
                if (floor == null) {
                    continue;
                }

                if (distance <= radius + 0.5 && random.nextFloat() < COLUMN_CHANCE) {
                    // Tallest in the middle, falling away towards the edge, with some jitter
                    int height = Math.round(peak * (1.0F - (float) distance / (radius + 1)) + random.nextInt(3) - 1);
                    for (int i = 1; i <= Math.max(1, height); i++) {
                        pos.setWithOffset(floor, 0, i, 0);
                        BlockState state = level.getBlockState(pos);
                        if (pos.getY() >= GeothermalFeature.ICE_MIN_Y || !(state.isAir() || state.is(Blocks.WATER))) {
                            break;
                        }
                        level.setBlock(pos, basalt, 2);
                        placed = true;
                    }
                }

                // Crust the dry floor; underwater floors stay magma
                BlockState floorState = level.getBlockState(floor);
                if (!floorState.is(Blocks.MAGMA_BLOCK) && !floorState.is(Blocks.BEDROCK)
                        && level.getBlockState(floor.above()).isAir() && random.nextFloat() < CRUST_CHANCE) {
                    level.setBlock(floor, random.nextFloat() < SMOOTH_CRUST_CHANCE ? Blocks.SMOOTH_BASALT.defaultBlockState() : basalt, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /// The nearest solid block below open space (air or water) around {@code start}'s height
    private static BlockPos findFloor(WorldGenLevel level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable().move(Direction.UP, FLOOR_SEARCH_UP);
        for (int i = 0; i <= FLOOR_SEARCH_UP + FLOOR_SEARCH_DOWN; i++, pos.move(Direction.DOWN)) {
            BlockState above = level.getBlockState(pos.above());
            if (level.getBlockState(pos).isSolidRender(level, pos) && (above.isAir() || above.is(Blocks.WATER))) {
                return pos.immutable();
            }
        }
        return null;
    }
}
