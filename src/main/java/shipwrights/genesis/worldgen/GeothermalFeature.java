package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/// Geothermal heat at the bottom of the moon's caves. From the bottom of the world up:
///
/// - **Flooded:** every open cave block at or below {@link #WATER_LEVEL} is filled with water, and every floor under
///   that water is magma, so magma is never exposed. The water is filled up to one flat level everywhere, so it's
///   always walled in and never flows. Lava (which noise caves get below y=-54 with aquifers off, and carver caves get
///   near the bottom) is replaced by water too
/// - **Stone gap:** from just above the water up to {@link #ICE_MIN_Y}: no ice and no magma, only stone and calcite
/// - **Ice dithers back in** from {@link #ICE_MIN_Y} up to {@link #ICE_FULL_Y}
///
/// The surface rules ({@code genesis:geothermal} vertical gradient in the moon's noise settings) keep packed ice out of
/// the gap. Ice features keep above {@link #ICE_MIN_Y}, either through their placed feature height ranges or by
/// checking it themselves.
///
/// Runs once per chunk (placed with no placement modifiers), after carvers and every other feature in the chunk except
/// {@code genesis:moon_geothermal_basalt} ({@link BasaltFormationFeature}), which grows basalt formations in and
/// around the pools.
public class GeothermalFeature extends Feature<NoneFeatureConfiguration> {
    /// The highest block that's flooded
    public static final int WATER_LEVEL = -46;
    /// The lowest block any ice may occupy; everything between this and the water is the stone gap
    public static final int ICE_MIN_Y = -37;
    /// Ice is fully back from here up
    public static final int ICE_FULL_Y = -30;

    public GeothermalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        int bottom = level.getMinBuildHeight();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                // Bottom up, so the block below has always been settled already
                for (int y = bottom; y <= WATER_LEVEL; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.LAVA)) {
                        continue;
                    }
                    level.setBlock(pos, water, 2);
                    placed = true;

                    BlockPos floor = pos.below();
                    BlockState floorState = level.getBlockState(floor);
                    if (floorState.isSolidRender(level, floor) && !floorState.is(Blocks.BEDROCK)) {
                        level.setBlock(floor, magma, 2);
                    }
                }
            }
        }
        return placed;
    }

    /// Whether ice can go at this height, i.e. it's above the stone gap
    public static boolean allowsIce(int y) {
        return y >= ICE_MIN_Y;
    }
}
