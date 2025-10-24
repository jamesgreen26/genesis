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

    private final double cellSize;
    private final double intensity;

    public static final MapCodec<Crater> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.DOUBLE.fieldOf("cell_size").forGetter(crater -> crater.cellSize),
                    com.mojang.serialization.Codec.DOUBLE.fieldOf("intensity").forGetter(crater -> crater.intensity)
            ).apply(instance, Crater::new)
    );

    public static final KeyDispatchDataCodec<Crater> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /**
     * Create a crater density function (2D in XZ plane)
     * @param cellSize Size of each Worley cell (larger = fewer, bigger craters)
     * @param intensity Multiplier for the crater depth effect
     */
    public Crater(double cellSize, double intensity) {
        this.cellSize = cellSize;
        this.intensity = intensity;
    }

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX();
        double z = context.blockZ();
        // Y is ignored - craters are 2D in the XZ plane

        // Find distance to nearest cell center using 2D Worley noise approach
        double dist = findNearestCellDistance(x, z);

        // Normalize distance by cell size
        double normalizedDist = dist / cellSize;

        // Apply crater formula: -(0.5 - abs(pow(dist, 3))) / max(pow(dist, 6), 1)
        double dist3 = normalizedDist * normalizedDist * normalizedDist;
        double dist6 = dist3 * dist3;

        double numerator = -(0.5 - Math.abs(dist3));
        double denominator = Math.max(dist6, 1.0);

        return (numerator / denominator) * intensity;
    }

    /**
     * Find the distance to the nearest Worley cell center in 2D space (XZ plane)
     */
    private double findNearestCellDistance(double x, double z) {
        // Determine which cell we're in
        int cellX = (int) Math.floor(x / cellSize);
        int cellZ = (int) Math.floor(z / cellSize);

        double minDistSq = Double.MAX_VALUE;

        // Check neighboring cells (3x3 grid in XZ plane)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int neighborX = cellX + dx;
                int neighborZ = cellZ + dz;

                // Generate deterministic random position within the cell
                double[] cellCenter = getCellCenter(neighborX, neighborZ);

                // Calculate 2D distance to this cell center
                double deltaX = x - cellCenter[0];
                double deltaZ = z - cellCenter[1];
                double distSq = deltaX * deltaX + deltaZ * deltaZ;

                minDistSq = Math.min(minDistSq, distSq);
            }
        }

        return Math.sqrt(minDistSq);
    }

    /**
     * Get the center point of a 2D cell using a deterministic hash function
     */
    private double[] getCellCenter(int cellX, int cellZ) {
        // Use simple hash function for deterministic randomness
        long seed = hash(cellX, cellZ);

        // Generate random offset within the cell using the seed
        double offsetX = cellX * cellSize + (randomDouble(seed, 0) * cellSize);
        double offsetZ = cellZ * cellSize + (randomDouble(seed, 1) * cellSize);

        return new double[]{offsetX, offsetZ};
    }

    /**
     * Simple 2D hash function for deterministic randomness
     */
    private long hash(int x, int z) {
        long h = x * 374761393L + z * 1274126177L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return h ^ (h >> 16);
    }

    /**
     * Generate a deterministic random double in [0, 1) from a seed and component
     */
    private double randomDouble(long seed, int component) {
        long h = seed + component * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return (h & 0x7FFFFFFFL) / (double) 0x80000000L;
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

    @Override
    public double minValue() {
        return -intensity;
    }

    @Override
    public double maxValue() {
        return intensity * 0.5;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    public double getCellSize() {
        return cellSize;
    }

    public double getIntensity() {
        return intensity;
    }
}
