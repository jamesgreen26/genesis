package g_mungus.genesis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Random;

public class RandomNoise implements DensityFunction {

    public static final MapCodec<RandomNoise> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("scale").forGetter(r -> r.scale),
                    Codec.DOUBLE.fieldOf("frequency").forGetter(r -> r.frequency)
            ).apply(instance, RandomNoise::new)
    );

    public static final KeyDispatchDataCodec<RandomNoise> KEY_CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final double scale;
    private final double frequency;

    public RandomNoise(double scale, double frequency) {
        this.scale = scale;
        this.frequency = frequency;
    }

    @Override
    public double compute(FunctionContext context) {
        long seed = getSeed(context.blockX(), context.blockY(), context.blockZ());
        Random random = new Random(seed);

        if (random.nextDouble() < frequency) {
            // Apply positive noise value
            return (random.nextDouble() * 2 - 1) * scale;
        } else {
            // Zero noise at this point
            return 0.0;
        }
    }

    private long getSeed(int x, int y, int z) {
        return x * 341873128712L + y * 132897987541L + z * 42317861L;
    }

    @Override public void fillArray(double[] arr, ContextProvider provider) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = compute(provider.forIndex(i));
        }
    }

    @Override public DensityFunction mapAll(Visitor visitor) {
        return this;
    }

    @Override public double minValue() { return -scale; }
    @Override public double maxValue() { return scale; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return KEY_CODEC; }
}
