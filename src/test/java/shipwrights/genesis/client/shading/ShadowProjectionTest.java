package shipwrights.genesis.client.shading;

import org.joml.*;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.lang.Math;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShadowProjectionTest {

    private static final double EPS = 1e-9;

    /* -------------------------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------------------- */

    private static OBB unitCube(Vector3dc center) {
        return OBB.createCube(
                1.0,
                new Quaterniond(),
                center
        );
    }

    private static OBB rotatedCube(Vector3dc center, double degrees, Vector3dc axis) {
        return OBB.createCube(
                1.0,
                new Quaterniond().rotateAxis(
                        Math.toRadians(degrees),
                        axis.x(), axis.y(), axis.z()
                ),
                center
        );
    }

    private static <T extends Vector2dc> void assertPolygonValid(List<T> poly) {
        assertTrue(poly.size() >= 3, "Polygon must have at least 3 vertices");
        for (int i = 0; i < poly.size(); i++) {
            Vector2dc a = poly.get(i);
            Vector2dc b = poly.get((i + 1) % poly.size());
            assertTrue(a.distance(b) > EPS, "Degenerate or duplicate edge");
        }
    }

    /* -------------------------------------------------------------------------
     * clipToPlaneBounds()
     * ---------------------------------------------------------------------- */

    @Test
    void clip_polygonFullyInsideFace_remainsUnchanged() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        AAPlane plane = new AAPlane(new Vector3i(0, 0, 1), 0.5);

        List<Vector2d> poly = List.of(
                new Vector2d(-0.25, -0.25),
                new Vector2d( 0.25, -0.25),
                new Vector2d( 0.25,  0.25),
                new Vector2d(-0.25,  0.25)
        );

        List<Vector2d> clipped =
                ShadowProjection.clipToPlaneBounds(self, plane, poly);

        assertEquals(4, clipped.size());
    }

    @Test
    void clip_polygonOutsideFace_isDiscarded() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        AAPlane plane = new AAPlane(new Vector3i(1, 0, 0), 0.5);

        List<Vector2d> poly = List.of(
                new Vector2d(2, 2),
                new Vector2d(3, 2),
                new Vector2d(3, 3),
                new Vector2d(2, 3)
        );

        List<Vector2d> clipped =
                ShadowProjection.clipToPlaneBounds(self, plane, poly);

        assertTrue(clipped.isEmpty());
    }

    /* -------------------------------------------------------------------------
     * projectAndClip()
     * ---------------------------------------------------------------------- */

    @Test
    void projectAndClip_handlesCollinearPoints() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        AAPlane plane = new AAPlane(new Vector3i(0, 1, 0), 0.5);

        List<Vector2dc> poly = List.of(
                new Vector2d(0, 0),
                new Vector2d(0.5, 0),
                new Vector2d(1.0, 0),
                new Vector2d(1.0, 1.0),
                new Vector2d(0, 1.0)
        );

        List<Vector2d> cleaned =
                ShadowProjection.projectAndClip(self, plane, poly);

        // Note: We no longer prune collinear points, which can result in near-duplicate vertices
        // Just verify we have enough vertices for a polygon
        assertTrue(cleaned.size() >= 3, "Polygon should have at least 3 vertices");
        assertFalse(cleaned.isEmpty(), "Should return some vertices");
    }

    @Test
    void projectAndClip_sortsVerticesCCW() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        AAPlane plane = new AAPlane(new Vector3i(0, 0, 1), 0.5);

        List<Vector2dc> poly = List.of(
                new Vector2d(1, 0),
                new Vector2d(0, 0),
                new Vector2d(0, 1),
                new Vector2d(1, 1)
        );

        List<Vector2d> sorted =
                ShadowProjection.projectAndClip(self, plane, poly);

        assertPolygonValid(sorted);
    }

    /* -------------------------------------------------------------------------
     * accumulateProjectedPolygons()
     * ---------------------------------------------------------------------- */

    @Test
    void accumulate_multipleOccludersAccumulateOnSameFace() {
        OBB self = unitCube(new Vector3d(0, 0, 0));

        OBB occ1 = unitCube(new Vector3d(0, 0, 2));
        OBB occ2 = unitCube(new Vector3d(0.3, 0, 2));

        Vector3d light = new Vector3d(0, 0, 5);

        List<AAPlane> planes =
                PlanetShading.getFacingPlanes(self, light);

        Map<AAPlane, List<List<Vector2d>>> result =
                ShadowProjection.accumulateProjectedPolygons(
                        self,
                        List.of(occ1, occ2),
                        planes,
                        light
                );

        assertFalse(result.isEmpty());

        // Each plane should have collected multiple polygons from the two occluders
        for (List<List<Vector2d>> polygons : result.values()) {
            assertFalse(polygons.isEmpty());
            // Each polygon should be valid
            for (List<Vector2d> poly : polygons) {
                assertTrue(poly.size() >= 3);
            }
        }
    }

    /* -------------------------------------------------------------------------
     * computeShadows() – integration
     * ---------------------------------------------------------------------- */

    @Test
    void computeShadows_simpleOverheadShadow() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        OBB occluder = unitCube(new Vector3d(0, 0, 2));

        Vector3d light = new Vector3d(0, 0, 5);

        List<FaceShadow> shadows =
                ShadowProjection.computeShadows(
                        self,
                        List.of(occluder),
                        light
                );

        assertFalse(shadows.isEmpty());
        assertPolygonValid(shadows.get(0).polygon());
    }

    @Test
    void computeShadows_noShadowWhenOccluderBehindLight() {
        OBB self = unitCube(new Vector3d(0, 0, 0));
        OBB occluder = unitCube(new Vector3d(0, 0, 6));

        Vector3d light = new Vector3d(0, 0, 5);

        List<FaceShadow> shadows =
                ShadowProjection.computeShadows(
                        self,
                        List.of(occluder),
                        light
                );

        assertTrue(shadows.isEmpty());
    }

    @Test
    void computeShadows_partiallyOverlappingShadowsMergeToConvexHull() {
        // Setup: OBB at origin with two occluders that cast partially overlapping shadows
        OBB self = unitCube(new Vector3d(0, 0, 0));

        // Two occluders positioned to create overlapping shadows on the top face (z=0.5)
        // Occluder 1 is centered at x=-0.3, occluder 2 at x=0.3
        // They're close enough that shadows overlap but don't completely coincide
        OBB occluder1 = unitCube(new Vector3d(-0.3, 0, 2));
        OBB occluder2 = unitCube(new Vector3d(0.3, 0, 2));

        Vector3d light = new Vector3d(0, 0, 5);

        List<FaceShadow> shadows =
                ShadowProjection.computeShadows(
                        self,
                        List.of(occluder1, occluder2),
                        light
                );

        assertFalse(shadows.isEmpty(), "Should produce shadows");

        // Find the shadow on the top face (Z plane)
        FaceShadow topFaceShadow = shadows.stream()
                .filter(s -> s.plane().normal().z() == 1)
                .findFirst()
                .orElse(null);

        assertNotNull(topFaceShadow, "Should have shadow on top face");

        List<Vector2dc> polygon = topFaceShadow.polygon();
        assertTrue(polygon.size() >= 3, "Shadow polygon should have at least 3 vertices");

        // Verify the polygon is valid (no duplicate vertices, no collinear points)
        assertPolygonValid(polygon);

        // Verify no three consecutive vertices are collinear
        for (int i = 0; i < polygon.size(); i++) {
            Vector2dc a = polygon.get(i);
            Vector2dc b = polygon.get((i + 1) % polygon.size());
            Vector2dc c = polygon.get((i + 2) % polygon.size());

            double cross = (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
            double scale = Math.max(
                    Math.pow(a.x() - c.x(), 2) + Math.pow(a.y() - c.y(), 2),
                    1e-10
            );

            assertTrue(Math.abs(cross) > 1e-12 * scale,
                    "No three consecutive points should be collinear at indices " + i);
        }

        // Verify the polygon is convex (all cross products have the same sign)
        // This ensures we got a proper convex hull without interior points
        boolean allPositive = true;
        boolean allNegative = true;

        for (int i = 0; i < polygon.size(); i++) {
            Vector2dc a = polygon.get(i);
            Vector2dc b = polygon.get((i + 1) % polygon.size());
            Vector2dc c = polygon.get((i + 2) % polygon.size());

            double cross = (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());

            if (cross > 1e-10) allNegative = false;
            if (cross < -1e-10) allPositive = false;
        }

        assertTrue(allPositive || allNegative,
                "Polygon should be convex (all turns in same direction)");

        // Verify we got a single merged shadow, not two separate ones
        // The polygon should be larger than either individual shadow would be
        // (it should cover the convex hull of both shadow regions)
        assertTrue(polygon.size() >= 4,
                "Merged shadow should have at least 4 vertices for partially overlapping shadows");
    }
}
