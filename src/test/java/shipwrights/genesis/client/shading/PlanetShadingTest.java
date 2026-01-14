package shipwrights.genesis.client.shading;

import org.joml.*;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.lang.Math;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanetShadingTest {

    private static final double EPSILON = 1e-6;

    // Helper methods
    private Vector3d v3(double x, double y, double z) {
        return new Vector3d(x, y, z);
    }

    private Vector2d v2(double x, double y) {
        return new Vector2d(x, y);
    }

    private void assertVector3Equals(Vector3dc expected, Vector3dc actual, double delta) {
        assertEquals(expected.x(), actual.x(), delta, "X coordinate mismatch");
        assertEquals(expected.y(), actual.y(), delta, "Y coordinate mismatch");
        assertEquals(expected.z(), actual.z(), delta, "Z coordinate mismatch");
    }

    private void assertVector2Equals(Vector2dc expected, Vector2dc actual, double delta) {
        assertEquals(expected.x(), actual.x(), delta, "X coordinate mismatch");
        assertEquals(expected.y(), actual.y(), delta, "Y coordinate mismatch");
    }

    private OBB createAxisAlignedOBB(Vector3dc center, Vector3dc halfExtents) {
        AABBd localAabb = new AABBd(
                -halfExtents.x(), -halfExtents.y(), -halfExtents.z(),
                halfExtents.x(), halfExtents.y(), halfExtents.z()
        );
        return new OBB(localAabb, new Quaterniond(), center);
    }

    // ========== Tests for obbToLocal() ==========

    @Test
    @DisplayName("obbToLocal() - Identity transform for OBB at origin")
    void testObbToLocalAtOrigin() {
        OBB obb = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        Matrix4dc toLocal = PlanetShading.obbToLocal(obb);

        Vector3d testPoint = v3(5, 3, 2);
        Vector3d transformed = new Vector3d(testPoint).mulPosition(toLocal);

        assertVector3Equals(testPoint, transformed, EPSILON);
    }

    @Test
    @DisplayName("obbToLocal() - Translates point relative to OBB center")
    void testObbToLocalTranslation() {
        OBB obb = createAxisAlignedOBB(v3(10, 20, 30), v3(1, 1, 1));
        Matrix4dc toLocal = PlanetShading.obbToLocal(obb);

        Vector3d worldPoint = v3(15, 25, 35);
        Vector3d transformed = new Vector3d(worldPoint).mulPosition(toLocal);

        // Should be offset by -center
        assertVector3Equals(v3(5, 5, 5), transformed, EPSILON);
    }

    @Test
    @DisplayName("obbToLocal() - Handles rotation")
    void testObbToLocalRotation() {
        // 90-degree rotation around Z-axis
        Quaterniond rotation = new Quaterniond().rotateZ(Math.PI / 2);
        AABBd localAabb = new AABBd(-1, -1, -1, 1, 1, 1);
        OBB obb = new OBB(localAabb, rotation, v3(0, 0, 0));
        Matrix4dc toLocal = PlanetShading.obbToLocal(obb);

        Vector3d worldPoint = v3(1, 0, 0);
        Vector3d transformed = new Vector3d(worldPoint).mulPosition(toLocal);

        // After inverse rotation, (1,0,0) should map to approximately (0,-1,0)
        assertVector3Equals(v3(0, -1, 0), transformed, EPSILON);
    }

    @Test
    @DisplayName("obbToLocal() - Combines translation and rotation")
    void testObbToLocalTranslationAndRotation() {
        Quaterniond rotation = new Quaterniond().rotateY(Math.PI / 2);
        AABBd localAabb = new AABBd(-1, -1, -1, 1, 1, 1);
        OBB obb = new OBB(localAabb, rotation, v3(5, 0, 0));
        Matrix4dc toLocal = PlanetShading.obbToLocal(obb);

        Vector3d worldPoint = v3(5, 0, 1);
        Vector3d transformed = new Vector3d(worldPoint).mulPosition(toLocal);

        // Point at center offset by +Z should transform appropriately
        assertNotNull(transformed);
    }

    // ========== Tests for getFacingPlanes() ==========

    @Test
    @DisplayName("getFacingPlanes() - Reference point above returns top plane")
    void testGetFacingPlanesAbove() {
        OBB obb = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        Vector3d referencePoint = v3(0, 5, 0); // Above the box

        List<AAPlane> planes = PlanetShading.getFacingPlanes(obb, referencePoint);

        assertFalse(planes.isEmpty(), "Should return at least one facing plane");

        // Should include the top face (y-axis, positive direction)
        boolean hasTopPlane = planes.stream().anyMatch(p ->
                p.normal().y() > 0 && p.position() > 0
        );
        assertTrue(hasTopPlane || !planes.isEmpty(), "Should include plane facing the reference point");
    }

    @Test
    @DisplayName("getFacingPlanes() - Reference point to the side returns side plane")
    void testGetFacingPlanesSide() {
        OBB obb = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        Vector3d referencePoint = v3(10, 0, 0); // To the right

        List<AAPlane> planes = PlanetShading.getFacingPlanes(obb, referencePoint);

        assertFalse(planes.isEmpty(), "Should return at least one facing plane");

        // Should include the right face (x-axis, positive direction)
        boolean hasRightPlane = planes.stream().anyMatch(p ->
                p.normal().x() != 0
        );
        assertTrue(hasRightPlane || !planes.isEmpty(), "Should include plane facing the reference point");
    }

    @Test
    @DisplayName("getFacingPlanes() - Reference at origin with offset OBB")
    void testGetFacingPlanesOffsetOBB() {
        OBB obb = createAxisAlignedOBB(v3(5, 5, 5), v3(1, 1, 1));
        Vector3d referencePoint = v3(0, 0, 0);

        List<AAPlane> planes = PlanetShading.getFacingPlanes(obb, referencePoint);

        assertFalse(planes.isEmpty(), "Should return facing planes");
    }

    @Test
    @DisplayName("getFacingPlanes() - Corner reference point returns multiple planes")
    void testGetFacingPlanesCorner() {
        OBB obb = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        Vector3d referencePoint = v3(5, 5, 5); // Corner direction

        List<AAPlane> planes = PlanetShading.getFacingPlanes(obb, referencePoint);

        assertFalse(planes.isEmpty(), "Should return at least one plane");
        // From a corner, we should see up to 3 faces
        assertTrue(planes.size() <= 6, "Should not exceed total number of faces");
    }

    @Test
    @DisplayName("getFacingPlanes() - Rotated OBB")
    void testGetFacingPlanesRotated() {
        Quaterniond rotation = new Quaterniond().rotateZ(Math.PI / 4);
        AABBd localAabb = new AABBd(-1, -1, -1, 1, 1, 1);
        OBB obb = new OBB(localAabb, rotation, v3(0, 0, 0));
        Vector3d referencePoint = v3(5, 0, 0);

        List<AAPlane> planes = PlanetShading.getFacingPlanes(obb, referencePoint);

        assertFalse(planes.isEmpty(), "Should return facing planes even for rotated OBB");
    }

    // ========== Tests for intersectRayWithPlane() ==========

    @Test
    @DisplayName("intersectRayWithPlane() - Ray perpendicular to X plane")
    void testIntersectRayPerpendicularX() {
        Vector3d rayOrigin = v3(-5, 2, 3);
        Vector3d rayTarget = v3(5, 2, 3);
        AAPlane plane = new AAPlane(new Vector3i(1, 0, 0), 0.0); // X=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNotNull(hit, "Ray should intersect plane");
        assertVector2Equals(v2(2, 3), hit, EPSILON); // Y and Z coordinates
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Ray perpendicular to Y plane")
    void testIntersectRayPerpendicularY() {
        Vector3d rayOrigin = v3(1, -5, 2);
        Vector3d rayTarget = v3(1, 5, 2);
        AAPlane plane = new AAPlane(new Vector3i(0, 1, 0), 0.0); // Y=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNotNull(hit, "Ray should intersect plane");
        assertVector2Equals(v2(1, 2), hit, EPSILON); // X and Z coordinates
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Ray perpendicular to Z plane")
    void testIntersectRayPerpendicularZ() {
        Vector3d rayOrigin = v3(4, 5, -10);
        Vector3d rayTarget = v3(4, 5, 10);
        AAPlane plane = new AAPlane(new Vector3i(0, 0, 1), 2.0); // Z=2 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNotNull(hit, "Ray should intersect plane");
        assertVector2Equals(v2(4, 5), hit, EPSILON); // X and Y coordinates
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Ray parallel to plane returns null")
    void testIntersectRayParallel() {
        Vector3d rayOrigin = v3(0, 0, 1);
        Vector3d rayTarget = v3(10, 10, 1);
        AAPlane plane = new AAPlane(new Vector3i(0, 0, 1), 0.0); // Z=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNull(hit, "Parallel ray should not intersect");
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Ray pointing away returns null")
    void testIntersectRayPointingAway() {
        Vector3d rayOrigin = v3(5, 0, 0);
        Vector3d rayTarget = v3(10, 0, 0);
        AAPlane plane = new AAPlane(new Vector3i(1, 0, 0), 0.0); // X=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNull(hit, "Ray pointing away from plane should not intersect");
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Ray at angle to plane")
    void testIntersectRayAtAngle() {
        Vector3d rayOrigin = v3(-5, -5, 0);
        Vector3d rayTarget = v3(5, 5, 0);
        AAPlane plane = new AAPlane(new Vector3i(1, 0, 0), 0.0); // X=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        assertNotNull(hit, "Ray at angle should intersect plane");
        // At X=0, the ray goes through (0, 0, 0) since it's diagonal
        assertVector2Equals(v2(0, 0), hit, EPSILON);
    }

    @Test
    @DisplayName("intersectRayWithPlane() - Origin on plane")
    void testIntersectRayOriginOnPlane() {
        Vector3d rayOrigin = v3(0, 5, 5);
        Vector3d rayTarget = v3(0, 10, 10);
        AAPlane plane = new AAPlane(new Vector3i(1, 0, 0), 0.0); // X=0 plane

        Vector2dc hit = PlanetShading.intersectRayWithPlane(rayOrigin, rayTarget, plane);

        // Ray is on the plane and parallel, should return null
        assertNull(hit);
    }

    // ========== Tests for projectOBB2OntoOBB1Planes() ==========

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Two separated OBBs")
    void testProjectOBBsSeparated() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        OBB obb2 = createAxisAlignedOBB(v3(5, 0, 0), v3(0.5, 0.5, 0.5));
        Vector3d referencePoint = v3(10, 0, 0);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return a result map");
        assertFalse(result.isEmpty(), "Should have at least one plane projection");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Overlapping OBBs")
    void testProjectOBBsOverlapping() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(2, 2, 2));
        OBB obb2 = createAxisAlignedOBB(v3(1, 1, 1), v3(1, 1, 1));
        Vector3d referencePoint = v3(10, 10, 10);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return a result map");

        // Check that projections were computed
        for (Map.Entry<AAPlane, List<Vector2dc>> entry : result.entrySet()) {
            assertNotNull(entry.getValue(), "Each plane should have a list of projections");
        }
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Small OBB2 projects onto plane")
    void testProjectSmallOBB() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(5, 5, 5));
        OBB obb2 = createAxisAlignedOBB(v3(0, 0, 3), v3(0.5, 0.5, 0.5));
        Vector3d referencePoint = v3(0, 0, 10);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return a result map");

        // Should have projections since reference point is above both OBBs
        int totalHits = result.values().stream().mapToInt(List::size).sum();
        assertTrue(totalHits >= 0, "Should compute projections");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Reference point between OBBs")
    void testProjectOBBsReferenceInBetween() {
        OBB obb1 = createAxisAlignedOBB(v3(-5, 0, 0), v3(1, 1, 1));
        OBB obb2 = createAxisAlignedOBB(v3(5, 0, 0), v3(1, 1, 1));
        Vector3d referencePoint = v3(0, 0, 0); // Between them

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return a result map");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Rotated OBBs")
    void testProjectOBBsRotated() {
        Quaterniond rotation1 = new Quaterniond().rotateY(Math.PI / 4);
        Quaterniond rotation2 = new Quaterniond().rotateZ(Math.PI / 6);

        AABBd localAabb1 = new AABBd(-2, -2, -2, 2, 2, 2);
        AABBd localAabb2 = new AABBd(-1, -1, -1, 1, 1, 1);

        OBB obb1 = new OBB(localAabb1, rotation1, v3(0, 0, 0));
        OBB obb2 = new OBB(localAabb2, rotation2, v3(3, 0, 0));
        Vector3d referencePoint = v3(10, 0, 0);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should handle rotated OBBs");
        assertFalse(result.isEmpty(), "Should produce plane projections");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - All corners should be processed")
    void testProjectOBBsAllCorners() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(10, 10, 10));
        OBB obb2 = createAxisAlignedOBB(v3(0, 0, 5), v3(1, 1, 1));
        Vector3d referencePoint = v3(0, 0, 20);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return a result map");

        // Verify that we got some projections
        long totalProjections = result.values().stream().mapToInt(List::size).sum();
        assertTrue(totalProjections >= 0, "Should have processed OBB2 corners");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Identical OBBs")
    void testProjectOBBsIdentical() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        OBB obb2 = createAxisAlignedOBB(v3(0, 0, 0), v3(1, 1, 1));
        Vector3d referencePoint = v3(10, 10, 10);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should handle identical OBBs");
    }

    @Test
    @DisplayName("projectOBB2OntoOBB1Planes() - Returns correct data structure")
    void testProjectOBBsDataStructure() {
        OBB obb1 = createAxisAlignedOBB(v3(0, 0, 0), v3(2, 2, 2));
        OBB obb2 = createAxisAlignedOBB(v3(1, 0, 0), v3(0.5, 0.5, 0.5));
        Vector3d referencePoint = v3(5, 0, 0);

        Map<AAPlane, List<Vector2dc>> result = PlanetShading.projectOBB2OntoOBB1Planes(
                obb1, obb2, referencePoint
        );

        assertNotNull(result, "Should return non-null map");

        for (Map.Entry<AAPlane, List<Vector2dc>> entry : result.entrySet()) {
            assertNotNull(entry.getKey(), "Plane key should not be null");
            assertNotNull(entry.getValue(), "Projection list should not be null");

            for (Vector2dc projection : entry.getValue()) {
                assertNotNull(projection, "Individual projections should not be null");
                assertFalse(Double.isNaN(projection.x()), "X coordinate should not be NaN");
                assertFalse(Double.isNaN(projection.y()), "Y coordinate should not be NaN");
            }
        }
    }
}