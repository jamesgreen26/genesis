package shipwrights.genesis.space;

import kotlin.Pair;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.registry.SpaceRegistry;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.transformProvider.StaticTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.space.type.CelestialType;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpaceLevel celestialRaycast tests")
class SpaceLevelTest {

    private SpaceRegistry testRegistry;
    private SpaceRegistry originalRegistry;

    @BeforeEach
    void setUp() throws Exception {
        // Save original registry
        originalRegistry = GenesisMod.SPACE_REGISTRY;

        // Create a test registry
        testRegistry = new SpaceRegistry(new ArrayList<>());

        // Replace the static final field using reflection
        setFinalStatic(GenesisMod.class.getDeclaredField("SPACE_REGISTRY"), testRegistry);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore original registry
        if (originalRegistry != null) {
            setFinalStatic(GenesisMod.class.getDeclaredField("SPACE_REGISTRY"), originalRegistry);
        }
        // Clear test registry using reflection
        if (testRegistry != null) {
            clearRegistry(testRegistry);
        }
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        // In Java 17+, we can't modify the modifiers field directly
        // But we can still set the field value if we bypass the security manager
        try {
            field.set(null, newValue);
        } catch (IllegalAccessException e) {
            // If that fails, try using the internal Unsafe API
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            Object staticFieldBase = unsafe.staticFieldBase(field);
            long staticFieldOffset = unsafe.staticFieldOffset(field);
            unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
        }
    }

    private static void clearRegistry(SpaceRegistry registry) throws Exception {
        Field celestialsField = SpaceRegistry.class.getDeclaredField("celestials");
        celestialsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<ResourceLocation, Celestial> celestialsMap =
            (java.util.Map<ResourceLocation, Celestial>) celestialsField.get(registry);
        celestialsMap.clear();
    }

    private void addCelestialToRegistry(Celestial celestial) throws Exception {
        // Add directly to celestials map using reflection
        Field celestialsField = SpaceRegistry.class.getDeclaredField("celestials");
        celestialsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<ResourceLocation, Celestial> celestialsMap =
            (java.util.Map<ResourceLocation, Celestial>) celestialsField.get(testRegistry);
        celestialsMap.put(celestial.getID(), celestial);
    }

