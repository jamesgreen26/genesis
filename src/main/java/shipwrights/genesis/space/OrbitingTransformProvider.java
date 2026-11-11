package shipwrights.genesis.space;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.MoreCodecs;

import java.util.Optional;

/**
 * Places a celestial on an elliptical orbit around its parent, described by classical orbital elements.
 * <p>
 * An uninclined orbit lies in the XZ plane and, for a positive period, moves from +X towards +Z.
 * All angles are in degrees, distances in blocks and times in ticks.
 *
 * @param parent               celestial to orbit
 * @param distance             semi-major axis; the radius for circular orbits
 * @param period               ticks per orbit, negative for retrograde
 * @param eccentricity         0 for a circle, approaching 1 for increasingly stretched ellipses
 * @param inclination          tilt of the orbital plane
 * @param inclinationDirection direction, around Y from +X, of the line the orbital plane tilts about
 *                             (longitude of the ascending node)
 * @param periapsisAngle       angle within the orbital plane from the ascending node to the closest approach
 * @param startAngle           angle past periapsis at tick 0 (true anomaly)
 * @param rotation             spin of the celestial
 */
public record OrbitingTransformProvider(
        ResourceLocation parent,
        double distance,
        double period,
        double eccentricity,
        double inclination,
        double inclinationDirection,
        double periapsisAngle,
        double startAngle,
        Rotation rotation
) implements Celestial.TransformProvider {

    static ResourceLocation TYPE = GenesisMod.id("orbiting");

    private static final int MAX_ITERATIONS = 32;
    private static final double TOLERANCE = 1e-12;

    /**
     * @param dayLength  ticks between the parent returning to the same place in the sky. Positive spins it east, as
     *                   {@link VantagePoint} has it, so the parent rises in the east; negative the other way. Empty when
     *                   tidally locked
     * @param axialTilt  tilt of the spin axis away from the orbit normal, about the ascending node
     * @param startAngle spin at tick 0, measured from the -Z face pointing at the parent
     */
    public record Rotation(Optional<Double> dayLength, double axialTilt, double startAngle) {
        public static final Rotation TIDALLY_LOCKED = new Rotation(Optional.empty(), 0.0, 0.0);

        private static final String TIDALLY_LOCKED_NAME = "tidally_locked";

        private static final Codec<Optional<Double>> DAY_LENGTH_CODEC = Codec.either(Codec.DOUBLE, Codec.STRING).comapFlatMap(
                either -> either.map(
                        dayLength -> DataResult.success(Optional.of(dayLength)),
                        name -> name.equals(TIDALLY_LOCKED_NAME)
                                ? DataResult.success(Optional.empty())
                                : DataResult.error(() -> "Expected a day length or \"" + TIDALLY_LOCKED_NAME + "\", got \"" + name + "\"")
                ),
                dayLength -> dayLength.<Either<Double, String>>map(Either::left).orElseGet(() -> Either.right(TIDALLY_LOCKED_NAME))
        );

        public static final Codec<Rotation> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        DAY_LENGTH_CODEC.fieldOf("dayLength").forGetter(Rotation::dayLength),
                        MoreCodecs.strictOptionalField(Codec.DOUBLE, "axialTilt", 0.0).forGetter(Rotation::axialTilt),
                        MoreCodecs.strictOptionalField(Codec.DOUBLE, "startAngle", 0.0).forGetter(Rotation::startAngle)
                ).apply(instance, Rotation::new)
        );
    }

    @Override
    public Vector3dc getPosition(Registry<Celestial> celestials, Time time) {
        Celestial parentCelestial = getParent(celestials);
        Vector3dc parentPosition = parentCelestial.transformProvider().getPosition(celestials, time);

        double eccentricAnomaly = solveKepler(getMeanAnomaly(time), eccentricity);
        Vector3d position = new Vector3d(
                distance * (Math.cos(eccentricAnomaly) - eccentricity),
                0.0,
                distance * Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(eccentricAnomaly)
        );

        return getOrbitalPlane().rotateY(-Math.toRadians(periapsisAngle)).transform(position).add(parentPosition);
    }

    @Override
    public Quaterniondc getRotation(Registry<Celestial> celestials, Time time) {
        // Tidally locked bodies turn once per orbit, in the same sense, following the mean longitude so the same face
        // points at the parent. The extra quarter turn points -Z at the parent rather than +X away from it
        double angle = Math.PI / 2.0 - Math.toRadians(periapsisAngle + rotation.startAngle()) - getMeanAnomaly(time);

        // A day turns it from there the other way, right-handed about +Y, which is east on its surface
        if (rotation.dayLength().isPresent()) {
            double dayLength = rotation.dayLength().get();
            angle += 2.0 * Math.PI * (time.simple() % dayLength) / dayLength;
        }

        return getOrbitalPlane()
                .rotateX(-Math.toRadians(rotation.axialTilt()))
                .rotateY(angle);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }

    private Celestial getParent(Registry<Celestial> celestials) {
        Celestial parentCelestial = celestials.get(parent);
        if (parentCelestial == null) {
            throw new IllegalStateException("Orbiting celestial references unknown parent " + parent);
        }
        return parentCelestial;
    }

    /**
     * Rotates the XZ plane onto the orbital plane, with the ascending node along +X.
     */
    private Quaterniond getOrbitalPlane() {
        return new Quaterniond()
                .rotateY(-Math.toRadians(inclinationDirection))
                .rotateX(-Math.toRadians(inclination));
    }

    private double getMeanAnomaly(Time time) {
        // Wrap before dividing so precision holds for large game times
        double meanMotion = 2.0 * Math.PI * (time.simple() % period) / period;
        return getStartMeanAnomaly() + meanMotion;
    }

    private double getStartMeanAnomaly() {
        double trueAnomaly = Math.toRadians(startAngle);
        double eccentricAnomaly = 2.0 * Math.atan2(
                Math.sqrt(1.0 - eccentricity) * Math.sin(trueAnomaly / 2.0),
                Math.sqrt(1.0 + eccentricity) * Math.cos(trueAnomaly / 2.0)
        );
        return eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly);
    }

    /**
     * Solves Kepler's equation {@code M = E - e sin E} for the eccentric anomaly {@code E} using Newton's method.
     */
    private static double solveKepler(double meanAnomaly, double eccentricity) {
        double m = Math.IEEEremainder(meanAnomaly, 2.0 * Math.PI);
        if (eccentricity == 0.0) {
            return m;
        }

        // Starting guess from Danby, "Fundamentals of Celestial Mechanics", converges for all elliptical orbits
        double e = m + 0.85 * eccentricity * Math.signum(Math.sin(m));
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double step = (e - eccentricity * Math.sin(e) - m) / (1.0 - eccentricity * Math.cos(e));
            e -= step;
            if (Math.abs(step) <= TOLERANCE) {
                break;
            }
        }
        return e;
    }

    private static DataResult<OrbitingTransformProvider> validate(OrbitingTransformProvider provider) {
        if (!(provider.distance > 0.0)) {
            return DataResult.error(() -> "Orbit distance must be positive, got " + provider.distance);
        }
        if (!(provider.eccentricity >= 0.0 && provider.eccentricity < 1.0)) {
            return DataResult.error(() -> "Orbit eccentricity must be at least 0 and less than 1, got " + provider.eccentricity);
        }
        if (provider.period == 0.0) {
            return DataResult.error(() -> "Orbit period must not be 0");
        }
        if (provider.rotation.dayLength().filter(dayLength -> dayLength == 0.0).isPresent()) {
            return DataResult.error(() -> "Day length must not be 0");
        }
        return DataResult.success(provider);
    }

    // Validated as a MapCodec so it stays a MapCodecCodec: the type dispatch codec only inlines fields alongside "type" for
    // MapCodecCodecs, and otherwise looks for them under a separate "value" key
    public static final Codec<OrbitingTransformProvider> CODEC = RecordCodecBuilder.<OrbitingTransformProvider>mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("parent").forGetter(OrbitingTransformProvider::parent),
                    Codec.DOUBLE.fieldOf("distance").forGetter(OrbitingTransformProvider::distance),
                    Codec.DOUBLE.fieldOf("period").forGetter(OrbitingTransformProvider::period),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "eccentricity", 0.0).forGetter(OrbitingTransformProvider::eccentricity),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "inclination", 0.0).forGetter(OrbitingTransformProvider::inclination),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "inclinationDirection", 0.0).forGetter(OrbitingTransformProvider::inclinationDirection),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "periapsisAngle", 0.0).forGetter(OrbitingTransformProvider::periapsisAngle),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "startAngle", 0.0).forGetter(OrbitingTransformProvider::startAngle),
                    MoreCodecs.strictOptionalField(Rotation.CODEC, "rotation", Rotation.TIDALLY_LOCKED).forGetter(OrbitingTransformProvider::rotation)
            ).apply(instance, OrbitingTransformProvider::new)
    ).flatXmap(OrbitingTransformProvider::validate, OrbitingTransformProvider::validate).codec();
}
