package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;
import shipwrights.genesis.content.GenesisBlocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A long, narrow crack in a cave floor, hidden under brittle ice: a hollow drop that opens a couple of seconds after
/// something steps on it. Placed on a floor block, it runs along a random axis, following the floor as it rises and
/// falls, deepest in the middle. It ends early where the floor does.
///
/// The hollow is only cut into solid ground, so the brittle ice never covers an opening that can be seen into. Hollow
/// rather than filled with water, as the moon is far too cold for liquid water.
public class CrevasseFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 20;
    private static final int MIN_DEPTH = 4;
    private static final int MAX_DEPTH = 10;
    /// How far the floor may rise or fall from one step of the crevasse to the next
    private static final int MAX_STEP = 1;

    public CrevasseFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!isFloor(level, origin)) return false;

        Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
        Direction across = along.getClockWise();
        int plannedLength = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        int width = 1 + random.nextInt(2);
        int depth = MIN_DEPTH + random.nextInt(MAX_DEPTH - MIN_DEPTH + 1);

        // The surface cells, which become brittle ice, and the hollow under them
        Set<BlockPos> surface = new HashSet<>();
        Set<BlockPos> hollow = new HashSet<>();
        BlockPos floor = origin;
        int length = 0;
        for (int i = 0; i < plannedLength; i++) {
            if (i > 0) {
                floor = followFloor(level, floor.relative(along));
                if (floor == null) break;
            }
            // Deepest in the middle, tapering towards the ends
            int stepDepth = Math.max(2, Math.round(depth * (float) Math.sin(Math.PI * (i + 0.5) / plannedLength)));

            List<BlockPos> stepSurface = new ArrayList<>();
            List<BlockPos> stepHollow = new ArrayList<>();
            boolean ok = true;
            for (int j = 0; j < width && ok; j++) {
                BlockPos top = floor.relative(across, j);
                ok = isFloor(level, top);
                stepSurface.add(top);
                for (int k = 1; k <= stepDepth && ok; k++) {
                    stepHollow.add(top.below(k));
                }
            }
            if (!ok) break;

            Set<BlockPos> withStepSurface = new HashSet<>(surface);
            withStepSurface.addAll(stepSurface);
            Set<BlockPos> withStepHollow = new HashSet<>(hollow);
            withStepHollow.addAll(stepHollow);
            if (!enclosed(level, stepHollow, withStepSurface, withStepHollow)) break;

            surface = withStepSurface;
            hollow = withStepHollow;
            length++;
        }
        if (length < MIN_LENGTH) return false;

        for (BlockPos pos : hollow) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState brittleIce = GenesisBlocks.BRITTLE_ICE.get().defaultBlockState();
        for (BlockPos pos : surface) {
            level.setBlock(pos, brittleIce, 2);
        }
        return true;
    }

    /// The floor at or within {@link #MAX_STEP} blocks above or below {@code pos}, if there is one
    private static @Nullable BlockPos followFloor(WorldGenLevel level, BlockPos pos) {
        for (int dy = 0; dy <= MAX_STEP; dy++) {
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
    /// that no opening shows anywhere but through the brittle ice
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
