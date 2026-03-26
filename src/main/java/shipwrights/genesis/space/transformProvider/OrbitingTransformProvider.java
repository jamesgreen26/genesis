package shipwrights.genesis.space.transformProvider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.space.Celestial;

import java.util.Random;

/**
 * Transform provider that simulates orbital mechanics for celestial bodies.
 * <br>
 * Uses the seed parameter to generate random but deterministic values for
 * orbital angles and base rotation (matching OrbitingBody behavior).
 */
public class OrbitingTransformProvider implements CelestialTransformProvider {
    public static final ResourceLocation TYPE = ResourceLocation.parse("genesis:orbiting");

    private final ResourceLocation parentID;
    private final int seed;

    // Configurable orbital parameters (from OrbitingBody)
    private final double orbitDistance;
    private final double orbitTime;
    private final double dayLength;

    // Random parameters derived from seed
    private final double orbitalTheta;
    private final double orbitalPhi;
    private final Quaterniondc baseRotation;

    /**
     * Creates an orbiting transform provider with specified orbital parameters.
     * The seed is used to generate random orbital angles and rotation.
     *
     * @param parentID the parent celestial body to orbit around
     * @param seed the seed for generating deterministic random orbital angles and rotation
     * @param orbitDistance the orbit radius in blocks
     * @param orbitTime the orbit period in ticks
     * @param dayLength the day length in ticks
     */
    public OrbitingTransformProvider(ResourceLocation parentID, int seed, double orbitDistance, double orbitTime, double dayLength) {
        this.parentID = parentID;
        this.seed = seed;
        this.orbitDistance = orbitDistance;
        this.orbitTime = orbitTime;
        this.dayLength = dayLength;

        // Generate random parameters from seed (similar to OrbitingBody)
        Random rand = new Random(seed);

        // Advance RNG like OrbitingBody does for consistency
        for (int i = 0; i < rand.nextInt(10); i++) {
            rand.nextDouble();
        }

        // Generate random base rotation
        this.baseRotation = new Quaterniond();
//        this.baseRotation = new Quaterniond().rotationXYZ(
//            rand.nextDouble(Math.PI),
//            rand.nextDouble(Math.PI),
//            rand.nextDouble(Math.PI)
//        );

        // Generate random orbital angles (spherical coordinates)
        this.orbitalTheta = rand.nextDouble() * 2 * Math.PI;   // longitude
        this.orbitalPhi = Math.PI / 2;
        //this.orbitalPhi = (Math.acos(2 * rand.nextDouble() - 1) + Math.PI) / 3; // latitude
    }

    private Celestial getParent(Registry<Celestial> registry) {
        Celestial parent = registry.get(parentID);
        if (parent == null) throw new IllegalStateException("Parent celestial not found in registry: " + parentID);
        return parent;
    }

    private int getYearLengthTicks() {
        return (int)(this.orbitTime);
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry) {
        if (this.dayLength == 0.0) {
            // Tidally locked: -Z side always faces the parent
            Vector3d myPos = getPosition(ticks, subticks, registry);
            Vector3d parentPos = new Vector3d(getParent(registry).getPosition(ticks, subticks, registry));
            Vector3d toParent = parentPos.sub(myPos, new Vector3d()).normalize();
            // Rotate so that local -Z points toward parent
            return new Quaterniond().rotateTo(new Vector3d(0, 0.8, -0.5).normalize(), toParent);
        }

        // Normal rotation: daily spin around Y axis (similar to OrbitingBody)
        double rotationalPeriod = (this.orbitTime / (this.orbitTime / this.dayLength - 1.0));
        return new Quaterniond(baseRotation).rotateY(
            -Math.PI * 2 * (ticks + subticks) / rotationalPeriod
        );
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry) {
        // Calculate orbital position (similar to OrbitingBody.getCurrentPos)
        Vector3d out = new Vector3d(1, 0, 0);

        int yearLength = getYearLengthTicks();

        if (yearLength <= 0) {
            throw new IllegalStateException("YearLength should be > 0");
        }

        // Rotate by orbital progression
        out = out.rotateY(Math.PI * 2 * (ticks + subticks) / yearLength);

        // Apply orbital angles
        out = out.rotateY(orbitalTheta);
        out = out.rotateX(orbitalPhi + Math.PI / 2);

        // Scale to orbit distance
        out.normalize(orbitDistance);

        // Add parent's position
        return out.add(getParent(registry).getPosition(ticks, subticks, registry), new Vector3d()).setComponent(1, 0);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    // Codec for serialization/deserialization
    public static final Codec<OrbitingTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("parentID").forGetter(p -> p.parentID),
                    Codec.INT.fieldOf("seed").forGetter(p -> p.seed),
                    Codec.DOUBLE.fieldOf("orbitDistance").forGetter(p -> p.orbitDistance),
                    Codec.DOUBLE.fieldOf("orbitTime").forGetter(p -> p.orbitTime),
                    Codec.DOUBLE.optionalFieldOf("dayLength", 1.0).forGetter(p -> p.dayLength)
            ).apply(instance, OrbitingTransformProvider::new)
    );

    // Example registration method (call this during mod initialization)
    public static void register() {
        CelestialTransformProvider.register(TYPE, CODEC);
    }
}
