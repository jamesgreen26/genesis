package shipwrights.genesis.space.transformProvider;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
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

    // Configurable orbital parameters (from OrbitingBody)
    private final double orbitDistance;
    private final double orbitTime;
    private final double eccentricity;
    private final double eccentricArgument;
    private final double inclination;
    private final double inclinedArgument;
    private final double initialOrbitProgress;

    //rotation stuff
    private final double baseRotationX;
    private final double baseRotationY;
    private final double baseRotationZ;
    private final Quaterniond baseRotation;
    private final double dayLength;
    private final double axialTilt;
    private final double tiltArgument;

    /**
     * Creates an orbiting transform provider with specified orbital parameters.
     * The seed is used to generate random orbital angles and rotation.
     * It is all relative to the equator of the parent body(if the parent body rotates, if not then just the xz plane)
     *
     * @param parentID the parent celestial body to orbit around
     * @param orbitDistance the average orbit radius in blocks
     * @param orbitTime the orbit period in ticks
     * @param eccentricity the eccentricty of the orbit
     * @param eccentricArgument which direction the orbit is stretched in, angle in radians
     * @param inclination how tilted the orbit is, in radians
     * @param inclinedArgument which direction the orbit is tilted in, angle in radians
     * @param initialOrbitProgress where on the orbit the planet starts, in radians
     * @param baseRotationX the initial rotation of the planet along x(radians)
     * @param baseRotationY the initial rotation of the planet along y(radians)
     * @param baseRotationZ the initial rotation of the planet along z(radians)
     * @param dayLength the day length in ticks
     * @param axialTilt axial tilt of the planet in radians
     * @param tiltArgument which direction the axial tilt is in, angle in radians
     */
    public OrbitingTransformProvider(ResourceLocation parentID, double orbitDistance, double orbitTime, double eccentricity, double eccentricArgument, double inclination, double inclinedArgument, double initialOrbitProgress, double baseRotationX, double baseRotationY, double baseRotationZ, double dayLength, double axialTilt, double tiltArgument) {
        if(eccentricity < 0.0 || eccentricity >= 1.0) {
            throw new IllegalStateException("Eccentricity must be between 0(inclusive) and 1(not inclusive)");
        }
        this.parentID = parentID;
        this.orbitDistance = orbitDistance;
        this.orbitTime = orbitTime;
        this.eccentricity = eccentricity;
        this.eccentricArgument = eccentricArgument;
        this.inclination = inclination;
        this.inclinedArgument = inclinedArgument;
        this.initialOrbitProgress = initialOrbitProgress;
        this.baseRotationX = baseRotationX;
        this.baseRotationY = baseRotationY;
        this.baseRotationZ = baseRotationZ;
        this.baseRotation = new Quaterniond().rotationXYZ(baseRotationX,baseRotationY,baseRotationZ);
        this.dayLength = dayLength;
        this.axialTilt = axialTilt;
        this.tiltArgument = tiltArgument;
    }

    private Celestial getParent() {
        return GenesisMod.SPACE_REGISTRY.get(parentID);
    }

    private int getYearLengthTicks() {
        return (int)(this.orbitTime);
    }

    @Override
    public Quaterniondc getRotation(long ticks, float subticks) {
        double rotationPeriod = this.orbitTime / (this.orbitTime / this.dayLength + 1.0);
        double ang = (ticks + subticks) / rotationPeriod * Math.PI * 2.0;
        if (this.dayLength == 0.0) {
            ang = (ticks + subticks) / this.orbitTime * Math.PI * 2.0;
        }
        Quaterniond rotation = new Quaterniond(this.baseRotation);
        rotation.rotateAxis(ang,new Vector3d(0.0,-1.0,0.0).rotateX(this.axialTilt).rotateY(this.tiltArgument));
        if(getParent().transformProvider() instanceof OrbitingTransformProvider o) {
            rotation.mul(o.orbitRelativeOrientation());
        }
        return rotation;
    }

    public Quaterniond orbitRelativeOrientation() {
        Quaterniond rotation = new Quaterniond().rotateX(this.axialTilt).rotateY(this.tiltArgument);
        rotation.invert();
        if(getParent().transformProvider() instanceof OrbitingTransformProvider o) {
            rotation.mul(o.orbitRelativeOrientation());
        }
        return rotation;
    }

    @Override
    public Vector3d getPosition(long ticks, float subticks) {
        double meanAnomaly = Math.PI * 2.0 * ((ticks + subticks) / this.orbitTime) + this.initialOrbitProgress;
        double eccentricAnomaly = meanAnomaly;
        if(this.eccentricity > 0.8) {eccentricAnomaly = Math.PI;}
        for(int n = 0; n < 64; n++) {
            eccentricAnomaly -= (eccentricAnomaly - this.eccentricity * Math.sin(eccentricAnomaly - this.eccentricArgument) - meanAnomaly) / (1.0 - this.eccentricity * Math.cos(eccentricAnomaly - this.eccentricArgument));
        }
        double trueAnomaly = Math.atan(Math.sqrt((1.0 + this.eccentricity) / (1.0 - this.eccentricity)) * Math.tan(eccentricAnomaly / 2.0)) * 2.0;
        double radialDistance = this.orbitDistance * (1.0 - this.eccentricity * this.eccentricity) / (1.0 + this.eccentricity * Math.cos(trueAnomaly - this.eccentricArgument));
        Vector3d pos = new Vector3d(Math.cos(trueAnomaly) * radialDistance,0.0,Math.sin(trueAnomaly) * radialDistance);
        pos.rotate(new Quaterniond().rotateAxis(this.inclination,new Vector3d(0.0,0.0,1.0).rotateY(this.inclinedArgument)));
        if(getParent().transformProvider() instanceof OrbitingTransformProvider o) {
            pos.rotate(o.orbitRelativeOrientation());
        }
        pos.add(getParent().getPosition(ticks,subticks));
        return pos;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    // Codec for serialization/deserialization
    public static final Codec<OrbitingTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("parentID").forGetter(p -> p.parentID),
                    Codec.DOUBLE.fieldOf("orbitDistance").forGetter(p -> p.orbitDistance),
                    Codec.DOUBLE.fieldOf("orbitTime").forGetter(p -> p.orbitTime),
                    Codec.DOUBLE.optionalFieldOf("eccentricity",0.0).forGetter(p -> p.eccentricity),
                    Codec.DOUBLE.optionalFieldOf("eccentricArgument",0.0).forGetter(p -> p.eccentricArgument),
                    Codec.DOUBLE.optionalFieldOf("inclination",0.0).forGetter(p -> p.inclination),
                    Codec.DOUBLE.optionalFieldOf("inclinedArgument",0.0).forGetter(p -> p.inclinedArgument),
                    Codec.DOUBLE.optionalFieldOf("initialOrbitProgress",0.0).forGetter(p -> p.initialOrbitProgress),
                    Codec.DOUBLE.optionalFieldOf("baseRotationX",0.0).forGetter(p -> p.baseRotationX),
                    Codec.DOUBLE.optionalFieldOf("baseRotationY",0.0).forGetter(p -> p.baseRotationY),
                    Codec.DOUBLE.optionalFieldOf("baseRotationZ",0.0).forGetter(p -> p.baseRotationZ),
                    Codec.DOUBLE.optionalFieldOf("dayLength", 24000.0).forGetter(p -> p.dayLength),
                    Codec.DOUBLE.optionalFieldOf("axialTilt",0.0).forGetter(p -> p.axialTilt),
                    Codec.DOUBLE.optionalFieldOf("tiltArgument",0.0).forGetter(p -> p.tiltArgument)
            ).apply(instance, OrbitingTransformProvider::new)
    );

    // Example registration method (call this during mod initialization)
    public static void register() {
        CelestialTransformProvider.register(TYPE, CODEC);
    }
}
