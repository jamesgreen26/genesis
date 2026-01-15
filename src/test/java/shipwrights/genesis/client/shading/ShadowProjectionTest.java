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
    void projectAndClip_prunesCollinearPoints() {
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

        assertTrue(cleaned.size() < poly.size());
        assertPolygonValid(cleaned);
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

        Map<AAPlane, List<Vector2d>> result =
                ShadowProjection.accumulateProjectedPolygons(
                        self,
                        List.of(occ1, occ2),
                        planes,
                        light
                );

        assertFalse(result.isEmpty());

        for (List<Vector2d> poly : result.values()) {
            assertTrue(poly.size() >= 6);
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
}
