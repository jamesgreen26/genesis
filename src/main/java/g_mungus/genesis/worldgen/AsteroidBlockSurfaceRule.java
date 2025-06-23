package g_mungus.genesis.worldgen;

import com.mojang.serialization.MapCodec;
import g_mungus.genesis.GenesisMod;
import g_mungus.genesis.asteroid.AsteroidBlock;
import g_mungus.genesis.asteroid.generation.AsteroidGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.NotNull;

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
            int index = (int) (hash3(i, j, k) % AsteroidGenerator.ASTEROID_COUNT);
            return GenesisMod.ASTEROID_BLOCK.get().defaultBlockState().setValue(AsteroidBlock.INDEX, index);
        };
    }

    private long hash3(int x, int y, int z) {
        long h = x * 73428767L ^ y * 91278311L ^ z * 37855139L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return Math.abs(h);
    }
}
