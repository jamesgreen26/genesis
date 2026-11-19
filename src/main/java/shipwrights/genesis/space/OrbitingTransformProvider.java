package shipwrights.genesis.space;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.MoreCodecs;
import shipwrights.genesis.util.schema.Schema;

import java.math.BigDecimal;
import java.util.List;
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

    /// What a new orbit starts out around
    private static final ResourceLocation DEFAULT_PARENT = GenesisMod.id("sun");

    /// What a new orbit's period and day length start as, in ticks: a Minecraft day, rather than an instant
    private static final double ONE_DAY = 24000;

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

        private static final Schema.NumberSchema DAY_LENGTH_SCHEMA =
                new Schema.NumberSchema(Schema.NumberKind.DOUBLE, Schema.Range.nonZero(), BigDecimal.valueOf(ONE_DAY));

        // Declared as a whole: probing the either would stop at the number, which declares itself, and never see the word
        private static final Codec<Optional<Double>> DAY_LENGTH_CODEC = MoreCodecs.declaring(
                Codec.either(MoreCodecs.ranged(Codec.DOUBLE, DAY_LENGTH_SCHEMA, BigDecimal::doubleValue),
                                MoreCodecs.oneOf(TIDALLY_LOCKED_NAME))
                        .xmap(
                                either -> either.map(Optional::of, name -> Optional.empty()),
                                dayLength -> dayLength.<Either<Double, String>>map(Either::left)
                                        .orElseGet(() -> Either.right(TIDALLY_LOCKED_NAME))),
                new Schema.UnionSchema(List.of(DAY_LENGTH_SCHEMA, new Schema.ChoiceSchema(List.of(TIDALLY_LOCKED_NAME)))),
                Optional.empty());

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

    // A MapCodec, so the type dispatch codec inlines its fields alongside "type"
    public static final MapCodec<OrbitingTransformProvider> CODEC = RecordCodecBuilder.<OrbitingTransformProvider>mapCodec(instance ->
            instance.group(
                    MoreCodecs.registryId(Celestial.REGISTRY_KEY, DEFAULT_PARENT).fieldOf("parent").forGetter(OrbitingTransformProvider::parent),
                    MoreCodecs.doubleRange(Schema.Range.above(0)).fieldOf("distance").forGetter(OrbitingTransformProvider::distance),
                    MoreCodecs.doubleRange(Schema.Range.nonZero(), ONE_DAY).fieldOf("period").forGetter(OrbitingTransformProvider::period),
                    MoreCodecs.strictOptionalField(MoreCodecs.doubleRange(Schema.Range.from(0, 1)), "eccentricity", 0.0).forGetter(OrbitingTransformProvider::eccentricity),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "inclination", 0.0).forGetter(OrbitingTransformProvider::inclination),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "inclinationDirection", 0.0).forGetter(OrbitingTransformProvider::inclinationDirection),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "periapsisAngle", 0.0).forGetter(OrbitingTransformProvider::periapsisAngle),
                    MoreCodecs.strictOptionalField(Codec.DOUBLE, "startAngle", 0.0).forGetter(OrbitingTransformProvider::startAngle),
                    MoreCodecs.strictOptionalField(Rotation.CODEC, "rotation", Rotation.TIDALLY_LOCKED).forGetter(OrbitingTransformProvider::rotation)
            ).apply(instance, OrbitingTransformProvider::new)
    );
}