    @Test
    @DisplayName("Should return null when no celestials are registered")
    void testRaycastWithNoCelestials() {
        // Arrange
        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNull(result, "Should return null when no celestials exist");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one star")
    void testRaycastHitsOneStar() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, // size
            1.0, // gravity
            1f, 1f, 1f // r, g, b
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0); // Ray pointing directly at star
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find the star");
        assertEquals(testStar, result.getFirst(), "Should return the star");
        assertTrue(result.getSecond() >= 0, "Distance squared should be non-negative");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one orbiting body")
    void testRaycastHitsOneOrbitingBody() throws Exception {
        // Arrange
        // Create orbiting body at (50, 0, 0) using StaticTransformProvider
        Celestial testBody = new Celestial(
            new StaticTransformProvider(50, 0, 0),
            ResourceLocation.parse("test:body1"),
            BuiltinCelestialTypes.BODY,
            0.5, // size
            1.0, // gravity
            1f, 1f, 1f // r, g, b
        );
        addCelestialToRegistry(testBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find the orbiting body");
        assertEquals(testBody, result.getFirst(), "Should return the orbiting body");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits multiple celestials")
    void testRaycastHitsMultipleCelestials() throws Exception {
        // Arrange
        // Create a far star and a closer star, both on the ray path
        Celestial farStar = new Celestial(
            new StaticTransformProvider(1000, 0, 0),
            ResourceLocation.parse("test:far_star"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        Celestial closeStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            ResourceLocation.parse("test:close_star"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );

        addCelestialToRegistry(farStar);
        addCelestialToRegistry(closeStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find a celestial");
        assertEquals(closeStar, result.getFirst(),
            "Should return the closer star");
    }

    @Test
    @DisplayName("Should return null when ray misses all celestials")
    void testRaycastMissesAllCelestials() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(0, 1, 0); // Ray pointing perpendicular to star
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNull(result, "Should return null when ray misses all celestials");
    }

    @Test
    @DisplayName("Should handle ray starting inside a celestial bounding box")
    void testRaycastStartingInsideCelestial() throws Exception {
        // Arrange
        Celestial largeStar = new Celestial(
            new StaticTransformProvider(50, 50, 50),
            ResourceLocation.parse("test:large_star"),
            BuiltinCelestialTypes.STAR,
            5.0, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(largeStar);

        // Origin is inside or near the star's bounding box
        Vector3d origin = new Vector3d(50, 50, 50);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should detect celestial even when starting inside");
    }

    @Test
    @DisplayName("Should handle normalized ray direction")
    void testRaycastWithNormalizedDirection() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 100, 0),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 1, 0).normalize(); // Normalized diagonal direction
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should handle normalized direction vectors");
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void testRaycastWithNegativeCoordinates() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(-100, -100, -100),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(-1, -1, -1).normalize();
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should handle negative coordinates");
    }

    @Test
    @DisplayName("Should correctly calculate distance squared")
    void testDistanceSquaredCalculation() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(100, 0, 0),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            0.1, 1.0, 1f, 1f, 1f // Small star
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find the star");
        double distanceSq = result.getSecond();
        assertTrue(distanceSq >= 0, "Distance squared should be non-negative");
        assertTrue(Double.isFinite(distanceSq), "Distance squared should be finite");
    }

    @Test
    @DisplayName("Should handle multiple stars along the same ray")
    void testRaycastWithMultipleStarsOnRay() throws Exception {
        // Arrange
        Celestial star1 = new Celestial(
            new StaticTransformProvider(200, 0, 0),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        Celestial star2 = new Celestial(
            new StaticTransformProvider(500, 0, 0),
            ResourceLocation.parse("test:star2"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        Celestial star3 = new Celestial(
            new StaticTransformProvider(1000, 0, 0),
            ResourceLocation.parse("test:star3"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );

        addCelestialToRegistry(star1);
        addCelestialToRegistry(star2);
        addCelestialToRegistry(star3);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find a celestial");
        assertEquals(star1, result.getFirst(),
            "Should return the closest celestial (star at 200)");
    }

    @Test
    @DisplayName("Should respect different tick values with orbiting bodies")
    void testRaycastWithDifferentTicksForOrbitingBody() throws Exception {
        // Arrange
        // Create body with tick-dependent provider
        CelestialTransformProvider tickDependentProvider = new CelestialTransformProvider() {
            @Override
            public Quaterniondc getRotation(long ticks, float subticks) {
                return new Quaterniond();
            }

            @Override
            public Vector3d getPosition(long ticks, float subticks) {
                // Position changes with ticks - at tick 0 it's at (100,0,0), at tick 1000 it's at (200,0,0)
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
            ResourceLocation.parse("test:body1"),
            BuiltinCelestialTypes.BODY,
            0.5, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(testBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);

        // Act - Test at different tick values
        Pair<Celestial, Double> result1 =
            SpaceLevel.celestialRaycast(0L, 0f, origin, direction, type -> true);
        Pair<Celestial, Double> result2 =
            SpaceLevel.celestialRaycast(1000L, 0f, origin, direction, type -> true);

        // Assert - Both should find the body, but at different distances
        assertNotNull(result1, "Should find body at tick 0");
        assertNotNull(result2, "Should find body at tick 1000");
        // Distance at tick 1000 should be larger since body moved further away
        assertTrue(result2.getSecond() > result1.getSecond(),
            "Body should be further away at tick 1000");
    }

    @Test
    @DisplayName("Should handle perpendicular rays")
    void testRaycastPerpendicular() throws Exception {
        // Arrange
        Celestial testStar = new Celestial(
            new StaticTransformProvider(0, 0, 100),
            ResourceLocation.parse("test:star1"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        addCelestialToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0); // Ray perpendicular to star direction
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNull(result, "Should miss star when ray is perpendicular");
    }

    @Test
    @DisplayName("Should prioritize closer orbiting body over far star")
    void testRaycastPrioritizesCloserBody() throws Exception {
        // Arrange
        Celestial farStar = new Celestial(
            new StaticTransformProvider(500, 0, 0),
            ResourceLocation.parse("test:far_star"),
            BuiltinCelestialTypes.STAR,
            1.0, 1.0, 1f, 1f, 1f
        );
        Celestial closeBody = new Celestial(
            new StaticTransformProvider(200, 0, 0),
            ResourceLocation.parse("test:close_body"),
            BuiltinCelestialTypes.BODY,
            0.5, 1.0, 1f, 1f, 1f
        );

        addCelestialToRegistry(farStar);
        addCelestialToRegistry(closeBody);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Pair<Celestial, Double> result =
            SpaceLevel.celestialRaycast(ticks, 0f, origin, direction, type -> true);

        // Assert
        assertNotNull(result, "Should find a celestial");
        assertEquals(closeBody, result.getFirst(),
            "Should return the closer orbiting body, not the far star");
    }
}
