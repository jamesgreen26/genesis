package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.GenesisBlocks;

/// A scattering of ice crystals around a point in a cave, each on a floor, wall or ceiling and pointing away from it.
/// They only grow on blocks in {@link #GROWS_ON}.
public class IceCrystalFeature extends Feature<NoneFeatureConfiguration> {
    public static final TagKey<Block> GROWS_ON = BlockTags.create(GenesisMod.id("ice_crystal_grows_on"));

    private static final int TRIES = 16;
    private static final int XZ_SPREAD = 3;
    private static final int Y_SPREAD = 2;

    public IceCrystalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState crystal = GenesisBlocks.ICE_CRYSTAL.get().defaultBlockState();

        boolean placed = false;
        for (int i = 0; i < TRIES; i++) {
            BlockPos pos = origin.offset(
                    random.nextInt(2 * XZ_SPREAD + 1) - XZ_SPREAD,
                    random.nextInt(2 * Y_SPREAD + 1) - Y_SPREAD,
                    random.nextInt(2 * XZ_SPREAD + 1) - XZ_SPREAD);
            if (!level.getBlockState(pos).isAir()) continue;

            for (Direction facing : Util.shuffledCopy(Direction.values(), random)) {
                BlockPos support = pos.relative(facing.getOpposite());
                BlockState supportState = level.getBlockState(support);
                if (supportState.is(GROWS_ON) && supportState.isFaceSturdy(level, support, facing)) {
                    level.setBlock(pos, crystal.setValue(AmethystClusterBlock.FACING, facing), 2);
                    placed = true;
                    break;
                }
            }
        }
        return placed;
    }
}
