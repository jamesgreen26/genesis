package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;
import shipwrights.genesis.GenesisMod;

import static java.lang.Math.max;

public class MultiCraterNoise implements DensityFunction {

    public static final ResourceLocation resourceLocation =
            ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "multi_crater");

    private final double[] scales;

    public MultiCraterNoise(double[] scales) {
        this.scales = scales;
    }

    public static final MapCodec<MultiCraterNoise> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.listOf()
                            .fieldOf("scales")
                            .forGetter(crater -> java.util.Arrays.stream(crater.scales).boxed().toList())
            ).apply(instance, list -> new MultiCraterNoise(list.stream().mapToDouble(Double::doubleValue).toArray()))
    );

    public static final KeyDispatchDataCodec<MultiCraterNoise> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        double total = 0.0;

        for (double scale : scales) {
            total += computeSingle(x, z, scale);
        }

        return total;
    }

    private double computeSingle(int x, int z, double scale) {
        double frequency = 0.5;

        double scaledX = frequency * x / scale;
        double scaledZ = frequency * z / scale;

        double scaleY = scale / 1280.0;

        int cellX = (int) Math.floor(scaledX);
        int cellZ = (int) Math.floor(scaledZ);

        double minDist = Double.MAX_VALUE;

        // 3x3 neighborhood
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int nx = cellX + offsetX;
                int nz = cellZ + offsetZ;

                long seed = hashCell(nx, nz);

                double randomX = nx + lcgRandom(seed);
                double randomZ = nz + lcgRandom(seed + 1);

                double sizeVar = 0.7 + lcgRandom(seed + 2) * 0.6;

                double dx = (scaledX - randomX) / sizeVar;
                double dz = (scaledZ - randomZ) / sizeVar;

                double dist = dx * dx + dz * dz;
                if (dist < minDist) minDist = dist;
            }
        }

        double crater = 12 * minDist / (frequency * frequency);

        return scaleY * (crater - 0.5) / max(1.0, crater * crater * crater);
    }

    private long hashCell(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    private double lcgRandom(long seed) {
        seed = (seed * 1103515245L + 12345L) & 0x7FFFFFFFL;
        return (double) seed / (double) 0x7FFFFFFFL;
    }

    @Override
    public void fillArray(double[] arr, @NotNull ContextProvider provider) {
        // Still fine, compute() is now heavier but called fewer times overall
        for (int i = 0; i < arr.length; i++) {
            arr[i] = compute(provider.forIndex(i));
        }
    }

    @Override
    public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
        return this;
    }

    @Override public double minValue() { return -1.0; }
    @Override public double maxValue() { return 1.0; }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}