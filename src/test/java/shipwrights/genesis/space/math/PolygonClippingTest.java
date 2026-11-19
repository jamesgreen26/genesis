package shipwrights.genesis.space.math;

import org.joml.Vector2d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolygonClippingTest {

    private static final double EPSILON = 1e-10;

    // Helper method to create Vector2d
    private Vector2d v(double x, double y) {
        return new Vector2d(x, y);
    }

    // Helper method to assert vector equality with tolerance
    private void assertVectorEquals(Vector2d expected, Vector2d actual, double delta) {
        assertEquals(expected.x, actual.x, delta, "X coordinate mismatch");
        assertEquals(expected.y, actual.y, delta, "Y coordinate mismatch");
    }

    // ========== Tests for cross() ==========

    @Test
    @DisplayName("cross() - Left turn (CCW) returns positive value")
    void testCrossLeftTurn() {
        Vector2d a = v(0, 0);
        Vector2d b = v(1, 0);
        Vector2d c = v(1, 1);

        double result = PolygonClipping.cross(a, b, c);
        assertTrue(result > 0, "Left turn should produce positive cross product");
        assertEquals(1.0, result, EPSILON);
    }

    @Test
    @DisplayName("cross() - Right turn (CW) returns negative value")
    void testCrossRightTurn() {
        Vector2d a = v(0, 0);
        Vector2d b = v(1, 0);
        Vector2d c = v(1, -1);

        double result = PolygonClipping.cross(a, b, c);
        assertTrue(result < 0, "Right turn should produce negative cross product");
        assertEquals(-1.0, result, EPSILON);
    }

    @Test
    @DisplayName("cross() - Collinear points return zero")
    void testCrossCollinear() {
        Vector2d a = v(0, 0);
        Vector2d b = v(1, 1);
        Vector2d c = v(2, 2);

        double result = PolygonClipping.cross(a, b, c);
        assertEquals(0.0, result, EPSILON, "Collinear points should produce zero cross product");
    }

    @Test
    @DisplayName("cross() - Works with negative coordinates")
    void testCrossNegativeCoordinates() {
        Vector2d a = v(-1, -1);
        Vector2d b = v(0, 0);
        Vector2d c = v(1, 1);

        double result = PolygonClipping.cross(a, b, c);
        assertEquals(0.0, result, EPSILON);
    }

    // ========== Tests for convexHull() ==========

    @Test
    @DisplayName("convexHull() - Triangle remains unchanged")
    void testConvexHullTriangle() {
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(0.5, 1)
        );

        List<Vector2d> hull = PolygonClipping.convexHull(triangle);

        assertEquals(3, hull.size(), "Convex triangle should remain unchanged");
    }

    @Test
    @DisplayName("convexHull() - Square remains unchanged")
    void testConvexHullSquare() {
        List<Vector2d> square = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(1, 1),
                v(0, 1)
        );

        List<Vector2d> hull = PolygonClipping.convexHull(square);

        assertEquals(4, hull.size(), "Convex square should remain unchanged");
    }

    @Test
    @DisplayName("convexHull() - Removes interior point")
    void testConvexHullRemovesInteriorPoint() {
        List<Vector2d> points = Arrays.asList(
                v(0, 0),
                v(2, 0),
                v(2, 2),
                v(0, 2),
                v(1, 1)  // interior point
        );

        List<Vector2d> hull = PolygonClipping.convexHull(points);

        assertEquals(4, hull.size(), "Interior point should be removed");

        // Verify the interior point is not in the hull
        boolean containsInterior = hull.stream().anyMatch(p ->
                Math.abs(p.x - 1) < EPSILON && Math.abs(p.y - 1) < EPSILON
        );
        assertFalse(containsInterior, "Interior point should not be in convex hull");
    }

    @Test
    @DisplayName("convexHull() - Handles collinear points")
    void testConvexHullCollinearPoints() {
        List<Vector2d> points = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(2, 0),
                v(3, 0)
        );

        List<Vector2d> hull = PolygonClipping.convexHull(points);

        // Collinear points should produce a degenerate hull with 2-4 points
        assertTrue(hull.size() >= 2, "Should have at least 2 points");
    }

    @Test
    @DisplayName("convexHull() - Handles two points")
    void testConvexHullTwoPoints() {
        List<Vector2d> points = Arrays.asList(v(0, 0), v(1, 1));
        List<Vector2d> hull = PolygonClipping.convexHull(points);

        assertEquals(2, hull.size());
    }

    @Test
    @DisplayName("convexHull() - Handles single point")
    void testConvexHullSinglePoint() {
        List<Vector2d> points = Arrays.asList(v(5, 5));
        List<Vector2d> hull = PolygonClipping.convexHull(points);

        assertEquals(1, hull.size());
        assertVectorEquals(v(5, 5), hull.get(0), EPSILON);
    }

    @Test
    @DisplayName("convexHull() - Produces CCW ordered vertices")
    void testConvexHullCCWOrder() {
        List<Vector2d> points = Arrays.asList(
                v(0, 0),
                v(2, 0),
                v(2, 2),
                v(0, 2),
                v(1, 1)  // interior
        );

        List<Vector2d> hull = PolygonClipping.convexHull(points);

        // Verify CCW ordering by checking that cross products are positive
        for (int i = 0; i < hull.size(); i++) {
            Vector2d a = hull.get(i);
            Vector2d b = hull.get((i + 1) % hull.size());
            Vector2d c = hull.get((i + 2) % hull.size());

            double crossProduct = PolygonClipping.cross(a, b, c);
            assertTrue(crossProduct >= -EPSILON,
                    "Hull vertices should be in CCW order (non-negative cross product)");
        }
    }

    @Test
    @DisplayName("convexHull() - Merges two overlapping squares")
    void testConvexHullOverlappingSquares() {
        // Two overlapping squares
        List<Vector2d> points = new ArrayList<>();
        // First square: (0,0) to (2,2)
        points.add(v(0, 0));
        points.add(v(2, 0));
        points.add(v(2, 2));
        points.add(v(0, 2));
        // Second square: (1,1) to (3,3)
        points.add(v(1, 1));
        points.add(v(3, 1));
        points.add(v(3, 3));
        points.add(v(1, 3));

        List<Vector2d> hull = PolygonClipping.convexHull(points);

        // The hull should be a hexagon or octagon
        assertTrue(hull.size() >= 4 && hull.size() <= 8,
                "Merged hull should have 4-8 vertices");

        // Verify corner points are included
        boolean hasOrigin = hull.stream().anyMatch(p ->
                Math.abs(p.x) < EPSILON && Math.abs(p.y) < EPSILON
        );
        boolean hasOpposite = hull.stream().anyMatch(p ->
                Math.abs(p.x - 3) < EPSILON && Math.abs(p.y - 3) < EPSILON
        );

        assertTrue(hasOrigin, "Should include origin corner");
        assertTrue(hasOpposite, "Should include opposite corner");
    }
}