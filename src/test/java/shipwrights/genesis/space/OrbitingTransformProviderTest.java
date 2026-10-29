package shipwrights.genesis.space;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.MinecraftBootstrap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitingTransformProviderTest {

    private static final ResourceLocation STAR = GenesisMod.id("star");
    private static final ResourceLocation PLANET = GenesisMod.id("planet");
    private static final ResourceLocation MOON = GenesisMod.id("moon");
    private static final Vector3dc STAR_POSITION = new Vector3d(5, 6, 7);
    private static final double STAR_MASS = 1e6;

    private MappedRegistry<Celestial> celestials;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
    }

    @BeforeEach
    void setUp() {
        celestials = new MappedRegistry<>(Celestial.REGISTRY_KEY, Lifecycle.stable());
        register(STAR, new Celestial(false, false, 10, STAR_MASS, new StaticTransformProvider(STAR_POSITION, new Vector3d())));
    }

    @Test
    void circularOrbitKeepsRadius() {
        double radius = 10000, period = 50000;
        register(PLANET, orbit(STAR, radius, period, 0, 0, 0, 0, 0));

        assertDistance(PLANET, STAR, radius, at(0), 1e-6);
        assertDistance(PLANET, STAR, radius, at(period / 4), 1e-6);
        // Large game times must not lose precision
        assertDistance(PLANET, STAR, radius, at(period * 1234.5), 1e-3);
    }

    @Test
    void orbitMovesFromXTowardsZ() {
        double radius = 10000, period = 50000;
        register(PLANET, orbit(STAR, radius, period, 0, 0, 0, 0, 0));

        assertRelativePosition(PLANET, new Vector3d(radius, 0, 0), at(0));
        assertRelativePosition(PLANET, new Vector3d(0, 0, radius), at(period / 4));
    }

    @Test
    void negativePeriodIsRetrograde() {
        double radius = 10000, period = 50000;
        register(PLANET, orbit(STAR, radius, -period, 0, 0, 0, 0, 0));

        assertRelativePosition(PLANET, new Vector3d(0, 0, -radius), at(period / 4));
    }

    @Test
    void startAngleOffsetsInitialPosition() {
        double radius = 10000;
        register(PLANET, orbit(STAR, radius, 50000, 0, 0, 0, 0, 90));

        assertRelativePosition(PLANET, new Vector3d(0, 0, radius), at(0));
    }

    @Test
    void inclinationTiltsAboutItsDirection() {
        double radius = 10000, period = 50000;
        register(PLANET, orbit(STAR, radius, period, 0, 30, 90, 0, 0));

        // Ascending node is along +Z, so a quarter orbit later the planet is at its highest point
        assertRelativePosition(PLANET, new Vector3d(0, 0, radius), at(0));
        assertRelativePosition(PLANET, new Vector3d(-radius * Math.cos(Math.toRadians(30)), radius * Math.sin(Math.toRadians(30)), 0), at(period / 4));
    }

    @Test
    void ellipticalOrbitReachesPeriapsisAndApoapsis() {
        double semiMajorAxis = 10000, eccentricity = 0.6, period = 50000;
        register(PLANET, orbit(STAR, semiMajorAxis, period, eccentricity, 0, 0, 90, 0));

        // Periapsis rotated to +Z, apoapsis opposite
        assertRelativePosition(PLANET, new Vector3d(0, 0, semiMajorAxis * (1 - eccentricity)), at(0));
        assertRelativePosition(PLANET, new Vector3d(0, 0, -semiMajorAxis * (1 + eccentricity)), at(period / 2));
        assertRelativePosition(PLANET, new Vector3d(0, 0, semiMajorAxis * (1 - eccentricity)), at(period * 1000));
    }

    @Test
    void highlyEccentricOrbitConverges() {
        double semiMajorAxis = 10000, eccentricity = 0.99, period = 50000;
        register(PLANET, orbit(STAR, semiMajorAxis, period, eccentricity, 0, 0, 0, 0));

        for (double t = 0; t < period; t += period / 97) {
            double distance = position(PLANET, at(t)).distance(STAR_POSITION);
            assertTrue(distance >= semiMajorAxis * (1 - eccentricity) - 1e-6 && distance <= semiMajorAxis * (1 + eccentricity) + 1e-6);
        }
    }

    @Test
    void missingPeriodFollowsPhysics() {
        double semiMajorAxis = 10000, eccentricity = 0.3;
        register(PLANET, new Celestial(false, false, 5, 1e3, new OrbitingTransformProvider(
                STAR, semiMajorAxis, Optional.empty(), eccentricity, 0, 0, 0, 0, OrbitingTransformProvider.Rotation.TIDALLY_LOCKED)));

        // Start at periapsis with the vis-viva speed and integrate the two-body problem
        double periapsis = semiMajorAxis * (1 - eccentricity);
        Vector3d r0 = new Vector3d(periapsis, 0, 0);
        Vector3d v0 = new Vector3d(0, 0, Math.sqrt(STAR_MASS * (2 / periapsis - 1 / semiMajorAxis)));

        double dt = 50000;
        Vector3d expected = integrate(r0, v0, STAR_MASS, dt).add(STAR_POSITION);
        assertEquals(0, expected.distance(position(PLANET, at(dt))), 1e-6 * expected.length());
    }

    @Test
    void tidallyLockedFacesParent() {
        double radius = 10000, period = 50000;
        register(PLANET, orbit(STAR, radius, period, 0, 20, 45, 0, 0));

        for (double t : new double[]{0, period / 3, period * 12.7}) {
            assertEquals(0, localDirectionToParent(PLANET, STAR, at(t)).distance(0, 0, -1), 1e-9);
        }
    }

    @Test
    void dayLengthIsMeasuredRelativeToParent() {
        double dayLength = 2400;
        register(PLANET, new Celestial(false, false, 5, 1e3, new OrbitingTransformProvider(
                STAR, 10000, Optional.of(50000.0), 0, 0, 0, 0, 0,
                new OrbitingTransformProvider.Rotation(Optional.of(dayLength), 0, 0))));

        assertEquals(0, localDirectionToParent(PLANET, STAR, at(0)).distance(0, 0, -1), 1e-9);
        assertEquals(0, localDirectionToParent(PLANET, STAR, at(dayLength)).distance(0, 0, -1), 1e-9);
        assertEquals(0, localDirectionToParent(PLANET, STAR, at(dayLength / 2)).distance(0, 0, 1), 1e-9);
    }

    @Test
    void nestedOrbitsFollowTheirParents() {
        double planetRadius = 10000, moonRadius = 100;
        register(PLANET, orbit(STAR, planetRadius, 50000, 0, 0, 0, 0, 0));
        register(MOON, new Celestial(false, false, 1, 1, new OrbitingTransformProvider(
                PLANET, moonRadius, Optional.of(3000.0), 0, 10, 0, 0, 0, OrbitingTransformProvider.Rotation.TIDALLY_LOCKED)));

        for (double t : new double[]{0, 750, 12500, 1e7}) {
            Time time = at(t);
            assertDistance(PLANET, STAR, planetRadius, time, 1e-3);
            assertDistance(MOON, PLANET, moonRadius, time, 1e-6);
        }
    }

    @Test
    void decodesWithDefaults() {
        OrbitingTransformProvider provider = decode("""
                {"parent": "genesis:star", "distance": 750, "rotation": {"dayLength": "tidally_locked"}}""");
        assertEquals(new OrbitingTransformProvider(STAR, 750, Optional.empty(), 0, 0, 0, 0, 0,
                OrbitingTransformProvider.Rotation.TIDALLY_LOCKED), provider);

        provider = decode("""
                {"parent": "genesis:star", "distance": 750, "period": 1000, "rotation": {"dayLength": 24000, "axialTilt": 23.4}}""");
        assertEquals(new OrbitingTransformProvider.Rotation(Optional.of(24000.0), 23.4, 0), provider.rotation());
    }

    @Test
    void rejectsInvalidOrbits() {
        assertTrue(decodeResult("""
                {"parent": "genesis:star", "distance": 750, "eccentricity": 1}""").error().isPresent());
        assertTrue(decodeResult("""
                {"parent": "genesis:star", "distance": 0}""").error().isPresent());
        assertTrue(decodeResult("""
                {"parent": "genesis:star", "distance": 750, "rotation": {"dayLength": "spinning"}}""").error().isPresent());
    }

    private void register(ResourceLocation id, Celestial celestial) {
        celestials.register(ResourceKey.create(Celestial.REGISTRY_KEY, id), celestial, Lifecycle.stable());
    }

    private Celestial.TransformProvider transform(ResourceLocation id) {
        return celestials.getOrThrow(ResourceKey.create(Celestial.REGISTRY_KEY, id)).transformProvider();
    }

    private Vector3dc position(ResourceLocation id, Time time) {
        return transform(id).getPosition(celestials, time);
    }

    private Vector3d localDirectionToParent(ResourceLocation id, ResourceLocation parent, Time time) {
        Vector3d toParent = new Vector3d(position(parent, time)).sub(position(id, time)).normalize();
        return new Quaterniond(transform(id).getRotation(celestials, time)).conjugate().transform(toParent);
    }

    private void assertDistance(ResourceLocation a, ResourceLocation b, double expected, Time time, double delta) {
        assertEquals(expected, position(a, time).distance(position(b, time)), delta);
    }

    private void assertRelativePosition(ResourceLocation id, Vector3dc expected, Time time) {
        assertEquals(0, new Vector3d(position(id, time)).sub(STAR_POSITION).distance(expected), 1e-6 * expected.length());
    }

    private static Celestial orbit(ResourceLocation parent, double distance, double period, double eccentricity,
                                   double inclination, double inclinationDirection, double periapsisAngle, double startAngle) {
        return new Celestial(false, false, 5, 1e3, new OrbitingTransformProvider(parent, distance, Optional.of(period),
                eccentricity, inclination, inclinationDirection, periapsisAngle, startAngle,
                OrbitingTransformProvider.Rotation.TIDALLY_LOCKED));
    }

    private static com.mojang.serialization.DataResult<OrbitingTransformProvider> decodeResult(String json) {
        return OrbitingTransformProvider.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static OrbitingTransformProvider decode(String json) {
        return decodeResult(json).getOrThrow(false, error -> {
            throw new AssertionError(error);
        });
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
