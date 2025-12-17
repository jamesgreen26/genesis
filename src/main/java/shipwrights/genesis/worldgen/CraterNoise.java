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

public class CraterNoise implements DensityFunction {

    public static final ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "crater");

    private final double scale;

    public CraterNoise(double scale) {
        this.scale = scale;
    }

    public static final MapCodec<CraterNoise> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.DOUBLE.fieldOf("scale").forGetter(crater -> crater.scale)
            ).apply(instance, CraterNoise::new)
    );

    public static final KeyDispatchDataCodec<CraterNoise> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        double frequency = 0.5;

        double scaledX = frequency * x / this.scale;
        double scaledZ = frequency * z / this.scale;

        double scaleY = this.scale / 1280.0;

        int cellX = (int) Math.floor(scaledX);
        int cellZ = (int) Math.floor(scaledZ);

        double minDist = Double.MAX_VALUE;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int neighborCellX = cellX + offsetX;
                int neighborCellZ = cellZ + offsetZ;

                long seed = hashCell(neighborCellX, neighborCellZ);
                double randomX = neighborCellX + lcgRandom(seed);
                double randomZ = neighborCellZ + lcgRandom(seed + 1);

                double cellSizeVariation = 0.7 + lcgRandom(seed + 2) * 0.6; // 0.7 to 1.3x size

                double dx = (scaledX - randomX) / cellSizeVariation;
                double dz = (scaledZ - randomZ) / cellSizeVariation;
                double dist = dx * dx + dz * dz;

                minDist = Math.min(minDist, dist);
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
        for (int i = 0; i < arr.length; i++) {
            FunctionContext ctx = provider.forIndex(i);
            arr[i] = compute(ctx);
        }
    }

    @Override
    public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
        return this; // No children to map
    }

    @Override public double minValue() { return 0.0; }
    @Override public double maxValue() { return -1.0; }
    @Override public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
