package shipwrights.genesis.space;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.MinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrbitingTransformProviderTest {

    private static final ResourceLocation STAR = GenesisMod.id("star");
    private static final ResourceLocation PLANET = GenesisMod.id("planet");
    private static final ResourceLocation MOON = GenesisMod.id("moon");
    private static final Vector3dc STAR_POSITION = new Vector3d(5, 6, 7);

    private MappedRegistry<Celestial> celestials;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
    }

    @BeforeEach
    void setUp() {
        celestials = new MappedRegistry<>(Celestial.REGISTRY_KEY, Lifecycle.stable());
        register(STAR, new Celestial(false, false, 10, 1e6, new StaticTransformProvider(STAR_POSITION, new Vector3d())));
    }

    @Test
    void circularOrbitKeepsRadius() {
        double radius = 10000;
        register(PLANET, new Celestial(false, false, 5, 1e3, circularOrbit(STAR, 1e6, radius)));

        double period = period(1e6, radius);
        assertDistance(PLANET, STAR, radius, at(0), 1e-6);
        assertDistance(PLANET, STAR, radius, at(period / 4), 1e-6);
        // Large game times must not lose precision
        assertDistance(PLANET, STAR, radius, at(period * 1234.5), 1e-3);
    }

    @Test
    void quarterOrbitMovesAlongVelocity() {
        double radius = 10000;
        register(PLANET, new Celestial(false, false, 5, 1e3, circularOrbit(STAR, 1e6, radius)));

        Vector3dc position = position(PLANET, at(period(1e6, radius) / 4));
        assertEquals(STAR_POSITION.x(), position.x(), 1e-3);
        assertEquals(STAR_POSITION.z() + radius, position.z(), 1e-3);
    }

    @Test
    void ellipticalOrbitReachesApoapsis() {
        double mu = 1e6, radius = 10000, speed = 1.2 * Math.sqrt(mu / radius);
        register(PLANET, new Celestial(false, false, 5, 1e3, new OrbitingTransformProvider(
                STAR, new Vector3d(radius, 0, 0), new Vector3d(0, 0, speed), new Vector3d(), new Vector3d())));

        double semiMajorAxis = 1 / (2 / radius - speed * speed / mu);
        double halfPeriod = Math.PI * Math.sqrt(semiMajorAxis * semiMajorAxis * semiMajorAxis / mu);
        assertDistance(PLANET, STAR, 2 * semiMajorAxis - radius, at(halfPeriod), 1e-3);
    }

    @Test
    void hyperbolicTrajectoryMatchesNumericalIntegration() {
        double mu = 1e6;
        Vector3d r0 = new Vector3d(10000, 200, -300);
        Vector3d v0 = new Vector3d(-1, 2, 2 * Math.sqrt(mu / r0.length()));
        register(PLANET, new Celestial(false, false, 5, 1e3, new OrbitingTransformProvider(STAR, r0, v0, new Vector3d(), new Vector3d())));

        double dt = 5000;
        Vector3d expected = integrate(r0, v0, mu, dt).add(STAR_POSITION);
        Vector3dc actual = position(PLANET, at(dt));
        assertEquals(0, expected.distance(actual), 1e-6 * expected.length());
    }

    @Test
    void nestedOrbitsFollowTheirParents() {
        double planetRadius = 10000, moonRadius = 100;
        register(PLANET, new Celestial(false, false, 5, 1e3, circularOrbit(STAR, 1e6, planetRadius)));
        register(MOON, new Celestial(false, false, 1, 1, circularOrbit(PLANET, 1e3, moonRadius)));

        for (double t : new double[]{0, period(1e3, moonRadius) / 4, period(1e6, planetRadius) / 4, 1e7}) {
            Time time = at(t);
            assertDistance(PLANET, STAR, planetRadius, time, 1e-3);
            assertDistance(MOON, PLANET, moonRadius, time, 1e-6);
        }
    }

    private void register(ResourceLocation id, Celestial celestial) {
        celestials.register(ResourceKey.create(Celestial.REGISTRY_KEY, id), celestial, Lifecycle.stable());
    }

    private Vector3dc position(ResourceLocation id, Time time) {
        return celestials.getOrThrow(ResourceKey.create(Celestial.REGISTRY_KEY, id))
                .transformProvider().getPosition(celestials, time);
    }

    private void assertDistance(ResourceLocation a, ResourceLocation b, double expected, Time time, double delta) {
        assertEquals(expected, position(a, time).distance(position(b, time)), delta);
    }

    private static OrbitingTransformProvider circularOrbit(ResourceLocation parent, double parentMass, double radius) {
        return new OrbitingTransformProvider(parent, new Vector3d(radius, 0, 0),
                new Vector3d(0, 0, Math.sqrt(parentMass / radius)), new Vector3d(), new Vector3d());
    }

    private static double period(double mu, double radius) {
        return 2 * Math.PI * Math.sqrt(radius * radius * radius / mu);
    }

    private static Time at(double t) {
        long ticks = (long) t;
        return new Time(ticks, (float) (t - ticks));
    }

    // RK4 reference solution for the two-body problem
    private static Vector3d integrate(Vector3dc r0, Vector3dc v0, double mu, double duration) {
        Vector3d r = new Vector3d(r0), v = new Vector3d(v0);
        int steps = 100000;
        double h = duration / steps;
        for (int i = 0; i < steps; i++) {
            Vector3d k1r = new Vector3d(v), k1v = gravity(r, mu);
            Vector3d k2r = new Vector3d(v).fma(h / 2, k1v), k2v = gravity(new Vector3d(r).fma(h / 2, k1r), mu);
            Vector3d k3r = new Vector3d(v).fma(h / 2, k2v), k3v = gravity(new Vector3d(r).fma(h / 2, k2r), mu);
            Vector3d k4r = new Vector3d(v).fma(h, k3v), k4v = gravity(new Vector3d(r).fma(h, k3r), mu);
            r.fma(h / 6, k1r).fma(h / 3, k2r).fma(h / 3, k3r).fma(h / 6, k4r);
            v.fma(h / 6, k1v).fma(h / 3, k2v).fma(h / 3, k3v).fma(h / 6, k4v);
        }
        return r;
    }

    private static Vector3d gravity(Vector3dc r, double mu) {
        double length = r.length();
        return new Vector3d(r).mul(-mu / (length * length * length));
    }
}
