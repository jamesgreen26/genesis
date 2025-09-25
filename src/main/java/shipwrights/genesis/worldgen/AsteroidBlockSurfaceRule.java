package shipwrights.genesis.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.GenesisBlocks;

import java.util.List;


public class AsteroidBlockSurfaceRule implements SurfaceRules.RuleSource {
    public static final KeyDispatchDataCodec<AsteroidBlockSurfaceRule> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(new AsteroidBlockSurfaceRule()));

    AsteroidBlockSurfaceRule() {}

    @Override
    public @NotNull KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context arg) {
        return (i, j, k) -> {
            int index = (int) (hash3(i, j, k) % states.size());
            return states.get(index);
        };
    }

    private long hash3(int x, int y, int z) {
        long h = x * 73428767L ^ y * 91278311L ^ z * 37855139L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return Math.abs(h);
    }

    private final List<BlockState> states = List.of(
        GenesisBlocks.ASTEROID_0.get().defaultBlockState()
    );
}
