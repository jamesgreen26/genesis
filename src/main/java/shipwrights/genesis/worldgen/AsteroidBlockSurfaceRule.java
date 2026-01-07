package shipwrights.genesis.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.content.block.GenesisBlocks;
import shipwrights.genesis.content.block.AsteroidBlock;

import java.util.ArrayList;
import java.util.List;


public class AsteroidBlockSurfaceRule implements SurfaceRules.RuleSource {
    public static final KeyDispatchDataCodec<AsteroidBlockSurfaceRule> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(new AsteroidBlockSurfaceRule()));

    private static List<BlockState> states;

    AsteroidBlockSurfaceRule() {}

    @Override
    public @NotNull KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context arg) {
        return (i, j, k) -> {
            int index = (int) (hash3(i, j, k) % getStates().size());
            return getStates().get(index);
        };
    }

    private long hash3(int x, int y, int z) {
        long h = x * 73428767L ^ y * 91278311L ^ z * 37855139L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return Math.abs(h);
    }

    private static List<BlockState> getStates() {
        if (states == null) {
            states = buildStates();
        }
        return states;
    }

    private static List<BlockState> buildStates() {
        List<BlockState> result = new ArrayList<>();
        BlockState baseState = GenesisBlocks.ASTEROID_0.get().defaultBlockState();

        int maxVariant = AsteroidBlock.VARIANT.getPossibleValues().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        int maxPalette = AsteroidBlock.PALETTE.getPossibleValues().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        for (int palette = 0; palette <= maxPalette; palette++) {
            for (int variant = 0; variant <= maxVariant; variant++) {
                result.add(baseState
                        .setValue(AsteroidBlock.VARIANT, variant)
                        .setValue(AsteroidBlock.PALETTE, palette));
            }
        }

        return result;
    }
}
