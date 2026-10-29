package shipwrights.genesis.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.util.MoreCodecs;

/**
 * Places a celestial on a two-body Keplerian orbit around its parent, defined by its position and velocity
 * relative to the parent at tick 0. Rotation starts at {@code initialRotation} (XYZ euler angles, radians) and
 * spins at a constant {@code rotationalVelocity} (axis-angle vector, radians per tick).
 * <p>
 * Distances are in blocks, time in ticks, and velocities in blocks per tick.
 */
public record OrbitingTransformProvider(
        ResourceLocation parent,
        Vector3dc initialRelativePosition,
        Vector3dc initialLinearVelocity,
        Vector3dc initialRotation,
        Vector3dc rotationalVelocity
) implements Celestial.TransformProvider{

    static ResourceLocation TYPE = GenesisMod.id("orbiting");

    private static final int MAX_ITERATIONS = 64;
    private static final double TOLERANCE = 1e-10;

    @Override
    public Vector3dc getPosition(Registry<Celestial> celestials, Time time) {
        Celestial parentCelestial = getParent(celestials);
        Vector3dc parentPosition = parentCelestial.transformProvider().getPosition(celestials, time);

        return getRelativePosition(parentCelestial.mass(), time.simple()).add(parentPosition);
    }

    @Override
    public Quaterniondc getRotation(Registry<Celestial> celestials, Time time) {
        Quaterniond rotation = new Quaterniond().rotateXYZ(initialRotation.x(), initialRotation.y(), initialRotation.z());

        double speed = rotationalVelocity.length();
        if (speed == 0.0) {
            return rotation;
        }

        double angle = (speed * time.simple()) % (2.0 * Math.PI);
        return new Quaterniond()
                .fromAxisAngleRad(
                        rotationalVelocity.x() / speed,
                        rotationalVelocity.y() / speed,
                        rotationalVelocity.z() / speed,
                        angle
                )
                .mul(rotation);
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
     * Propagates the initial state vector forward by {@code dt} using the universal variable formulation,
     * which handles elliptical, parabolic and hyperbolic orbits alike.
     */
    private Vector3d getRelativePosition(double mu, double dt) {
        Vector3dc r0Vec = initialRelativePosition;
        Vector3dc v0Vec = initialLinearVelocity;

        double r0 = r0Vec.length();
        if (mu <= 0.0 || r0 == 0.0) {
            // No gravity (or degenerate start): straight-line motion
            return new Vector3d(v0Vec).mul(dt).add(r0Vec);
        }

        double sqrtMu = Math.sqrt(mu);
        double vr0 = r0Vec.dot(v0Vec) / r0;
        double alpha = 2.0 / r0 - v0Vec.lengthSquared() / mu; // reciprocal of the semi-major axis

        // Bound orbits are periodic; wrap time so the solver stays well conditioned for large game times
        if (alpha > 0.0) {
            double period = 2.0 * Math.PI / (sqrtMu * Math.pow(alpha, 1.5));
            dt %= period;
        }

        double chi = solveUniversalAnomaly(dt, r0, vr0, alpha, sqrtMu);
        double chi2 = chi * chi;
        double z = alpha * chi2;

        double f = 1.0 - chi2 / r0 * stumpffC(z);
        double g = dt - chi2 * chi / sqrtMu * stumpffS(z);

        return new Vector3d(r0Vec).mul(f).fma(g, v0Vec);
    }

    private static double solveUniversalAnomaly(double dt, double r0, double vr0, double alpha, double sqrtMu) {
        if (dt == 0.0) {
            return 0.0;
        }

        // Initial guesses from Vallado, "Fundamentals of Astrodynamics and Applications"
        double chi;
        if (alpha > 1e-12) {
            chi = sqrtMu * alpha * dt;
        } else if (alpha < -1e-12) {
            double semiMajorAxis = 1.0 / alpha;
            double sign = Math.signum(dt);
            chi = sign * Math.sqrt(-semiMajorAxis) * Math.log(
                    (-2.0 * sqrtMu * sqrtMu * alpha * dt)
                            / (r0 * vr0 + sign * Math.sqrt(-sqrtMu * sqrtMu * semiMajorAxis) * (1.0 - r0 * alpha))
            );
        } else {
            chi = sqrtMu * dt / r0;
        }

        double a = r0 * vr0 / sqrtMu;
        double b = 1.0 - alpha * r0;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double chi2 = chi * chi;
            double z = alpha * chi2;
            double c = stumpffC(z);
            double s = stumpffS(z);

            double value = a * chi2 * c + b * chi2 * chi * s + r0 * chi - sqrtMu * dt;
            double derivative = a * chi * (1.0 - z * s) + b * chi2 * c + r0;

            double step = value / derivative;
            chi -= step;
            if (Math.abs(step) <= TOLERANCE * Math.max(1.0, Math.abs(chi))) {
                break;
            }
        }
        return chi;
    }

    private static double stumpffS(double z) {
        if (Math.abs(z) < 1e-6) {
            return 1.0 / 6.0 - z / 120.0;
        }
        if (z > 0.0) {
            double sz = Math.sqrt(z);
            return (sz - Math.sin(sz)) / (sz * sz * sz);
        }
        double sz = Math.sqrt(-z);
        return (Math.sinh(sz) - sz) / (sz * sz * sz);
    }

    private static double stumpffC(double z) {
        if (Math.abs(z) < 1e-6) {
            return 0.5 - z / 24.0;
        }
        if (z > 0.0) {
            return (1.0 - Math.cos(Math.sqrt(z))) / z;
        }
        return (Math.cosh(Math.sqrt(-z)) - 1.0) / -z;
    }

    public static final Codec<OrbitingTransformProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("parent").forGetter(OrbitingTransformProvider::parent),
                    MoreCodecs.VECTOR3DC.fieldOf("initialRelativePosition").forGetter(OrbitingTransformProvider::initialRelativePosition),
                    MoreCodecs.VECTOR3DC.fieldOf("initialLinearVelocity").forGetter(OrbitingTransformProvider::initialLinearVelocity),
                    MoreCodecs.VECTOR3DC.fieldOf("initialRotation").forGetter(OrbitingTransformProvider::initialRotation),
                    MoreCodecs.VECTOR3DC.fieldOf("rotationalVelocity").forGetter(OrbitingTransformProvider::rotationalVelocity)
            ).apply(instance, OrbitingTransformProvider::new)
    );
}
