package shipwrights.dataplanets.systemCreation.dimension.biome.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CrystalFeature extends Feature<NoneFeatureConfiguration> {
    public CrystalFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> arg) {
        BlockPos blockPos = arg.origin();
        RandomSource randomSource = arg.random();


        WorldGenLevel worldGenLevel;
        for(worldGenLevel = arg.level(); worldGenLevel.isEmptyBlock(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2; blockPos = blockPos.below()) {
        }

        BlockState startState = worldGenLevel.getBlockState(blockPos);

        if (!startState.isSolid() || startState.is(Blocks.AMETHYST_BLOCK) || randomSource.nextInt(120) != 0) {
            return false;
        }

        blockPos = blockPos.below(5 + randomSource.nextInt(12));
        int height = 20 + randomSource.nextInt(15);
        int maxRadius = 4 + randomSource.nextInt(3);
        int baseRadius = 2 + randomSource.nextInt(2);
        int expandHeight = (int) (height * 0.5F);
        int taperStartHeight = (int) (height * 0.6F);

        for (int k = 0; k < height; ++k) {
            int radius;
            if (k < expandHeight) {
                float expandProgress = (float) k / (float) expandHeight;
                radius = (int) (baseRadius + (maxRadius - baseRadius) * expandProgress);
            } else if (k < taperStartHeight) {
                radius = maxRadius;
            } else {
                float taperProgress = (float) (k - taperStartHeight) / (float) (height - taperStartHeight);
                radius = Math.max(0, (int) (maxRadius * (1.0F - taperProgress)));
            }

            for (int m = -radius; m <= radius; ++m) {
                for (int n = -radius; n <= radius; ++n) {
                    if (Math.abs(m) + Math.abs(n) <= radius) {
                        this.setBlock(worldGenLevel, blockPos.offset(m, k, n), Blocks.AMETHYST_BLOCK.defaultBlockState());
                    }
                }
            }
        }

        for (int o = -1; o <= 1; ++o) {
            for (int p = -1; p <= 1; ++p) {
                if (Math.abs(o) + Math.abs(p) <= 1) {
                    BlockPos blockPos2 = blockPos.offset(o, -1, p);
                    int depth = 3 + randomSource.nextInt(3);

                    for (int d = 0; d < depth; ++d) {
                        this.setBlock(worldGenLevel, blockPos2, Blocks.AMETHYST_BLOCK.defaultBlockState());
                        blockPos2 = blockPos2.below();
                    }
                }
            }
        }

        return true;
    }
}