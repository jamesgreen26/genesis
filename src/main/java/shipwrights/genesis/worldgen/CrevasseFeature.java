package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.content.GenesisBlocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A long crack in a cave floor, frozen over with thin ice all the way down: a drop that opens when something jumps or
/// falls onto it, breaking through every layer to the bottom. Placed on a floor block, it runs out both ways from it at
/// a random angle, following the floor as it rises and falls. It's widest and deepest in the middle, tapering to its
/// ends, and deeper along its middle than at its sides. It ends early where the floor does.
///
/// It's only cut into solid ground, so once broken through, it never opens out into anything else. Ice rather than
/// water, as the moon is far too cold for liquid water.
public class CrevasseFeature extends Feature<NoneFeatureConfiguration> {
    // A feature may only reach about a chunk from where it's placed, so the crevasse's half length plus its half width
    // is kept within 16 blocks of it
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 26;
    private static final float MIN_HALF_WIDTH = 1.5F;
    private static final float MAX_HALF_WIDTH = 2.5F;
    private static final int MIN_DEPTH = 8;
    private static final int MAX_DEPTH = 18;
    /// How much shallower its sides are than its middle, as a fraction of the depth there
    private static final float SIDE_SHALLOWING = 0.6F;
    /// How far it advances along, and across, between the columns it takes in: under a block, so that it leaves no
    /// gaps when running at an angle to the grid
    private static final float STEP = 0.5F;
    /// How far the floor may rise or fall from one step of the crevasse to the next
    private static final int MAX_FLOOR_STEP = 1;

    public CrevasseFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!isFloor(level, origin)) return false;

        float angle = random.nextFloat() * Mth.TWO_PI;
        float alongX = Mth.cos(angle);
        float alongZ = Mth.sin(angle);
        float length = MIN_LENGTH + random.nextFloat() * (MAX_LENGTH - MIN_LENGTH);
        float maxHalfWidth = MIN_HALF_WIDTH + random.nextFloat() * (MAX_HALF_WIDTH - MIN_HALF_WIDTH);
        int maxDepth = MIN_DEPTH + random.nextInt(MAX_DEPTH - MIN_DEPTH + 1);

        Crevasse crevasse = new Crevasse(level, -alongZ, alongX);
        int steps = 0;
        // Out from the middle one way, then the other, each stopping where the floor or the ground gives out
        for (int sign : new int[]{1, -1}) {
            int floorY = origin.getY();
            for (int i = sign > 0 ? 0 : 1; i * STEP <= length / 2; i++) {
                float along = sign * i * STEP;
                double x = origin.getX() + 0.5D + along * alongX;
                double z = origin.getZ() + 0.5D + along * alongZ;
                BlockPos middle = followFloor(level, BlockPos.containing(x, floorY, z));
                if (middle == null) break;

                // 1 in the middle, falling to 0 at the ends
                float taper = Mth.cos(Mth.PI * along / length);
                int depth = Math.max(2, Math.round(maxDepth * taper));
                float halfWidth = Math.max(0.5F, maxHalfWidth * Mth.sqrt(taper));
                // Keep the ice out of the geothermal zone's stone gap
                if (!GeothermalFeature.allowsIce(middle.getY() - depth)) break;
                if (!crevasse.tryAddStep(x, z, middle.getY(), halfWidth, depth)) break;

                floorY = middle.getY();
                steps++;
            }
        }
        if (steps * STEP < MIN_LENGTH) return false;

        BlockState thinIce = GenesisBlocks.THIN_ICE.get().defaultBlockState();
        for (BlockPos pos : crevasse.surface) {
            level.setBlock(pos, thinIce, 2);
        }
        for (BlockPos pos : crevasse.hollow) {
            level.setBlock(pos, thinIce, 2);
        }
        return true;
    }

    /// The crevasse as it's planned out, a step at a time
    private static final class Crevasse {
        private final WorldGenLevel level;
        private final float acrossX;
        private final float acrossZ;
        /// The floor block at the top of each column it takes in, by its x and z
        private final Map<Long, BlockPos> tops = new HashMap<>();
        /// The floor blocks it takes in, and the ground under them
        private final Set<BlockPos> surface = new HashSet<>();
        private final Set<BlockPos> hollow = new HashSet<>();

        private Crevasse(WorldGenLevel level, float acrossX, float acrossZ) {
            this.level = level;
            this.acrossX = acrossX;
            this.acrossZ = acrossZ;
        }

        /// Takes in a cross-section of the crevasse, centred on {@code x}, {@code z}, if the floor runs all the way
        /// across it and the ground around it is solid
        private boolean tryAddStep(double x, double z, int floorY, float halfWidth, int depth) {
            Map<Long, BlockPos> stepTops = new HashMap<>();
            List<BlockPos> stepSurface = new ArrayList<>();
            List<BlockPos> stepHollow = new ArrayList<>();
            for (float across = -halfWidth; across <= halfWidth + 1.0E-4F; across += STEP) {
                int columnX = Mth.floor(x + across * acrossX);
                int columnZ = Mth.floor(z + across * acrossZ);
                long column = BlockPos.asLong(columnX, 0, columnZ);
                BlockPos top = tops.get(column);
                if (top == null) top = stepTops.get(column);
                if (top == null) {
                    top = followFloor(level, new BlockPos(columnX, floorY, columnZ));
                    if (top == null) return false;
                    stepTops.put(column, top);
                }

                float side = across / halfWidth;
                int columnDepth = Math.max(1, Math.round(depth * (1.0F - SIDE_SHALLOWING * side * side)));
                stepSurface.add(top);
                for (int k = 1; k <= columnDepth; k++) {
                    stepHollow.add(top.below(k));
                }
            }

            Set<BlockPos> withStepSurface = new HashSet<>(surface);
            withStepSurface.addAll(stepSurface);
            Set<BlockPos> withStepHollow = new HashSet<>(hollow);
            withStepHollow.addAll(stepHollow);
            if (!enclosed(level, stepHollow, withStepSurface, withStepHollow)) return false;

            tops.putAll(stepTops);
            surface.addAll(stepSurface);
            hollow.addAll(stepHollow);
            return true;
        }
    }

    /// The floor at or within {@link #MAX_FLOOR_STEP} blocks above or below {@code pos}, if there is one
    private static @Nullable BlockPos followFloor(WorldGenLevel level, BlockPos pos) {
        for (int dy = 0; dy <= MAX_FLOOR_STEP; dy++) {
            if (isFloor(level, pos.above(dy))) return pos.above(dy);
            if (dy > 0 && isFloor(level, pos.below(dy))) return pos.below(dy);
        }
        return null;
    }

    /// A solid block that can be replaced, with open air above it
    private static boolean isFloor(WorldGenLevel level, BlockPos pos) {
        return replaceable(level, pos) && level.getBlockState(pos.above()).isAir();
    }

    /// Whether every one of {@code cells} can be hollowed and is closed in by solid ground or the crevasse itself, so
    /// that it opens nowhere but at the top
    private static boolean enclosed(WorldGenLevel level, List<BlockPos> cells, Set<BlockPos> surface, Set<BlockPos> hollow) {
        for (BlockPos pos : cells) {
            if (!replaceable(level, pos)) return false;
            for (Direction side : Direction.values()) {
                BlockPos neighbour = pos.relative(side);
                if (!hollow.contains(neighbour) && !surface.contains(neighbour) && !solid(level, neighbour)) return false;
            }
        }
        return true;
    }

    private static boolean replaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return solid(level, pos) && !state.is(BlockTags.FEATURES_CANNOT_REPLACE) && !state.hasBlockEntity();
    }

    private static boolean solid(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }
}
