package shipwrights.dataplanets.systemCreation.dimension.noise;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;
import shipwrights.dataplanets.DataplanetsMod;

public class AlienDensityFunction implements DensityFunction.SimpleFunction {

    private final long seed;
    private final double worldScale;
    private static final AlienDensityFunction INSTANCE = new AlienDensityFunction();


    public static final MapCodec<AlienDensityFunction> MAP_CODEC = MapCodec.unit(INSTANCE);

    public static final KeyDispatchDataCodec<AlienDensityFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    public static ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath(DataplanetsMod.MOD_ID, "alien");
    }

    public AlienDensityFunction(long seed, double worldScale) {
        this.seed = seed;
        this.worldScale = worldScale;
    }

    public AlienDensityFunction() {
        this(8675309L, 512);
    }

    @Override
    public double compute(FunctionContext ctx) {
        // Scale horizontal coordinates for feature size
        double x = ctx.blockX() / worldScale;
        double z = ctx.blockZ() / worldScale;

        double y = 0;

        double density = 0;


        // -------- Domain Warp (low frequency for large-scale distortion) --------
        double warpX = fbmNoise(x * 0.5 + 31.5, 0, z * 0.5 + 123.4, 3, 0.5) * 0.4;
        double warpZ = fbmNoise(x * 0.5 + 210.0, 0, z * 0.5 + 11.0, 3, 0.5) * 0.4;

        double wx = x + warpX;
        double wz = z + warpZ;

        // -------- Biome/Region Selection (very low frequency) --------
        double biomeNoise = fbmNoise(x * 0.3, 0.0, z * 0.3, 2, 0.5);

        // -------- Low Frequency Mountains (large-scale terrain) --------
        double mountains = fbmNoise(wx * 1.0, 0, wz * 1.0, 4, 0.6);
        mountains = Math.pow(Math.max(0, mountains * 0.5 + 0.5), 1.5) * 1.2;

        // -------- Mid Frequency Hills (medium-scale variation) --------
        double hills = fbmNoise(wx * 2.0, y * 0.5, wz * 2.0, 4, 0.55) * 0.4;

        // -------- Ridged Features (alien spires and mesas) --------
        double ridges = ridgedNoise(wx * 1.5, y * 2.0, wz * 1.5, 4);
        ridges = Math.pow(ridges, 1.5) * 0.8;

        // -------- Plateaus and Mesas (terraced features) --------
        double plateauBase = fbmNoise(wx * 0.8, 0.0, wz * 0.8, 3, 0.5);
        double plateauHeight = Math.floor(plateauBase * 5.0) / 5.0; // Create steps
        double plateaus = plateauHeight * 0.6;

        // -------- Terraces (horizontal layering) --------
        double terraceNoise = fbmNoise(wx * 1.2, 0.0, wz * 1.2, 2, 0.5);
        double terraces = Math.sin((y + terraceNoise * 0.3) * Math.PI * 6.0) * 0.08;

        // -------- Blend terrain features based on biome --------
        // Biome < -0.3: Rolling hills
        // Biome -0.3 to 0.3: Mixed terrain with ridges
        // Biome > 0.3: Plateaus and mesas

        double terrain;
        if (biomeNoise < -0.3) {
            // Rolling hills biome
            terrain = mountains * 0.8 + hills;
        } else if (biomeNoise > 0.3) {
            // Plateau/mesa biome
            terrain = plateaus + ridges * 0.3 + terraces;
        } else {
            // Mixed ridged terrain
            double blend = (biomeNoise + 0.3) / 0.6;
            double hillTerrain = mountains * 0.8 + hills;
            double ridgeTerrain = plateaus + ridges * 0.5 + terraces;
            terrain = lerp(hillTerrain, ridgeTerrain, blend);
        }

        // Add terrain to base density
        density += terrain;

        // -------- High Frequency Detail (fine surface details) --------
        double detail1 = fbmNoise(wx * 4.0, y * 2.0, wz * 4.0, 3, 0.5) * 0.15;
        double detail2 = fbmNoise(wx * 8.0, y * 4.0, wz * 8.0, 2, 0.45) * 0.08;
        density += detail1 + detail2;

        // -------- Caves and Overhangs (3D noise for hollow spaces) --------
        double cave = fbmNoise(wx * 6.0, y * 4.0, wz * 6.0, 3, 0.6);
        cave = Math.abs(cave);
        // Only carve caves where cave noise is very low (creates voids)
        if (cave < 0.2) {
            density -= (0.2 - cave) * 4.0; // Strong negative density for caves
        }

        // -------- Hard Floor (ensure ground exists below certain height) --------
        // y = -1 is roughly at block y=0
        if (y < -1.5) {
            double floorBoost = (-1.5 - y) * 4.0;
            density += floorBoost;
        }

        // -------- Soft Sky Gradient (encourage open air above) --------
        if (y > 1.0) {
            density -= (y - 1.0) * 3.0;
        }

        // Clamp to safe range
        return clamp(density, -4.0, 4.0);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider provider) {
        for (int i = 0; i < ds.length; i++) {
            ds[i] = compute(provider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public double minValue() {
        return -4.0;
    }

    @Override
    public double maxValue() {
        return 4.0;
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    // -------- Utility Noise Functions --------

    private double noise(double x, double y, double z) {
        // Proper interpolated value noise
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;

        // Smooth interpolation (smoothstep)
        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        // Hash the 8 corners of the cube
        double c000 = hash(xi, yi, zi);
        double c100 = hash(xi + 1, yi, zi);
        double c010 = hash(xi, yi + 1, zi);
        double c110 = hash(xi + 1, yi + 1, zi);
        double c001 = hash(xi, yi, zi + 1);
        double c101 = hash(xi + 1, yi, zi + 1);
        double c011 = hash(xi, yi + 1, zi + 1);
        double c111 = hash(xi + 1, yi + 1, zi + 1);

        // Trilinear interpolation
        double x00 = lerp(c000, c100, u);
        double x10 = lerp(c010, c110, u);
        double x01 = lerp(c001, c101, u);
        double x11 = lerp(c011, c111, u);

        double y0 = lerp(x00, x10, v);
        double y1 = lerp(x01, x11, v);

        return lerp(y0, y1, w);
    }

    private double hash(int x, int y, int z) {
        // Hash integer coordinates to a random value in [-1, 1]
        long n = x * 1619L + y * 31337L + z * 6971L + seed;
        n = (n << 13) ^ n;
        return ((n * (n * n * 60493L + 19990303L) + 1376312589L) & 0x7fffffffL) / 1073741824.0 - 1.0;
    }

    private double fade(double t) {
        // Smoothstep function: 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double fbmNoise(double x, double y, double z, int octaves, double persistence) {
        double total = 0.0, amplitude = 1.0, frequency = 1.0, max = 0.0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2.01; // slightly irrational to reduce repetition
        }
        return total / max;
    }

    private double ridgedNoise(double x, double y, double z, int octaves) {
        double total = 0.0, amplitude = 1.0, frequency = 1.0, prev = 1.0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(noise(x * frequency, y * frequency, z * frequency));
            n *= n;
            total += n * amplitude * prev;
            prev = n;
            amplitude *= 0.55;
            frequency *= 2.03;
        }
        return total;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : (Math.min(v, max));
    }
}
