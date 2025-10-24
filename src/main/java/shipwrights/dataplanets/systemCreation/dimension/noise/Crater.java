package shipwrights.dataplanets.systemCreation.dimension.noise;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.DataplanetsMod;

/**
 * Crater density function that creates crater-like depressions using 2D Worley noise cells.
 * Uses the formula: -(0.5 - abs(pow(dist, 3))) / max(pow(dist, 6), 1)
 * where dist is the normalized distance to the nearest cell center in the XZ plane.
 * Y coordinate is ignored - craters are consistent at all heights.
 */
public class Crater implements DensityFunction {

    public static final ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, "crater");

    public static final Crater INSTANCE = new Crater();

    public static final MapCodec<Crater> MAP_CODEC = MapCodec.unit(INSTANCE);

    public static final KeyDispatchDataCodec<Crater> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        // Scale for crater distribution (smaller = more frequent, smaller craters)
        double scale = 64.0;
        double scaledX = x / scale;
        double scaledZ = z / scale;

        // Find the grid cell containing this point
        int cellX = (int) Math.floor(scaledX);
        int cellZ = (int) Math.floor(scaledZ);

        double minDist = Double.MAX_VALUE;

        // Check neighboring cells (3x3 grid)
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int neighborCellX = cellX + offsetX;
                int neighborCellZ = cellZ + offsetZ;

                // Generate random point within this cell
                long seed = hashCell(neighborCellX, neighborCellZ);
                double randomX = neighborCellX + lcgRandom(seed);
                double randomZ = neighborCellZ + lcgRandom(seed + 1);

                // Vary cell size based on the cell's seed (smaller variation range)
                double cellSizeVariation = 0.7 + lcgRandom(seed + 2) * 0.6; // 0.7 to 1.3x size

                // Calculate distance to this cell's point
                double dx = (scaledX - randomX) / cellSizeVariation;
                double dz = (scaledZ - randomZ) / cellSizeVariation;
                double dist = dx * dx + dz * dz;

                minDist = Math.min(minDist, dist);
            }
        }

        double crater = 12 * minDist;

        return 0.05 * (crater - 0.5) / Math.max(1.0, crater * crater * crater);
    }

    // Hash function for generating consistent random values per cell
    private long hashCell(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    // Simple linear congruential generator for [0, 1) random values
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
