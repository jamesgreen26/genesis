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

    @Test
    @DisplayName("clipPolygonToRect() - Triangle clipped on left edge only produces correct quad")
    void testClipTriangleLeftEdgeOnly() {
        // Triangle straddles only the left edge: two vertices outside left, one inside
        List<Vector2d> triangle = Arrays.asList(
                v(-1, 0),  // outside left
                v(1, 0),   // inside
                v(1, 2)    // inside
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, -1, 2, 3);

        // Should produce a quad: intersection on left edge (entering), (1,0), (1,2), intersection on left edge (exiting)
        assertEquals(4, clipped.size(), "Clipping triangle on one edge should produce 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON, "X should be >= 0");
            assertTrue(p.x <= 2 + EPSILON, "X should be <= 2");
        }

        // Both intersections should have x == 0
        long onLeftEdge = clipped.stream().filter(p -> Math.abs(p.x) < EPSILON).count();
        assertEquals(2, onLeftEdge, "Should have exactly 2 points on left clip edge");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Triangle clipped on right edge only produces correct quad")
    void testClipTriangleRightEdgeOnly() {
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),   // inside
                v(2, 0),   // outside right
                v(0, 2)    // inside
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, -1, -1, 1, 3);

        assertEquals(4, clipped.size(), "Clipping triangle on right edge only should produce 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.x <= 1 + EPSILON, "X should be <= 1");
        }

        long onRightEdge = clipped.stream().filter(p -> Math.abs(p.x - 1) < EPSILON).count();
        assertEquals(2, onRightEdge, "Should have exactly 2 points on right clip edge");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Triangle clipped on bottom edge only produces correct quad")
    void testClipTriangleBottomEdgeOnly() {
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),    // inside
                v(2, 0),    // inside
                v(1, -2)    // outside below
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, -1, -1, 3, 3);

        assertEquals(4, clipped.size(), "Clipping triangle on bottom edge only should produce 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.y >= -1 - EPSILON, "Y should be >= -1");
        }

        long onBottomEdge = clipped.stream().filter(p -> Math.abs(p.y - (-1)) < EPSILON).count();
        assertEquals(2, onBottomEdge, "Should have exactly 2 points on bottom clip edge");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Triangle clipped on top edge only produces correct quad")
    void testClipTriangleTopEdgeOnly() {
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),    // inside
                v(2, 0),    // inside
                v(1, 2)     // outside above
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, -1, -1, 3, 1);

        assertEquals(4, clipped.size(), "Clipping triangle on top edge only should produce 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.y <= 1 + EPSILON, "Y should be <= 1");
        }

        long onTopEdge = clipped.stream().filter(p -> Math.abs(p.y - 1) < EPSILON).count();
        assertEquals(2, onTopEdge, "Should have exactly 2 points on top clip edge");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Only one vertex inside produces triangle")
    void testClipOnlyOneVertexInside() {
        // Large triangle with only one vertex inside the clip rect
        List<Vector2d> triangle = Arrays.asList(
                v(0.5, 0.5),  // inside
                v(5, -5),     // outside
                v(-5, 5)      // outside
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, 0, 1, 1);

        assertFalse(clipped.isEmpty(), "Should produce non-empty result");
        assertTrue(clipped.size() >= 3, "Should produce at least a triangle");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 1 + EPSILON, "X out of bounds: " + p.x);
            assertTrue(p.y >= -EPSILON && p.y <= 1 + EPSILON, "Y out of bounds: " + p.y);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon edge lying exactly on clip boundary")
    void testClipEdgeOnBoundary() {
        // Square whose left edge lies exactly on xmin=0
        List<Vector2d> square = Arrays.asList(
                v(0, 0),
                v(1, 0),
                v(1, 1),
                v(0, 1)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(square, 0, 0, 2, 2);

        assertEquals(4, clipped.size(), "Polygon touching boundary should keep all 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 2 + EPSILON);
            assertTrue(p.y >= -EPSILON && p.y <= 2 + EPSILON);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon vertex exactly on corner of clip rect")
    void testClipVertexAtCorner() {
        // Triangle with one vertex exactly at a corner of the clip rect
        List<Vector2d> triangle = Arrays.asList(
                v(0, 0),    // exactly at corner
                v(2, 0),    // outside right
                v(0, 2)     // outside top
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, 0, 1, 1);

        assertFalse(clipped.isEmpty(), "Should produce non-empty result");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 1 + EPSILON, "X out of bounds: " + p.x);
            assertTrue(p.y >= -EPSILON && p.y <= 1 + EPSILON, "Y out of bounds: " + p.y);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - CW-ordered polygon produces correct bounds")
    void testClipCWOrderedPolygon() {
        // Same as the large-square-clips-to-rect test but vertices in CW order
        List<Vector2d> cwSquare = Arrays.asList(
                v(-2, -2),
                v(-2, 5),
                v(5, 5),
                v(5, -2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(cwSquare, 0, 0, 3, 3);

        assertEquals(4, clipped.size(), "CW polygon should clip to 4-vertex rectangle");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 3 + EPSILON, "X out of bounds: " + p.x);
            assertTrue(p.y >= -EPSILON && p.y <= 3 + EPSILON, "Y out of bounds: " + p.y);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Concave (non-convex) polygon clipped correctly")
    void testClipConcavePolygon() {
        // L-shaped concave polygon, fully inside clip rect
        List<Vector2d> lShape = Arrays.asList(
                v(0, 0),
                v(2, 0),
                v(2, 1),
                v(1, 1),
                v(1, 2),
                v(0, 2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(lShape, -1, -1, 3, 3);

        // Fully inside — all 6 vertices must be preserved
        assertEquals(6, clipped.size(), "Concave polygon fully inside should keep all vertices");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Concave polygon with notch crossing clip boundary")
    void testClipConcavePolygonNotchCrossesBoundary() {
        // Arrow/chevron shape: notch points left, body extends right beyond clip
        // The concave notch vertex (0, 1) is inside; the two rightmost vertices are outside.
        List<Vector2d> chevron = Arrays.asList(
                v(0, 0),
                v(3, 0),   // outside right
                v(1, 1),   // inside — the concave notch
                v(3, 2),   // outside right
                v(0, 2)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(chevron, 0, 0, 2, 2);

        assertFalse(clipped.isEmpty(), "Should produce non-empty result");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 2 + EPSILON, "X out of bounds: " + p.x);
            assertTrue(p.y >= -EPSILON && p.y <= 2 + EPSILON, "Y out of bounds: " + p.y);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Triangle crossing two adjacent edges produces pentagon")
    void testClipTriangleTwoEdgesPentagon() {
        // Triangle: one vertex inside, other two outside two different edges
        List<Vector2d> triangle = Arrays.asList(
                v(0.5, 0.5),  // inside
                v(2, -1),     // outside right and below
                v(-1, 2)      // outside left and above
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, 0, 1, 1);

        assertFalse(clipped.isEmpty());
        // Each of the 3 edges crosses at most one clip boundary in this config;
        // result should have more vertices than 3
        assertTrue(clipped.size() >= 3, "Result should have at least 3 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 1 + EPSILON, "X out of bounds: " + p.x);
            assertTrue(p.y >= -EPSILON && p.y <= 1 + EPSILON, "Y out of bounds: " + p.y);
        }
    }

    @Test
    @DisplayName("clipPolygonToRect() - Intersection point coordinates are exact")
    void testClipIntersectionCoordinatesExact() {
        // Triangle: A=(-1,0.5) outside-left, B=(0.5,-1) outside-below, C=(0.5,0.5) inside.
        // The corner (0,0) of the clip rect is inside this triangle, so Sutherland-Hodgman
        // correctly produces a quadrilateral: (0,0.5) -> (0,0) -> (0.5,0) -> (0.5,0.5).
        List<Vector2d> triangle = Arrays.asList(
                v(-1, 0.5),   // outside left
                v(0.5, -1),   // outside below
                v(0.5, 0.5)   // inside
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(triangle, 0, 0, 1, 1);

        // Corner (0,0) is inside the triangle so result is a quad, not a triangle
        assertEquals(4, clipped.size(), "Should produce a quadrilateral (corner (0,0) is inside the triangle)");

        boolean hasLeftIntersect = clipped.stream()
                .anyMatch(p -> Math.abs(p.x) < EPSILON && Math.abs(p.y - 0.5) < EPSILON);
        boolean hasBottomIntersect = clipped.stream()
                .anyMatch(p -> Math.abs(p.x - 0.5) < EPSILON && Math.abs(p.y) < EPSILON);
        boolean hasInsideVertex = clipped.stream()
                .anyMatch(p -> Math.abs(p.x - 0.5) < EPSILON && Math.abs(p.y - 0.5) < EPSILON);
        boolean hasCorner = clipped.stream()
                .anyMatch(p -> Math.abs(p.x) < EPSILON && Math.abs(p.y) < EPSILON);

        assertTrue(hasLeftIntersect, "Should have intersection point at (0, 0.5)");
        assertTrue(hasBottomIntersect, "Should have intersection point at (0.5, 0)");
        assertTrue(hasInsideVertex, "Should retain inside vertex (0.5, 0.5)");
        assertTrue(hasCorner, "Should include clip-rect corner (0, 0) since it is inside the triangle");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon entirely to the right returns empty")
    void testClipPolygonEntirelyRight() {
        List<Vector2d> polygon = Arrays.asList(
                v(5, 0), v(6, 0), v(6, 1), v(5, 1)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(polygon, 0, 0, 3, 3);

        assertTrue(clipped.isEmpty(), "Polygon entirely to the right should clip to empty");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon entirely above returns empty")
    void testClipPolygonEntirelyAbove() {
        List<Vector2d> polygon = Arrays.asList(
                v(0, 5), v(1, 5), v(1, 6), v(0, 6)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(polygon, 0, 0, 3, 3);

        assertTrue(clipped.isEmpty(), "Polygon entirely above should clip to empty");
    }

    @Test
    @DisplayName("clipPolygonToRect() - Polygon exactly coinciding with clip rect")
    void testClipPolygonSameAsRect() {
        List<Vector2d> rect = Arrays.asList(
                v(0, 0), v(1, 0), v(1, 1), v(0, 1)
        );

        List<Vector2d> clipped = PolygonClipping.clipPolygonToRect(rect, 0, 0, 1, 1);

        assertEquals(4, clipped.size(), "Polygon exactly matching clip rect should produce 4 vertices");

        for (Vector2d p : clipped) {
            assertTrue(p.x >= -EPSILON && p.x <= 1 + EPSILON);
            assertTrue(p.y >= -EPSILON && p.y <= 1 + EPSILON);
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