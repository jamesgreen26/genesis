package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import shipwrights.genesis.content.GenesisBlocks;

/// A pillar of packed ice from a cave floor up to its ceiling, flaring out where it meets them and pinching to a
/// narrow waist in the middle, like an hourglass. Placed at the open block just above a floor.
///
/// Only stands between solid ground: it skips if there's no ceiling within reach or too little room, if the floor or
/// ceiling is a thin ledge, or if its flared ends would hang out over open space.
public class IcePillarFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_HEIGHT = 4;
    private static final int MAX_HEIGHT = 48;
    private static final float MIN_END_RADIUS = 2.0F;
    private static final float MAX_END_RADIUS = 4.5F;
    /// The waist as a fraction of the ends' radius
    private static final float MIN_WAIST = 0.3F;
    private static final float MAX_WAIST = 0.5F;
    /// How sharply it pinches in: higher keeps it narrow for more of its height
    private static final float TAPER = 1.6F;
    /// How far each of its end columns carries on past the floor and ceiling through open space, so the flared ends
    /// meet an uneven floor or ceiling without gaps
    private static final int FOLLOW_THROUGH = 4;
    /// How thick the floor below its centre and the ceiling above must be, so it doesn't stand on a ledge or overhang
    private static final int MIN_GROUND_THICKNESS = 4;

    public IcePillarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos bottom = context.origin();

        BlockState floor = level.getBlockState(bottom.below());
        if (!level.getBlockState(bottom).isAir() || !floor.isSolidRender(level, bottom.below())
                || floor.is(GenesisBlocks.BRITTLE_ICE.get())) {
            return false;
        }

        // The open height between floor and ceiling
        int height = 0;
        while (height < MAX_HEIGHT && level.getBlockState(bottom.above(height)).isAir()) {
            height++;
        }
        if (height < MIN_HEIGHT || height >= MAX_HEIGHT
                || !level.getBlockState(bottom.above(height)).isSolidRender(level, bottom.above(height))) {
            return false;
        }

        // Taller pillars have wider ends
        float endRadius = Mth.lerp(Math.min(1.0F, height / 24.0F), MIN_END_RADIUS, MAX_END_RADIUS) + random.nextFloat() * 0.5F;
        float waistRadius = Math.max(0.8F, endRadius * Mth.lerp(random.nextFloat(), MIN_WAIST, MAX_WAIST));

        BlockPos top = bottom.above(height - 1);
        if (!solidFor(level, bottom.below(), Direction.DOWN, MIN_GROUND_THICKNESS)
                || !solidFor(level, top.above(), Direction.UP, MIN_GROUND_THICKNESS)
                || !endsSupported(level, bottom, top, endRadius)) {
            return false;
        }

        BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
        for (int y = 0; y < height; y++) {
            // 0 at the floor and ceiling, 1 at the middle
            float middle = 1.0F - Math.abs(2.0F * (y + 0.5F) / height - 1.0F);
            float radius = Mth.lerp((float) Math.pow(middle, 1.0F / TAPER), endRadius, waistRadius);
            int reach = Mth.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    // Slightly ragged edges
                    float edge = radius + (random.nextFloat() - 0.5F) * 0.6F;
                    if (dx * dx + dz * dz > edge * edge) continue;
                    BlockPos pos = bottom.offset(dx, y, dz);
                    if (open(level, pos)) {
                        level.setBlock(pos, ice, 2);
                    }
                }
            }
        }

        // Carry the ends' columns on down to the floor and up to the ceiling where those are further away than at the
        // pillar's centre, stopping at the first solid block so nothing closed off is filled
        int endReach = Mth.ceil(endRadius);
        for (int dx = -endReach; dx <= endReach; dx++) {
            for (int dz = -endReach; dz <= endReach; dz++) {
                if (dx * dx + dz * dz > endRadius * endRadius) continue;
                for (int i = 1; i <= FOLLOW_THROUGH; i++) {
                    BlockPos pos = bottom.offset(dx, -i, dz);
                    if (!open(level, pos)) break;
                    level.setBlock(pos, ice, 2);
                }
                for (int i = 0; i < FOLLOW_THROUGH; i++) {
                    BlockPos pos = bottom.offset(dx, height + i, dz);
                    if (!open(level, pos)) break;
                    level.setBlock(pos, ice, 2);
                }
            }
        }
        return true;
    }

    /// Whether {@code length} blocks from {@code start} on in {@code direction} are all solid
    private static boolean solidFor(WorldGenLevel level, BlockPos start, Direction direction, int length) {
        for (int i = 0; i < length; i++) {
            BlockPos pos = start.relative(direction, i);
            if (!level.getBlockState(pos).isSolidRender(level, pos)) return false;
        }
        return true;
    }

    /// Whether every column under the flared base, and over the flared top, meets solid ground within the follow-through
    private static boolean endsSupported(WorldGenLevel level, BlockPos bottom, BlockPos top, float endRadius) {
        int reach = Mth.ceil(endRadius);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > endRadius * endRadius) continue;
                if (!meetsGround(level, bottom.offset(dx, 0, dz), Direction.DOWN)
                        || !meetsGround(level, top.offset(dx, 0, dz), Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Whether solid ground is at {@code pos} or within the follow-through beyond it in {@code direction}
    private static boolean meetsGround(WorldGenLevel level, BlockPos pos, Direction direction) {
        for (int i = 0; i <= FOLLOW_THROUGH; i++) {
            BlockPos at = pos.relative(direction, i);
            if (level.getBlockState(at).isSolidRender(level, at)) return true;
        }
        return false;
    }

    private static boolean open(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
