package g_mungus.genesis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import g_mungus.genesis.GenesisMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import java.util.Random;

public class RandomNoise implements DensityFunction {

    public static final MapCodec<RandomNoise> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("scale").forGetter(r -> r.scale)
            ).apply(instance, RandomNoise::new)
    );

    public static final KeyDispatchDataCodec<RandomNoise> KEY_CODEC = KeyDispatchDataCodec.of(MAP_CODEC);


    private final double scale;

    public RandomNoise(double scale) {
        this.scale = scale;
    }

    @Override
    public double compute(FunctionContext context) {
        long seed = getSeed(context.blockX(), context.blockY(), context.blockZ());
        Random random = new Random(seed);
        return (random.nextDouble() * 2 - 1) * scale;  // Range [-scale, +scale]
    }

    private long getSeed(int x, int y, int z) {
        // Simple deterministic hash based on coordinates
        long seed = x * 341873128712L + y * 132897987541L + z * 42317861L;
        return seed;
    }

    @Override public void fillArray(double[] arr, ContextProvider provider) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = compute(provider.forIndex(i));
        }
    }

    @Override public DensityFunction mapAll(Visitor visitor) {
        return this; // No child function to map
    }

    @Override public double minValue() { return -scale; }
    @Override public double maxValue() { return scale; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return KEY_CODEC; }
}


