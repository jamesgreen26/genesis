package shipwrights.genesis.space;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.registry.SpaceRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;

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
        Method resetMethod = SpaceRegistry.class.getDeclaredMethod("reset");
        resetMethod.setAccessible(true);
        resetMethod.invoke(registry);
    }

    private void addStarToRegistry(Star star) throws Exception {
        // Use reflection to access private addStar method
        Method addStarMethod = SpaceRegistry.class.getDeclaredMethod("addStar",
            net.minecraft.resources.ResourceLocation.class, Star.class);
        addStarMethod.setAccessible(true);
        addStarMethod.invoke(testRegistry, star.getID(), star);
    }

    private void addOrbitingBodyToRegistry(OrbitingBody body, Star parent) throws Exception {
        // First add the parent star
        addStarToRegistry(parent);

        // Link the parent using the new public method
        body.defineParent(parent);

        // Add directly to orbitingBodies map using reflection
        Field bodiesField = SpaceRegistry.class.getDeclaredField("orbitingBodies");
        bodiesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<net.minecraft.resources.ResourceLocation, OrbitingBody> bodiesMap =
            (java.util.Map<net.minecraft.resources.ResourceLocation, OrbitingBody>) bodiesField.get(testRegistry);
        bodiesMap.put(body.getID(), body);
    }

    /**
     * Simple test implementation of CustomTransformProvider that returns a fixed position and rotation
     */
    private static class FixedTransformProvider implements CustomTransformProvider {
        private final Vector3d position;
        private final Quaterniondc rotation;

        public FixedTransformProvider(double x, double y, double z) {
            this.position = new Vector3d(x, y, z);
            this.rotation = new Quaterniond();
        }

        @Override
        public Quaterniondc getRotation(long ticks, float subticks, Orbitable parent) {
            return rotation;
        }

        @Override
        public Vector3d getCurrentPos(long ticks, float subticks, Orbitable parent) {
            return new Vector3d(position);
        }

        @Override
        public net.minecraft.resources.ResourceLocation getType() {
            return net.minecraft.resources.ResourceLocation.parse("genesis:test_fixed");
        }
    }

    @Test
    @DisplayName("Should return empty when no celestials are registered")
    void testRaycastWithNoCelestials() {
        // Arrange
        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when no celestials exist");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one star")
    void testRaycastHitsOneStar() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 1.0, 100, 0, 0);
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0); // Ray pointing directly at star
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find the star");
        assertEquals(testStar, result.get().getCelestial(), "Should return the star");
        assertTrue(result.get().getDistanceSquared() >= 0, "Distance squared should be non-negative");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits one orbiting body")
    void testRaycastHitsOneOrbitingBody() throws Exception {
        // Arrange
        // Position parent star away from the ray path
        Star parentStar = new Star("test:parent", 5000, 0.01, 0, 1000, 0);

        // Create orbiting body with fixed position at (50, 0, 0) using CustomTransformProvider
        OrbitingBody testBody = new OrbitingBody(
            "test:body1",
            "test:parent",
            0.5, 50, 1000, 1.0, 1f, 1f, 1f,
            new FixedTransformProvider(50, 0, 0)
        );
        addOrbitingBodyToRegistry(testBody, parentStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find the orbiting body");
        assertEquals(testBody, result.get().getCelestial(), "Should return the orbiting body");
    }

    @Test
    @DisplayName("Should return closest celestial when ray hits multiple celestials")
    void testRaycastHitsMultipleCelestials() throws Exception {
        // Arrange
        // Create a far star and a closer star, both on the ray path
        Star farStar = new Star("test:far_star", 5000, 1.0, 1000, 0, 0);
        Star closeStar = new Star("test:close_star", 6000, 1.0, 100, 0, 0);

        addStarToRegistry(farStar);
        addStarToRegistry(closeStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find a celestial");
        assertEquals(closeStar, result.get().getCelestial(),
            "Should return the closer star");
    }

    @Test
    @DisplayName("Should return empty when ray misses all celestials")
    void testRaycastMissesAllCelestials() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 1.0, 100, 0, 0);
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(0, 1, 0); // Ray pointing perpendicular to star
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when ray misses all celestials");
    }

    @Test
    @DisplayName("Should handle ray starting inside a celestial bounding box")
    void testRaycastStartingInsideCelestial() throws Exception {
        // Arrange
        Star largeStar = new Star("test:large_star", 5000, 5.0, 50, 50, 50);
        addStarToRegistry(largeStar);

        // Origin is inside or near the star's bounding box
        Vector3d origin = new Vector3d(50, 50, 50);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should detect celestial even when starting inside");
    }

    @Test
    @DisplayName("Should handle normalized ray direction")
    void testRaycastWithNormalizedDirection() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 1.0, 100, 100, 0);
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 1, 0).normalize(); // Normalized diagonal direction
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should handle normalized direction vectors");
    }

    @Test
    @DisplayName("Should handle negative coordinates")
    void testRaycastWithNegativeCoordinates() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 1.0, -100, -100, -100);
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(-1, -1, -1).normalize();
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should handle negative coordinates");
    }

    @Test
    @DisplayName("Should correctly calculate distance squared")
    void testDistanceSquaredCalculation() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 0.1, 100, 0, 0); // Small star
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find the star");
        double distanceSq = result.get().getDistanceSquared();
        assertTrue(distanceSq >= 0, "Distance squared should be non-negative");
        assertTrue(Double.isFinite(distanceSq), "Distance squared should be finite");
    }

    @Test
    @DisplayName("Should handle multiple stars along the same ray")
    void testRaycastWithMultipleStarsOnRay() throws Exception {
        // Arrange
        Star star1 = new Star("test:star1", 5000, 1.0, 200, 0, 0);
        Star star2 = new Star("test:star2", 6000, 1.0, 500, 0, 0);
        Star star3 = new Star("test:star3", 7000, 1.0, 1000, 0, 0);

        addStarToRegistry(star1);
        addStarToRegistry(star2);
        addStarToRegistry(star3);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find a celestial");
        assertEquals(star1, result.get().getCelestial(),
            "Should return the closest celestial (star at 200)");
    }

    @Test
    @DisplayName("Should respect different tick values with orbiting bodies")
    void testRaycastWithDifferentTicksForOrbitingBody() throws Exception {
        // Arrange
        // Position parent star away from the ray path
        Star parentStar = new Star("test:parent", 5000, 0.01, 0, 1000, 0);

        // Create orbiting body that returns different positions based on ticks
        CustomTransformProvider tickDependentProvider = new CustomTransformProvider() {
            @Override
            public Quaterniondc getRotation(long ticks, float subticks, Orbitable parent) {
                return new Quaterniond();
            }

            @Override
            public Vector3d getCurrentPos(long ticks, float subticks, Orbitable parent) {
                // Position changes with ticks - at tick 0 it's at (100,0,0), at tick 1000 it's at (200,0,0)
                double x = 100 + (ticks / 10.0);
                return new Vector3d(x, 0, 0);
            }

            @Override
            public net.minecraft.resources.ResourceLocation getType() {
                return net.minecraft.resources.ResourceLocation.parse("genesis:test_tick_dependent");
            }
        };

        OrbitingBody testBody = new OrbitingBody(
            "test:body1",
            "test:parent",
            0.5, 50, 1000, 1.0, 1f, 1f, 1f,
            tickDependentProvider
        );
        addOrbitingBodyToRegistry(testBody, parentStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);

        // Act - Test at different tick values
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result1 =
            SpaceLevel.celestialRaycast(0L, origin, direction);
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result2 =
            SpaceLevel.celestialRaycast(1000L, origin, direction);

        // Assert - Both should find the body, but at different distances
        assertTrue(result1.isPresent(), "Should find body at tick 0");
        assertTrue(result2.isPresent(), "Should find body at tick 1000");
        // Distance at tick 1000 should be larger since body moved further away
        assertTrue(result2.get().getDistanceSquared() > result1.get().getDistanceSquared(),
            "Body should be further away at tick 1000");
    }

    @Test
    @DisplayName("Should handle perpendicular rays")
    void testRaycastPerpendicular() throws Exception {
        // Arrange
        Star testStar = new Star("test:star1", 5000, 1.0, 0, 0, 100);
        addStarToRegistry(testStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0); // Ray perpendicular to star direction
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertFalse(result.isPresent(), "Should miss star when ray is perpendicular");
    }

    @Test
    @DisplayName("Should prioritize closer orbiting body over far star")
    void testRaycastPrioritizesCloserBody() throws Exception {
        // Arrange
        Star farStar = new Star("test:far_star", 5000, 1.0, 500, 0, 0);
        // Position parent star away from the ray path
        Star parentStar = new Star("test:parent", 5000, 0.01, 0, 1000, 0);

        // Create orbiting body closer to origin than the far star
        OrbitingBody closeBody = new OrbitingBody(
            "test:close_body",
            "test:parent",
            0.5, 50, 1000, 1.0, 1f, 1f, 1f,
            new FixedTransformProvider(200, 0, 0)
        );

        addStarToRegistry(farStar);
        addOrbitingBodyToRegistry(closeBody, parentStar);

        Vector3d origin = new Vector3d(0, 0, 0);
        Vector3d direction = new Vector3d(1, 0, 0);
        long ticks = 0L;

        // Act
        Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> result =
            SpaceLevel.celestialRaycast(ticks, origin, direction);

        // Assert
        assertTrue(result.isPresent(), "Should find a celestial");
        assertEquals(closeBody, result.get().getCelestial(),
            "Should return the closer orbiting body, not the far star");
    }
}
