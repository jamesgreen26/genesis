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
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();

        double center = getNoise(x, y, z);
        double scale = 1.0;

        if (getNoise(x + 25320, y * 4 - 3453, z * 2 + 534) > frequency) return 0.0;

        // Compare with adjacent blocks
        if (center < getNoise(x + 1, y, z)) return 0.0;
        if (center < getNoise(x - 1, y, z)) return 0.0;
        if (center < getNoise(x, y + 1, z)) return 0.0;
        if (center < getNoise(x, y - 1, z)) return 0.0;
        if (center < getNoise(x, y, z + 1)) return 0.0;
        if (center < getNoise(x, y, z - 1)) return 0.0;

        // Confirm it's strong enough
        double scaled = center * scale;
        return scaled > 0.1 ? scaled : 0.0;
    }

    private double getNoise(int x, int y, int z) {
        // Mix coordinates more smoothly to reduce axis-aligned artifacts
        long seed = x * 3129871L ^ z * 116129781L ^ y * 42317861L;
        seed = seed * seed * 42317861L + seed * 11L;

        // Spread value out
        seed ^= (seed >>> 13);
        seed *= 0x5DEECE66DL;
        seed ^= (seed >>> 17);

        // Return in [0.0, 1.0]
        return (double)(seed & 0xFFFFFFF) / (double)0xFFFFFFF;
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
