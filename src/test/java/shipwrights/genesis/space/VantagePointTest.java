package shipwrights.genesis.space;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.MinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VantagePointTest {

    private static final Vector3dc PLANET_POSITION = new Vector3d(5, 6, 7);
    private static final double SIZE = 100;
    private static final double EPSILON = 1e-9;

    private MappedRegistry<Celestial> celestials;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
    }

    @BeforeEach
    void setUp() {
        celestials = new MappedRegistry<>(Celestial.REGISTRY_KEY, Lifecycle.stable());
    }

    @Test
    void originStandsOnNegativeZFace() {
        var vantage = onPlanet(new Vector3d(), 0, 0);

        assertVector(new Vector3d(5, 6, 7 - 50), vantage.getPosition());
        assertVector(new Vector3d(0, 0, -1), up(vantage));
        assertVector(new Vector3d(-1, 0, 0), east(vantage));
        assertVector(new Vector3d(0, 1, 0), north(vantage));
    }

    @Test
    void longitudeIncreasesEastward() {
        var vantage = onPlanet(new Vector3d(), 0, Math.PI / 2);

        assertVector(new Vector3d(5 - 50, 6, 7), vantage.getPosition());
        assertVector(new Vector3d(-1, 0, 0), up(vantage));
        assertVector(new Vector3d(0, 1, 0), north(vantage));
    }

    @Test
    void northPoleStandsOnTopFace() {
        var vantage = onPlanet(new Vector3d(), Math.PI / 2, 0);

        assertVector(new Vector3d(5, 56, 7), vantage.getPosition());
        assertVector(new Vector3d(0, 1, 0), up(vantage));
    }

    @Test
    void upIsFaceNormalAwayFromFaceCentre() {
        double longitude = Math.PI / 8;
        var vantage = onPlanet(new Vector3d(), 0, longitude);

        assertVector(new Vector3d(5 - 50 * Math.tan(longitude), 6, 7 - 50), vantage.getPosition());
        assertVector(new Vector3d(0, 0, -1), up(vantage));
    }

    @Test
    void positionIsAlwaysOnTheSurface() {
        for (double latitude = -1.5; latitude <= 1.5; latitude += 0.25) {
            for (double longitude = -3; longitude <= 3; longitude += 0.25) {
                var vantage = onPlanet(new Vector3d(), latitude, longitude);
                Vector3d local = new Vector3d(vantage.getPosition()).sub(PLANET_POSITION).absolute();
                assertEquals(SIZE / 2, local.get(local.maxComponent()), EPSILON);

                // The frame must be orthonormal, with up along a face normal
                Vector3d up = up(vantage), east = east(vantage);
                assertEquals(1, up.length(), EPSILON);
                assertEquals(0, up.dot(east), EPSILON);
                // Stepping north must increase latitude
                Vector3d position = new Vector3d(vantage.getPosition()).sub(PLANET_POSITION);
                Vector3d stepped = new Vector3d(position).fma(1e-3, north(vantage));
                assertTrue(latitudeOf(stepped) > latitudeOf(position), () -> "north decreases latitude at " + position);
                assertEquals(1, up.absolute(new Vector3d()).get(up.absolute(new Vector3d()).maxComponent()), EPSILON);
            }
        }
    }

    @Test
    void followsCelestialRotation() {
        // Rotating a quarter turn about Y takes -Z to -X
        var vantage = onPlanet(new Vector3d(0, Math.PI / 2, 0), 0, 0);

        assertVector(new Vector3d(5 - 50, 6, 7), vantage.getPosition());
        assertVector(new Vector3d(-1, 0, 0), up(vantage));
    }

    private VantagePoint.OnCelestial onPlanet(Vector3dc rotation, double latitude, double longitude) {
        var planet = new Celestial(false, false, SIZE, 1e3, new StaticTransformProvider(PLANET_POSITION, rotation));
        celestials.register(ResourceKey.create(Celestial.REGISTRY_KEY, GenesisMod.id("planet")), planet, Lifecycle.stable());
        return new VantagePoint.OnCelestial(celestials, planet, new Time(0, 0), latitude, longitude);
    }

    private static Vector3d up(VantagePoint vantage) {
        return vantage.getRotation().transform(new Vector3d(0, 1, 0));
    }

    private static Vector3d east(VantagePoint vantage) {
        return vantage.getRotation().transform(new Vector3d(1, 0, 0));
    }

    private static Vector3d north(VantagePoint vantage) {
        return vantage.getRotation().transform(new Vector3d(0, 0, -1));
    }

    private static double latitudeOf(Vector3dc position) {
        return Math.atan2(position.y(), Math.hypot(position.x(), position.z()));
    }

    private static void assertVector(Vector3dc expected, Vector3dc actual) {
        assertEquals(0, expected.distance(actual), EPSILON, () -> "expected " + expected + " but was " + actual);
    }
}
