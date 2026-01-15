package shipwrights.genesis.math;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OBB (Oriented Bounding Box) class.
 *
 * Note: OBB uses the Separating Axis Theorem (SAT) to calculate distance.
 * For boxes to be truly separated (distance > 0), they must not overlap in ANY dimension.
 */
class OBBTest {

    @Test
    @DisplayName("Should return zero distance for identical boxes")
    void testIdenticalBoxes() {
        AABBd aabb = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation = new Quaterniond();
        Vector3d center = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb, rotation, center);
        OBB box2 = new OBB(aabb, rotation, center);

        double distance = box1.distanceTo(box2);

        assertEquals(0.0, distance, 0.0001, "Identical boxes should have zero distance");
    }

    @Test
    @DisplayName("Should return zero distance when boxes overlap")
    void testOverlappingBoxes() {
        AABBd aabb1 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(0.3, 0, 0);  // Overlaps with box1
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        assertEquals(0.0, distance, 0.0001, "Overlapping boxes should have zero distance");
    }

    @Test
    @DisplayName("Should return correct distance when small boxes are separated along X axis")
    void testSeparatedBoxesAlongXAxis() {
        // Small boxes (0.5x0.5x0.5) that don't overlap in any dimension
        AABBd aabb1 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(2, 0, 0);  // Separated by 2 units
        OBB box2 = new OBB(aabb2, rotation2, center2);

        // Box1 spans X:[-0.25, 0.25], Box2 spans X:[1.75, 2.25]
        // Gap = 1.75 - 0.25 = 1.5
        double distance = box1.distanceTo(box2);

        assertEquals(1.5, distance, 0.0001, "Separated boxes should have distance of 1.5");
    }

    @Test
    @DisplayName("Should return correct distance when small boxes are separated along Y axis")
    void testSeparatedBoxesAlongYAxis() {
        AABBd aabb1 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(0, 3, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        // Gap = 3 - 0.5 = 2.5
        double distance = box1.distanceTo(box2);

        assertEquals(2.5, distance, 0.0001, "Separated boxes should have distance of 2.5");
    }

    @Test
    @DisplayName("Should return correct distance when small boxes are separated along Z axis")
    void testSeparatedBoxesAlongZAxis() {
        AABBd aabb1 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(0, 0, 1);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        // Gap = 1 - 0.5 = 0.5
        double distance = box1.distanceTo(box2);

        assertEquals(0.5, distance, 0.0001, "Separated boxes should have distance of 0.5");
    }

    @Test
    @DisplayName("Should return zero distance when boxes are barely touching")
    void testBoxesBarelyTouching() {
        AABBd aabb1 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(1, 0, 0);  // Touching at x=0.5 and x=0.5
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        assertEquals(0.0, distance, 0.0001, "Touching boxes should have zero distance");
    }

    @Test
    @DisplayName("Should return correct distance with different sized boxes")
    void testDifferentSizedBoxes() {
        AABBd aabb1 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);  // 1x1x1 box
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-1, -1, -1, 1, 1, 1);  // 2x2x2 box
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(5, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        // Box1 X-extent: 0.5, Box2 X-extent: 1
        // Gap = 5 - 0.5 - 1 = 3.5
        double distance = box1.distanceTo(box2);

        assertEquals(3.5, distance, 0.0001, "Distance between different sized boxes should be 3.5");
    }

    @Test
    @DisplayName("Should return symmetric distance (A to B equals B to A)")
    void testSymmetricDistance() {
        AABBd aabb1 = new AABBd(-0.3, -0.3, -0.3, 0.3, 0.3, 0.3);
        Quaterniond rotation1 = new Quaterniond().rotateX(Math.toRadians(30));
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.2, -0.2, 0.5, 0.2, 0.2);
        Quaterniond rotation2 = new Quaterniond().rotateY(Math.toRadians(45));
        Vector3d center2 = new Vector3d(3, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distanceAtoB = box1.distanceTo(box2);
        double distanceBtoA = box2.distanceTo(box1);

        assertEquals(distanceAtoB, distanceBtoA, 0.0001, "Distance should be symmetric");
        assertTrue(distanceAtoB > 0, "Boxes should be separated");
    }

    @Test
    @DisplayName("Should handle rotated boxes")
    void testRotatedBoxes() {
        AABBd aabb1 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation2 = new Quaterniond().rotateZ(Math.toRadians(45));
        Vector3d center2 = new Vector3d(3, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        // Rotated box has larger projection, but should still be separated
        assertTrue(distance > 1.5, "Distance should be greater than 1.5");
        assertTrue(distance < 2.5, "Distance should be less than 2.5");
    }

    @Test
    @DisplayName("Should handle very small boxes")
    void testVerySmallBoxes() {
        AABBd aabb1 = new AABBd(-0.01, -0.01, -0.01, 0.01, 0.01, 0.01);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.01, -0.01, -0.01, 0.01, 0.01, 0.01);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(0.5, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        assertEquals(0.48, distance, 0.01, "Distance between small boxes should be approximately 0.48");
    }

    @Test
    @DisplayName("Should return Euclidean distance for diagonally separated boxes")
    void testDiagonalSeparation() {
        // Small boxes separated diagonally
        AABBd aabb1 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(3, 3, 3);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        // Closest points: (0.5, 0.5, 0.5) and (2.5, 2.5, 2.5)
        // Distance = sqrt((2-0)^2 + (2-0)^2 + (2-0)^2) = sqrt(12) ≈ 3.464
        assertEquals(Math.sqrt(12), distance, 0.01, "Diagonal separation should return Euclidean distance");
    }

    @Test
    @DisplayName("Should return zero for deeply overlapping boxes")
    void testDeeplyOverlappingBoxes() {
        AABBd aabb1 = new AABBd(-2, -2, -2, 2, 2, 2);
        Quaterniond rotation1 = new Quaterniond();
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        Quaterniond rotation2 = new Quaterniond();
        Vector3d center2 = new Vector3d(0, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        assertEquals(0.0, distance, 0.0001, "Deeply overlapping boxes should have zero distance");
    }

    @Test
    @DisplayName("Should handle complex rotations on multiple axes")
    void testComplexRotations() {
        AABBd aabb1 = new AABBd(-0.3, -0.3, -0.3, 0.3, 0.3, 0.3);
        Quaterniond rotation1 = new Quaterniond()
                .rotateX(Math.toRadians(30))
                .rotateY(Math.toRadians(45))
                .rotateZ(Math.toRadians(60));
        Vector3d center1 = new Vector3d(0, 0, 0);
        OBB box1 = new OBB(aabb1, rotation1, center1);

        AABBd aabb2 = new AABBd(-0.3, -0.3, -0.3, 0.3, 0.3, 0.3);
        Quaterniond rotation2 = new Quaterniond()
                .rotateX(Math.toRadians(15))
                .rotateY(Math.toRadians(75))
                .rotateZ(Math.toRadians(30));
        Vector3d center2 = new Vector3d(5, 0, 0);
        OBB box2 = new OBB(aabb2, rotation2, center2);

        double distance = box1.distanceTo(box2);

        assertTrue(distance > 3.0, "Distance should be positive and reasonably large");
        assertTrue(distance < 6.0, "Distance should be less than center separation");
    }

    @Test
    @DisplayName("createCube should create a cube-shaped OBB with correct dimensions")
    void testCreateCube() {
        double sideLength = 2.0;
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(45));
        Vector3d center = new Vector3d(5, 10, 15);

        OBB cube = OBB.createCube(sideLength, rotation, center);

        // Verify the local AABB has correct dimensions (should be -1 to 1 on all axes)
        Vector3d extent = cube.localAabb().extent(new Vector3d());
        assertEquals(1.0, extent.x, 0.0001, "Half-extent X should be 1.0");
        assertEquals(1.0, extent.y, 0.0001, "Half-extent Y should be 1.0");
        assertEquals(1.0, extent.z, 0.0001, "Half-extent Z should be 1.0");

        // Verify center is set correctly
        assertEquals(5.0, cube.center().x(), 0.0001, "Center X should be 5.0");
        assertEquals(10.0, cube.center().y(), 0.0001, "Center Y should be 10.0");
        assertEquals(15.0, cube.center().z(), 0.0001, "Center Z should be 15.0");

        // Verify orientation is set correctly
        assertEquals(rotation.x, cube.orientation().x(), 0.0001, "Orientation X should match");
        assertEquals(rotation.y, cube.orientation().y(), 0.0001, "Orientation Y should match");
        assertEquals(rotation.z, cube.orientation().z(), 0.0001, "Orientation Z should match");
        assertEquals(rotation.w, cube.orientation().w(), 0.0001, "Orientation W should match");
    }

    @Test
    @DisplayName("createCube should create cubes that correctly calculate distance")
    void testCreateCubeDistance() {
        Quaterniond noRotation = new Quaterniond();

        // Create two 1x1x1 cubes separated by 2 units along X axis
        OBB cube1 = OBB.createCube(1.0, noRotation, new Vector3d(0, 0, 0));
        OBB cube2 = OBB.createCube(1.0, noRotation, new Vector3d(2, 0, 0));

        double distance = cube1.distanceTo(cube2);

        // Cubes span from -0.5 to 0.5, so they're touching at x=0.5 and x=1.5
        // Distance should be 1.0
        assertEquals(1.0, distance, 0.0001, "Distance between touching 1x1x1 cubes should be 1.0");
    }

    @Test
    @DisplayName("fromAABB should create OBB from centered AABB")
    void testFromAABBCentered() {
        // AABB centered at origin
        AABBd aabb = new AABBd(-2, -3, -4, 2, 3, 4);

        OBB obb = OBB.fromAABB(aabb);

        // Verify center is at origin
        assertEquals(0.0, obb.center().x(), 0.0001, "Center X should be 0");
        assertEquals(0.0, obb.center().y(), 0.0001, "Center Y should be 0");
        assertEquals(0.0, obb.center().z(), 0.0001, "Center Z should be 0");

        // Verify local AABB has correct half-extents (2, 3, 4)
        Vector3d extent = obb.localAabb().extent(new Vector3d());
        assertEquals(2.0, extent.x, 0.0001, "Half-extent X should be 2.0");
        assertEquals(3.0, extent.y, 0.0001, "Half-extent Y should be 3.0");
        assertEquals(4.0, extent.z, 0.0001, "Half-extent Z should be 4.0");

        // Verify identity rotation
        Quaterniond expectedRotation = new Quaterniond();
        assertEquals(expectedRotation.x, obb.orientation().x(), 0.0001);
        assertEquals(expectedRotation.y, obb.orientation().y(), 0.0001);
        assertEquals(expectedRotation.z, obb.orientation().z(), 0.0001);
        assertEquals(expectedRotation.w, obb.orientation().w(), 0.0001);
    }

    @Test
    @DisplayName("fromAABB should create OBB from off-center AABB")
    void testFromAABBOffCenter() {
        // AABB offset from origin: from (5, 10, 15) to (9, 16, 23)
        AABBd aabb = new AABBd(5, 10, 15, 9, 16, 23);

        OBB obb = OBB.fromAABB(aabb);

        // Verify center is at (7, 13, 19)
        assertEquals(7.0, obb.center().x(), 0.0001, "Center X should be 7.0");
        assertEquals(13.0, obb.center().y(), 0.0001, "Center Y should be 13.0");
        assertEquals(19.0, obb.center().z(), 0.0001, "Center Z should be 19.0");

        // Verify local AABB has correct half-extents: (9-5)/2=2, (16-10)/2=3, (23-15)/2=4
        Vector3d extent = obb.localAabb().extent(new Vector3d());
        assertEquals(2.0, extent.x, 0.0001, "Half-extent X should be 2.0");
        assertEquals(3.0, extent.y, 0.0001, "Half-extent Y should be 3.0");
        assertEquals(4.0, extent.z, 0.0001, "Half-extent Z should be 4.0");
    }

    @Test
    @DisplayName("fromAABB should create OBB that correctly calculates distance")
    void testFromAABBDistance() {
        // Create two AABBs and convert to OBBs
        AABBd aabb1 = new AABBd(-1, -1, -1, 1, 1, 1);  // Centered at origin, 2x2x2
        AABBd aabb2 = new AABBd(4, -1, -1, 6, 1, 1);   // Centered at (5, 0, 0), 2x2x2

        OBB obb1 = OBB.fromAABB(aabb1);
        OBB obb2 = OBB.fromAABB(aabb2);

        double distance = obb1.distanceTo(obb2);

        // Box 1 extends from -1 to 1 on X, Box 2 extends from 4 to 6 on X
        // Distance should be 4 - 1 = 3
        assertEquals(3.0, distance, 0.0001, "Distance between AABBs should be 3.0");
    }

    @Test
    @DisplayName("fromAABB should handle single-point AABB")
    void testFromAABBSinglePoint() {
        // AABB that's just a point at (5, 10, 15)
        AABBd aabb = new AABBd(5, 10, 15, 5, 10, 15);

        OBB obb = OBB.fromAABB(aabb);

        // Verify center
        assertEquals(5.0, obb.center().x(), 0.0001, "Center X should be 5.0");
        assertEquals(10.0, obb.center().y(), 0.0001, "Center Y should be 10.0");
        assertEquals(15.0, obb.center().z(), 0.0001, "Center Z should be 15.0");

        // Verify zero extents
        Vector3d extent = obb.localAabb().extent(new Vector3d());
        assertEquals(0.0, extent.x, 0.0001, "Half-extent X should be 0.0");
        assertEquals(0.0, extent.y, 0.0001, "Half-extent Y should be 0.0");
        assertEquals(0.0, extent.z, 0.0001, "Half-extent Z should be 0.0");
    }
    private static final double EPS = 1e-9;

    @Test
    void identityTransform_preservesCenterAndExtents() {
        AABBi shipyardAABB = new AABBi(0, 0, 0, 10, 20, 30);
        Matrix4d identity = new Matrix4d();

        OBB obb = OBB.fromShip(shipyardAABB, identity);

        // Center should be the AABB center
        Vector3d expectedCenter = new Vector3d(5, 10, 15);
        assertTrue(obb.center().equals(expectedCenter, EPS));

        // Orientation should be identity
        Quaterniond expectedOrientation = new Quaterniond();
        assertTrue(obb.orientation().equals(expectedOrientation, EPS));

        // Half-extents should match original AABB extents
        AABBdc local = obb.localAabb();
        assertEquals(-5, local.minX(), EPS);
        assertEquals( 5, local.maxX(), EPS);
        assertEquals(-10, local.minY(), EPS);
        assertEquals( 10, local.maxY(), EPS);
        assertEquals(-15, local.minZ(), EPS);
        assertEquals( 15, local.maxZ(), EPS);
    }

    @Test
    void translationOnly_movesCenterButKeepsOrientationAndSize() {
        AABBi shipyardAABB = new AABBi(-2, -2, -2, 2, 2, 2);
        Matrix4d transform = new Matrix4d().translation(10, 20, 30);

        OBB obb = OBB.fromShip(shipyardAABB, transform);

        // Center should be translated
        Vector3d expectedCenter = new Vector3d(10, 20, 30);
        assertTrue(obb.center().equals(expectedCenter, EPS));

        // Orientation remains identity
        assertTrue(obb.orientation().equals(new Quaterniond(), EPS));

        // Extents unchanged
        AABBdc local = obb.localAabb();
        assertEquals(-2, local.minX(), EPS);
        assertEquals( 2, local.maxX(), EPS);
        assertEquals(-2, local.minY(), EPS);
        assertEquals( 2, local.maxY(), EPS);
        assertEquals(-2, local.minZ(), EPS);
        assertEquals( 2, local.maxZ(), EPS);
    }

    @Test
    void rotationOnly_affectsOrientationButNotCenterOrSize() {
        AABBi shipyardAABB = new AABBi(-2, -2, -2, 2, 2, 2);
        Matrix4d transform = new Matrix4d()
                .rotateY(Math.toRadians(90));

        OBB obb = OBB.fromShip(shipyardAABB, transform);

        // Center remains at original center
        Vector3d expectedCenter = new Vector3d(0, 0, 0);
        assertTrue(obb.center().equals(expectedCenter, EPS));

        // Orientation should match rotation
        Quaterniond expectedOrientation = new Quaterniond().rotateY(Math.toRadians(90));
        assertTrue(obb.orientation().equals(expectedOrientation, EPS));

        // Size unchanged
        AABBdc local = obb.localAabb();
        assertEquals(-2, local.minX(), EPS);
        assertEquals( 2, local.maxX(), EPS);
        assertEquals(-2, local.minY(), EPS);
        assertEquals( 2, local.maxY(), EPS);
        assertEquals(-2, local.minZ(), EPS);
        assertEquals( 2, local.maxZ(), EPS);
    }

    @Test
    void uniformScaling_scalesLocalAabbButKeepsCenterAndOrientation() {
        AABBi shipyardAABB = new AABBi(-1, -2, -3, 1, 2, 3);
        Matrix4d transform = new Matrix4d()
                .scale(2.0);

        OBB obb = OBB.fromShip(shipyardAABB, transform);

        // Center remains at origin (AABB center is (0,0,0))
        Vector3d expectedCenter = new Vector3d(0, 0, 0);
        assertTrue(obb.center().equals(expectedCenter, EPS));

        // Orientation remains identity
        assertTrue(obb.orientation().equals(new Quaterniond(), EPS), "actual value: " + obb.orientation());

        // Local AABB must be centered at origin and uniformly scaled
        AABBdc local = obb.localAabb();
        assertEquals(-2, local.minX(), EPS);
        assertEquals( 2, local.maxX(), EPS);
        assertEquals(-4, local.minY(), EPS);
        assertEquals( 4, local.maxY(), EPS);
        assertEquals(-6, local.minZ(), EPS);
        assertEquals( 6, local.maxZ(), EPS);
    }


    @Test
    void rotationAndTranslation_affectOrientationAndCenterButNotLocalAabb() {
        AABBi shipyardAABB = new AABBi(-2, -2, -2, 2, 2, 2);

        Matrix4d transform = new Matrix4d()
                .translate(10, 5, -7)
                .rotateY(Math.toRadians(90));

        OBB obb = OBB.fromShip(shipyardAABB, transform);

        // Center should be rotated (no-op here) then translated
        Vector3d expectedCenter = new Vector3d(10, 5, -7);
        assertTrue(obb.center().equals(expectedCenter, EPS), "actual value: " + obb.center());

        // Orientation should include rotation only (no scale)
        Quaterniond expectedOrientation = new Quaterniond()
                .rotateY(Math.toRadians(90));
        assertTrue(obb.orientation().equals(expectedOrientation, EPS));

        // Local AABB must remain centered on origin
        AABBdc local = obb.localAabb();
        assertEquals(-2, local.minX(), EPS);
        assertEquals( 2, local.maxX(), EPS);
        assertEquals(-2, local.minY(), EPS);
        assertEquals( 2, local.maxY(), EPS);
        assertEquals(-2, local.minZ(), EPS);
        assertEquals( 2, local.maxZ(), EPS);
    }

}
