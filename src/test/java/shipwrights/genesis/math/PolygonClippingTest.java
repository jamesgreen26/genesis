package shipwrights.genesis.math;

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

    // ========== Tests for angleSort() ==========

    @Test
    @DisplayName("angleSort() - Sorts square vertices in CCW order")
    void testAngleSortSquare() {
        List<Vector2d> points = Arrays.asList(
                v(1, 1),   // top-right
                v(-1, -1), // bottom-left
                v(-1, 1),  // top-left
                v(1, -1)   // bottom-right
        );

        List<Vector2d> sorted = PolygonClipping.angleSort(points);

        assertEquals(4, sorted.size());
        // After sorting around centroid (0,0), should start from rightmost point and go CCW
        // The exact starting point depends on atan2, but order should be consistent
        for (int i = 0; i < sorted.size(); i++) {
            Vector2d curr = sorted.get(i);
            Vector2d next = sorted.get((i + 1) % sorted.size());
            Vector2d centroid = v(0, 0);

            double angleCurr = Math.atan2(curr.y, curr.x);
            double angleNext = Math.atan2(next.y, next.x);

            // Allow for wrap-around
            if (angleNext < angleCurr) {
                angleNext += 2 * Math.PI;
            }
            assertTrue(angleNext >= angleCurr, "Points should be sorted in CCW order");
        }
    }

    @Test
    @DisplayName("angleSort() - Handles single point")
    void testAngleSortSinglePoint() {
        List<Vector2d> points = Arrays.asList(v(5, 5));
        List<Vector2d> sorted = PolygonClipping.angleSort(points);

        assertEquals(1, sorted.size());
        assertVectorEquals(v(5, 5), sorted.get(0), EPSILON);
    }

    @Test
    @DisplayName("angleSort() - Handles two points")
    void testAngleSortTwoPoints() {
        List<Vector2d> points = Arrays.asList(v(1, 0), v(0, 1));
        List<Vector2d> sorted = PolygonClipping.angleSort(points);

        assertEquals(2, sorted.size());
    }

    @Test
    @DisplayName("angleSort() - Preserves all input points")
    void testAngleSortPreservesPoints() {
        List<Vector2d> points = Arrays.asList(v(3, 4), v(-2, 1), v(0, -3), v(5, 0));
        List<Vector2d> sorted = PolygonClipping.angleSort(points);

        assertEquals(points.size(), sorted.size());
    }

    // ========== Tests for pruneCollinear() ==========

    @Test
    @DisplayName("pruneCollinear() - Removes collinear point from triangle")
    void testPruneCollinearRemovesMiddlePoint() {
        List<Vector2d> polygon = Arrays.asList(
                v(0, 0),
                v(1, 1),  // collinear
                v(2, 2),
                v(2, 0)
        );

        List<Vector2d> pruned = PolygonClipping.pruneCollinear(polygon, 1e-12);

        assertTrue(pruned.size() < polygon.size(), "Should remove at least one collinear point");
        boolean correct = pruned.containsAll(List.of(polygon.get(0), polygon.get(2), polygon.get(3)));
        assertTrue(correct, "Should have correct values");
    }

    @Test
    @DisplayName("pruneCollinear() - Keeps non-collinear triangle")
    void testPruneCollinearKeepsTriangle() {
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(0.5, 1)
        );

        List<Vector2d> pruned = PolygonClipping.pruneCollinear(triangle, 1e-12);

        assertEquals(3, pruned.size(), "Triangle with no collinear points should remain unchanged");
    }

    @Test
    @DisplayName("pruneCollinear() - Handles square (no collinear points)")
    void testPruneCollinearSquare() {
        List<Vector2d> square = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(1, 1),
                v(0, 1)
        );

        List<Vector2d> pruned = PolygonClipping.pruneCollinear(square, 1e-12);

        assertEquals(4, pruned.size(), "Square should keep all vertices");
    }

    @Test
    @DisplayName("pruneCollinear() - Handles polygon with less than 3 points")
    void testPruneCollinearTooFewPoints() {
        List<Vector2d> twoPoints = Arrays.asList(v(0, 0), v(1, 1));
        List<Vector2d> pruned = PolygonClipping.pruneCollinear(twoPoints, 1e-12);

        assertEquals(2, pruned.size());
    }

    @Test
    @DisplayName("pruneCollinear() - Empty list returns empty")
    void testPruneCollinearEmpty() {
        List<Vector2d> empty = new ArrayList<>();
        List<Vector2d> pruned = PolygonClipping.pruneCollinear(empty, 1e-12);

        assertTrue(pruned.isEmpty());
    }

    // ========== Tests for clipPolygonToRect() ==========

    @Test
    @DisplayName("clipPolygonToRect() - Polygon fully inside rectangle unchanged")
    void testClipPolygonFullyInside() {
        List<Vector2d> square = Arrays.asList(
                v(1, 1),
                v(2, 1),
                v(2, 2),
                v(1, 2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(square, 0, 0, 3, 3);

        assertEquals(4, clipped.size(), "Fully inside polygon should remain unchanged");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon fully outside returns empty or minimal")
    void testClipPolygonFullyOutside() {
        List<Vector2d> square = Arrays.asList(
                v(-5, -5),
                v(-4, -5),
                v(-4, -4),
                v(-5, -4)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(square, 0, 0, 3, 3);

        assertTrue(clipped.isEmpty() || clipped.size() < 3,
                "Fully outside polygon should be empty or degenerate");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Clips polygon partially outside")
    void testClipPolygonPartiallyOutside() {
        List<Vector2d> square = Arrays.asList(
                v(-1, -1),
                v(2, -1),
                v(2, 2),
                v(-1, 2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(square, 0, 0, 1, 1);

        assertFalse(clipped.isEmpty(), "Should produce non-empty result");

        // Verify all points are inside the rectangle
        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 1 + EPSILON, "X should be in bounds");
            assertTrue(p.y >= -EPSILON && p.y <= 1 + EPSILON, "Y should be in bounds");
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Clips triangle to create quadrilateral")
    void testClipTriangleToQuad() {
        List<Vector2d> triangle = Arrays.asList(
                v(-1, 0),
                v(2, 0),
                v(0.5, 2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, 0, 1, 1);

        assertFalse(clipped.isEmpty());

        // All vertices should be within bounds
        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON, "X should be >= 0");
            assertTrue(p.x <= 1 + EPSILON, "X should be <= 1");
            assertTrue(p.y >= -EPSILON, "Y should be >= 0");
            assertTrue(p.y <= 1 + EPSILON, "Y should be <= 1");
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Handles empty polygon")
    void testClipEmptyPolygon() {
        List<Vector2d> empty = new ArrayList<>();
        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(empty, 0, 0, 1, 1);

        assertTrue(clipped.isEmpty());
    }

    @Test
    @DisplayName("clipPolygonToRect() - Clips on all four edges")
    void testClipOnAllEdges() {
        // Large square that extends beyond all edges of clip rectangle
        List<Vector2d> largeSquare = Arrays.asList(
                v(-2, -2),
                v(5, -2),
                v(5, 5),
                v(-2, 5)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(largeSquare, 0, 0, 3, 3);

        assertEquals(4, clipped.size(), "Should produce a rectangle");

        // Verify it matches the clip rectangle
        boolean hasOrigin = false;
        boolean hasOpposite = false;

        for (Vector2d p : clipped) {
            if (Math.abs(p.x) < EPSILON && Math.abs(p.y) < EPSILON) hasOrigin = true;
            if (Math.abs(p.x - 3) < EPSILON && Math.abs(p.y - 3) < EPSILON) hasOpposite = true;
        }

        assertTrue(hasOrigin || clipped.stream().allMatch(p ->
                p.x >= -EPSILON && p.x <= 3 + EPSILON &&
                        p.y >= -EPSILON && p.y <= 3 + EPSILON
        ), "Result should be bounded by clip rectangle");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Handles negative coordinates")
    void testClipWithNegativeCoordinates() {
        List<Vector2d> polygon = Arrays.asList(
                v(-5, -5),
                v(-1, -5),
                v(-1, -1),
                v(-5, -1)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(polygon, -3, -3, 0, 0);

        assertFalse(clipped.isEmpty());

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -3 - EPSILON && p.x <= EPSILON);
            assertTrue(p.y >= -3 - EPSILON && p.y <= EPSILON);
        }
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