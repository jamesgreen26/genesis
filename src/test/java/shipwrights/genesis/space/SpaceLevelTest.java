package shipwrights.genesis.space;

import kotlin.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.space.properties.EmptyProperties;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.transformProvider.StaticTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpaceLevel celestialRaycast tests")
class SpaceLevelTest {

    private List<Celestial> candidates;

    @BeforeEach
    void setUp() {
        candidates = new ArrayList<>();
    }

    private void addCelestial(Celestial celestial) {
        candidates.add(celestial);
    }

    @Test
    @DisplayName("Should return null when no celestials are registered")
    void testRaycastWithNoCelestials() {
        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNull(result, "Should return null when no celestials exist");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one star")
    void testRaycastHitsOneStar() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find the star");
        assertEquals(testStar, result.getFirst(), "Should return the star");
        assertTrue(result.getSecond() >= 0, "Distance squared should be non-negative");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one orbiting body")
    void testRaycastHitsOneOrbitingBody() {
        Celestial testBody = new Celestial(
            new StaticTransformProvider(50, 0, 0),
            BuiltinCelestialTypes.BODY,
            0.5, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find the orbiting body");
        assertEquals(testBody, result.getFirst(), "Should return the orbiting body");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits multiple celestials")
    void testRaycastHitsMultipleCelestials() {
        Celestial farStar = new Celestial(
            new StaticTransformProvider(1000, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        Celestial closeStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );

        addCelestial(farStar);
        addCelestial(closeStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find a celestial");
        assertEquals(closeStar, result.getFirst(), "Should return the closer star");
    }

    @Test
    @DisplayName("Should return null when ray misses all celestials")
    void testRaycastMissesAllCelestials() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(0, 1, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNull(result, "Should return null when ray misses all celestials");
    }

    @Test
    @DisplayName("Should handle ray starting inside a celestial bounding box")
    void testRaycastStartingInsideCelestial() {
        Celestial largeStar = new Celestial(
            new StaticTransformProvider(50, 50, 50),
            BuiltinCelestialTypes.STAR,
            5.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(largeStar);

        Vector3d origin = new Vector3d(50, 50, 50);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should detect celestial even when starting inside");
    }

    @Test
    @DisplayName("Should handle normalized ray direction")
    void testRaycastWithNormalizedDirection() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 100, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 1, 0).normalize();
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should handle normalized direction vectors");
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void testRaycastWithNegativeCoordinates() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(-100, -100, -100),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(-1, -1, -1).normalize();
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should handle negative coordinates");
    }

    @Test
    @DisplayName("Should correctly calculate distance squared")
    void testDistanceSquaredCalculation() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            BuiltinCelestialTypes.STAR,
            0.1, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find the star");
        double distanceSq = result.getSecond();
        assertTrue(distanceSq >= 0, "Distance squared should be non-negative");
        assertTrue(Double.isFinite(distanceSq), "Distance squared should be finite");
    }

    @Test
    @DisplayName("Should handle multiple stars along the same ray")
    void testRaycastWithMultipleStarsOnRay() {
        Celestial star1 = new Celestial(
            new StaticTransformProvider(200, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        Celestial star2 = new Celestial(
            new StaticTransformProvider(500, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        Celestial star3 = new Celestial(
            new StaticTransformProvider(1000, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );

        addCelestial(star1);
        addCelestial(star2);
        addCelestial(star3);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find a celestial");
        assertEquals(star1, result.getFirst(), "Should return the closest celestial (star at 200)");
    }

    @Test
    @DisplayName("Should respect different tick values with orbiting bodies")
    void testRaycastWithDifferentTicksForOrbitingBody() {
        CelestialTransformProvider tickDependentProvider = new CelestialTransformProvider() {
            @Override
            public Quaterniondc getRotation(long ticks, float subticks, Registry<Celestial> registry) {
                return new Quaterniond();
            }

            @Override
            public Vector3d getPosition(long ticks, float subticks, Registry<Celestial> registry) {
                double x = 100 + (ticks / 10.0);
                return new Vector3d(x, 0, 0);
            }

            @Override
            public ResourceLocation getType() {
                return ResourceLocation.parse("genesis:test_tick_dependent");
            }
        };

        Celestial testBody = new Celestial(
            tickDependentProvider,
            BuiltinCelestialTypes.BODY,
            0.5, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);

        Pair<Celestial, Double> result1 =
            SpaceLevel.celestialRaycast(candidates, 0L, 0f, null, origin, direction, type -> true);
        Pair<Celestial, Double> result2 =
            SpaceLevel.celestialRaycast(candidates, 1000L, 0f, null, origin, direction, type -> true);

        assertNotNull(result1, "Should find body at tick 0");
        assertNotNull(result2, "Should find body at tick 1000");
        assertTrue(result2.getSecond() > result1.getSecond(),
            "Body should be further away at tick 1000");
    }

    @Test
    @DisplayName("Should handle perpendicular rays")
    void testRaycastPerpendicular() {
        Celestial testStar = new Celestial(
            new StaticTransformProvider(0, 0, 100),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        addCelestial(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNull(result, "Should miss star when ray is perpendicular");
    }

    @Test
    @DisplayName("Should prioritize closer orbiting body over far star")
    void testRaycastPrioritizesCloserBody() {
        Celestial farStar = new Celestial(
            new StaticTransformProvider(500, 0, 0),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );
        Celestial closeBody = new Celestial(
            new StaticTransformProvider(200, 0, 0),
            BuiltinCelestialTypes.BODY,
            0.5, 1.0, 1f, 1f, 1f,
            EmptyProperties.INSTANCE
        );

        addCelestial(farStar);
        addCelestial(closeBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(candidates, ticks, 0f, null, origin, direction, type -> true);

        assertNotNull(result, "Should find a celestial");
        assertEquals(closeBody, result.getFirst(),
            "Should return the closer orbiting body, not the far star");
    }
}
