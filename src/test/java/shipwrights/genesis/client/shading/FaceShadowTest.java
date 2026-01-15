package shipwrights.genesis.client.shading;

import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.AAPlane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FaceShadowTest {

    private static final double EPSILON = 1e-10;

    // Helper to create Vector2dc
    private Vector2dc v(double x, double y) {
        return new Vector2d(x, y);
    }

    // Helper to create AAPlane (Z-axis plane at given position)
    private AAPlane planeXY(double z) {
        return new AAPlane(new Vector3i(0, 0, 1), z);
    }

    @Test
    @DisplayName("merge() - Throws exception for different planes")
    void testMergeDifferentPlanes() {
        FaceShadow shadow1 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(1, 0), v(1, 1))
        );

        FaceShadow shadow2 = new FaceShadow(
                planeXY(5),  // different plane
                Arrays.asList(v(0, 0), v(1, 0), v(1, 1))
        );

        assertThrows(IllegalArgumentException.class, () -> shadow1.merge(shadow2));
    }

    @Test
    @DisplayName("merge() - Merges two non-overlapping squares")
    void testMergeNonOverlappingSquares() {
        // First square: (0,0) to (1,1)
        FaceShadow shadow1 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(1, 0), v(1, 1), v(0, 1))
        );

        // Second square: (2,0) to (3,1)
        FaceShadow shadow2 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(2, 0), v(3, 0), v(3, 1), v(2, 1))
        );

        FaceShadow merged = shadow1.merge(shadow2);

        assertEquals(planeXY(0), merged.plane());
        assertNotNull(merged.polygon());
        assertTrue(merged.polygon().size() >= 4, "Merged polygon should have at least 4 vertices");

        // The convex hull should contain corners from both squares
        List<Vector2dc> polygon = merged.polygon();
        boolean hasOrigin = polygon.stream().anyMatch(p ->
                Math.abs(p.x()) < EPSILON && Math.abs(p.y()) < EPSILON
        );
        boolean hasCorner = polygon.stream().anyMatch(p ->
                Math.abs(p.x() - 3) < EPSILON && Math.abs(p.y() - 1) < EPSILON
        );

        assertTrue(hasOrigin, "Should include origin corner from first square");
        assertTrue(hasCorner, "Should include far corner from second square");
    }

    @Test
    @DisplayName("merge() - Merges two overlapping squares")
    void testMergeOverlappingSquares() {
        // First square: (0,0) to (2,2)
        FaceShadow shadow1 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(2, 0), v(2, 2), v(0, 2))
        );

        // Second square: (1,1) to (3,3)
        FaceShadow shadow2 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(1, 1), v(3, 1), v(3, 3), v(1, 3))
        );

        FaceShadow merged = shadow1.merge(shadow2);

        assertEquals(planeXY(0), merged.plane());
        List<Vector2dc> polygon = merged.polygon();
        assertNotNull(polygon);
        assertTrue(polygon.size() >= 4, "Merged polygon should have vertices");

        // Verify extreme corners are in the result
        boolean hasOrigin = polygon.stream().anyMatch(p ->
                Math.abs(p.x()) < EPSILON && Math.abs(p.y()) < EPSILON
        );
        boolean hasOpposite = polygon.stream().anyMatch(p ->
                Math.abs(p.x() - 3) < EPSILON && Math.abs(p.y() - 3) < EPSILON
        );

        assertTrue(hasOrigin, "Should include (0,0) corner");
        assertTrue(hasOpposite, "Should include (3,3) corner");
    }

    @Test
    @DisplayName("merge() - Merges identical triangles")
    void testMergeIdenticalTriangles() {
        FaceShadow shadow1 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(1, 0), v(0.5, 1))
        );

        FaceShadow shadow2 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(1, 0), v(0.5, 1))
        );

        FaceShadow merged = shadow1.merge(shadow2);

        assertEquals(3, merged.polygon().size(),
                "Merging identical triangles should produce same triangle");
    }

    @Test
    @DisplayName("merge() - One polygon contains the other")
    void testMergeContainedPolygon() {
        // Large square: (0,0) to (10,10)
        FaceShadow shadow1 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0, 0), v(10, 0), v(10, 10), v(0, 10))
        );

        // Small square: (2,2) to (3,3) - contained in first
        FaceShadow shadow2 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(2, 2), v(3, 2), v(3, 3), v(2, 3))
        );

        FaceShadow merged = shadow1.merge(shadow2);

        // Result should be the convex hull, which is the larger square
        assertEquals(4, merged.polygon().size(),
                "Result should be the larger square");

        // Verify corners of large square are in result
        List<Vector2dc> polygon = merged.polygon();
        boolean hasOrigin = polygon.stream().anyMatch(p ->
                Math.abs(p.x()) < EPSILON && Math.abs(p.y()) < EPSILON
        );
        boolean hasCorner = polygon.stream().anyMatch(p ->
                Math.abs(p.x() - 10) < EPSILON && Math.abs(p.y() - 10) < EPSILON
        );

        assertTrue(hasOrigin && hasCorner,
                "Result should have corners of the larger square");
    }

    @Test
    @DisplayName("merge() - Result has no collinear points")
    void testMergeRemovesCollinearPoints() {
        // Create a square with extra collinear points
        List<Vector2dc> polygon1 = new ArrayList<>();
        polygon1.add(v(0, 0));
        polygon1.add(v(0.5, 0));  // collinear with (0,0) and (1,0)
        polygon1.add(v(1, 0));
        polygon1.add(v(1, 1));
        polygon1.add(v(0, 1));

        FaceShadow shadow1 = new FaceShadow(planeXY(0), polygon1);

        // Simple triangle
        FaceShadow shadow2 = new FaceShadow(
                planeXY(0),
                Arrays.asList(v(0.25, 0.25), v(0.75, 0.25), v(0.5, 0.75))
        );

        FaceShadow merged = shadow1.merge(shadow2);

        // The result should have collinear points removed
        assertNotNull(merged.polygon());
        assertTrue(merged.polygon().size() >= 3, "Should have at least 3 vertices");

        // Verify no three consecutive points are collinear
        List<Vector2dc> polygon = merged.polygon();
        for (int i = 0; i < polygon.size(); i++) {
            Vector2d a = new Vector2d(polygon.get(i));
            Vector2d b = new Vector2d(polygon.get((i + 1) % polygon.size()));
            Vector2d c = new Vector2d(polygon.get((i + 2) % polygon.size()));

            double cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
            double scale = a.distanceSquared(c);

            // This is the same test as pruneCollinear uses
            assertTrue(Math.abs(cross) > 1e-12 * scale || scale < EPSILON,
                    "No three consecutive points should be collinear");
        }
    }
}
