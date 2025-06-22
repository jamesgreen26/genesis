package g_mungus.genesis.worldgen;


import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class AsteroidBelt implements DensityFunction {

    public static final AsteroidBelt INSTANCE = new AsteroidBelt();

    public static final MapCodec<AsteroidBelt> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<AsteroidBelt> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private AsteroidBelt() {}

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX();
        double y = context.blockY() - 100;
        double z = context.blockZ();

        double majorRadius = 15000.0; // distance from center to tube center
        double minorRadius = 470.0;    // radius of tube

        double q = Math.sqrt(x * x + z * z) - majorRadius;
        double dist = Math.sqrt(q * q + y * y) - minorRadius;

        // Return positive density near torus surface, zero elsewhere
        return Math.min(Math.max(0.0, Math.pow(-dist / 2000, 3)), 0.1);
    }

    @Override
    public void fillArray(double[] arr, ContextProvider provider) {
        for (int i = 0; i < arr.length; i++) {
            FunctionContext ctx = provider.forIndex(i);
            arr[i] = compute(ctx);
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return this; // No children to map
    }

    @Override public double minValue() { return 0.0; }
    @Override public double maxValue() { return 1.0; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}

