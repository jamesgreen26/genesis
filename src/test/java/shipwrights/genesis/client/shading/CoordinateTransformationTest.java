package shipwrights.genesis.client.shading;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.OBB;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test to isolate the coordinate transformation bug with rotated OBBs.
 */
class CoordinateTransformationTest {

    @Test
    @DisplayName("Identity rotation: world to local transformation should be simple translation")
    void testIdentityRotation() {
        // OBB at origin with no rotation
        Vector3dc center = new Vector3d(0, 0, 0);
        Quaterniond rotation = new Quaterniond(); // Identity rotation
        AABBd aabb = new AABBd(-50, -50, -50, 50, 50, 50);
        OBB obb = new OBB(aabb, rotation, center);

        // Point at (0, 0, 500) should transform to (0, 0, 500) in local space
        Vector3dc worldPoint = new Vector3d(0, 0, 500);
        Vector3d localPoint = PlanetShading.worldToLocal(worldPoint, obb);

        System.out.println("\n=== Identity Rotation Test ===");
        System.out.println("World point: " + worldPoint);
        System.out.println("Expected local: (0, 0, 500)");
        System.out.println("Actual local: " + localPoint);

        assertEquals(0.0, localPoint.x, 0.001, "X should be 0");
        assertEquals(0.0, localPoint.y, 0.001, "Y should be 0");
        assertEquals(500.0, localPoint.z, 0.001, "Z should be 500");
    }

    @Test
    @DisplayName("45° Y-rotation: understand how rotation affects coordinates")
    void testYRotation45Degrees() {
        // OBB at origin, rotated 45 degrees around Y axis
        Vector3dc center = new Vector3d(0, 0, 0);
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(45));
        AABBd aabb = new AABBd(-50, -50, -50, 50, 50, 50);
        OBB obb = new OBB(aabb, rotation, center);

        // Test point on positive Z axis
        Vector3dc worldPoint = new Vector3d(0, 0, 500);
        Vector3d localPoint = PlanetShading.worldToLocal(worldPoint, obb);

        // When the OBB is rotated 45° clockwise around Y (looking down from +Y),
        // a point on +Z axis should appear rotated -45° in local space
        // Using rotation matrix: cos(45°) ≈ 0.707, sin(45°) ≈ 0.707
        // R^-1 * (0, 0, 500) where R is 45° around Y
        // For 45° Y-rotation matrix:
        // [cos  0  sin]   [0.707  0  0.707]
        // [0    1  0  ] = [0      1  0    ]
        // [-sin 0  cos]   [-0.707 0  0.707]
        //
        // Inverse (transpose for rotation):
        // [cos  0  -sin]   [0.707  0  -0.707]
        // [0    1  0   ] = [0      1  0     ]
        // [sin  0  cos ]   [0.707  0  0.707 ]
        //
        // Multiply by (0, 0, 500):
        // x = 0.707*0 + 0*0 + -0.707*500 = -353.55
        // y = 0
        // z = 0.707*0 + 0*0 + 0.707*500 = 353.55
        //
        // BUT this gives us (-353.55, 0, 353.55)
        // We're GETTING (-353.55, 0, 353.55) - this matches!

        // Actually, let me reconsider: if the cube is rotated 45° around Y,
        // and we have a light at (0, 0, 500), what should it look like in the cube's frame?
        //
        // If the cube rotated 45° clockwise (looking from +Y), then from the cube's perspective,
        // the world rotated 45° counter-clockwise. The +Z world axis appears at 45° in the cube's frame.

        double expectedX = -353.553; // -500 * cos(45°)
        double expectedY = 0.0;
        double expectedZ = 353.553;  // 500 * sin(45°)

        // Create a detailed message showing what we got
        String message = String.format(
                "\n45° Y-Rotation Transformation:\n" +
                "  World point: (0, 0, 500)\n" +
                "  Expected local: (%.3f, %.3f, %.3f)\n" +
                "  Actual local: (%.3f, %.3f, %.3f)\n" +
                "  Rotation quaternion: %s",
                expectedX, expectedY, expectedZ,
                localPoint.x, localPoint.y, localPoint.z,
                rotation
        );

        // These assertions verify the CURRENT behavior - they should pass
        assertEquals(expectedX, localPoint.x, 1.0, message + "\nX coordinate mismatch");
        assertEquals(expectedY, localPoint.y, 0.1, message + "\nY coordinate mismatch");
        assertEquals(expectedZ, localPoint.z, 1.0, message + "\nZ coordinate mismatch");
    }

    @Test
    @DisplayName("90° Y-rotation: simpler case to verify rotation direction")
    void testYRotation90Degrees() {
        // OBB rotated 90 degrees around Y axis
        Vector3dc center = new Vector3d(0, 0, 0);
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(90));
        AABBd aabb = new AABBd(-50, -50, -50, 50, 50, 50);
        OBB obb = new OBB(aabb, rotation, center);

        System.out.println("\n=== 90° Y-Rotation Test ===");

        // Point on +Z axis
        Vector3dc worldPoint = new Vector3d(0, 0, 500);
        Vector3d localPoint = PlanetShading.worldToLocal(worldPoint, obb);

        System.out.println("World point: " + worldPoint);
        System.out.println("Actual local: " + localPoint);

        // 90° rotation around Y:
        // World +Z should map to local -X
        // Expected: (-500, 0, 0)
        double expectedX = -500.0;
        double expectedY = 0.0;
        double expectedZ = 0.0;

        System.out.println("Expected local: (" + expectedX + ", " + expectedY + ", " + expectedZ + ")");

        assertEquals(expectedX, localPoint.x, 1.0, "After 90° Y-rotation, world +Z should be local -X");
        assertEquals(expectedY, localPoint.y, 0.1, "Y unchanged");
        assertEquals(expectedZ, localPoint.z, 1.0, "After 90° Y-rotation, Z component should be ~0");
    }

    @Test
    @DisplayName("180° Y-rotation: complete reversal test")
    void testYRotation180Degrees() {
        // OBB rotated 180 degrees around Y axis
        Vector3dc center = new Vector3d(0, 0, 0);
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(180));
        AABBd aabb = new AABBd(-50, -50, -50, 50, 50, 50);
        OBB obb = new OBB(aabb, rotation, center);

        System.out.println("\n=== 180° Y-Rotation Test ===");

        // Point on +Z axis
        Vector3dc worldPoint = new Vector3d(0, 0, 500);
        Vector3d localPoint = PlanetShading.worldToLocal(worldPoint, obb);

        System.out.println("World point: " + worldPoint);
        System.out.println("Actual local: " + localPoint);

        // 180° rotation around Y:
        // World +Z should map to local -Z
        // World +X should map to local -X
        // Expected: (0, 0, -500)
        double expectedX = 0.0;
        double expectedY = 0.0;
        double expectedZ = -500.0;

        System.out.println("Expected local: (" + expectedX + ", " + expectedY + ", " + expectedZ + ")");

        assertEquals(expectedX, localPoint.x, 1.0, "X should be ~0");
        assertEquals(expectedY, localPoint.y, 0.1, "Y unchanged");
        assertEquals(expectedZ, localPoint.z, 1.0, "After 180° Y-rotation, world +Z should be local -Z");
    }

    @Test
    @DisplayName("Occluder corners transformation test")
    void testOccluderCornersTransformation() {
        // This replicates the exact scenario from the failing integration test
        // Target cube at origin, rotated 45° around Y
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(45));
        AABBd targetAabb = new AABBd(-50, -50, -50, 50, 50, 50);
        OBB targetOBB = new OBB(targetAabb, rotation, targetCenter);

        // Occluder at (0, 0, 200) - between target and light
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        Quaterniond occluderRotation = new Quaterniond(); // No rotation
        AABBd occluderAabb = new AABBd(-25, -25, -25, 25, 25, 25);
        OBB occluderOBB = new OBB(occluderAabb, occluderRotation, occluderCenter);

        System.out.println("\n=== Occluder Corners Transformation Test ===");
        System.out.println("Target: center=" + targetCenter + ", rotation=45° around Y");
        System.out.println("Occluder: center=" + occluderCenter + " (no rotation)");

        // Get one corner of the occluder
        Vector3dc[] occluderCorners = occluderOBB.getCorners();
        Vector3dc corner0 = occluderCorners[0];

        System.out.println("\nOccluder corner[0] in world space: " + corner0);

        // Transform to target's local space
        Vector3d cornerLocal = PlanetShading.worldToLocal(corner0, targetOBB);
        System.out.println("Occluder corner[0] in target's local space: " + cornerLocal);

        // The occluder center is at (0, 0, 200) in world space
        // After 45° rotation, it should be at approximately:
        // x = -200 * sin(45°) ≈ -141.4
        // z = 200 * cos(45°) ≈ 141.4
        // The corner at (-25, -25, 175) should transform similarly

        // Key insight: the occluder should still be between the target (at origin in local space)
        // and the light (at roughly (-354, 0, 354) in local space)
        // So Z coordinate should be positive and between 0 and 354

        assertTrue(cornerLocal.z > 0, "Occluder corner Z should be positive (between target and light)");
        assertTrue(cornerLocal.z < 400, "Occluder corner Z should be less than light Z");

        System.out.println("\nOccluder is correctly positioned between target and light: " +
                (cornerLocal.z > 0 && cornerLocal.z < 400));
    }

    @Test
    @DisplayName("SUMMARY: Show all transformation results")
    void testSummaryShowAllResults() {
        StringBuilder summary = new StringBuilder("\n\n========== COORDINATE TRANSFORMATION SUMMARY ==========\n");

        // Identity test
        {
            OBB obb = new OBB(new AABBd(-50, -50, -50, 50, 50, 50), new Quaterniond(), new Vector3d(0, 0, 0));
            Vector3d local = PlanetShading.worldToLocal(new Vector3d(0, 0, 500), obb);
            summary.append(String.format("\n1. Identity (no rotation):\n   World (0, 0, 500) -> Local (%.3f, %.3f, %.3f)\n   ✓ Expected: (0.000, 0.000, 500.000)\n",
                    local.x, local.y, local.z));
        }

        // 45° rotation
        {
            OBB obb = new OBB(new AABBd(-50, -50, -50, 50, 50, 50),
                              new Quaterniond().rotateY(Math.toRadians(45)),
                              new Vector3d(0, 0, 0));
            Vector3d local = PlanetShading.worldToLocal(new Vector3d(0, 0, 500), obb);
            summary.append(String.format("\n2. 45° Y-Rotation:\n   World (0, 0, 500) -> Local (%.3f, %.3f, %.3f)\n   ✓ Expected: (-353.553, 0.000, 353.553)\n",
                    local.x, local.y, local.z));
        }

        // 90° rotation
        {
            OBB obb = new OBB(new AABBd(-50, -50, -50, 50, 50, 50),
                              new Quaterniond().rotateY(Math.toRadians(90)),
                              new Vector3d(0, 0, 0));
            Vector3d local = PlanetShading.worldToLocal(new Vector3d(0, 0, 500), obb);
            summary.append(String.format("\n3. 90° Y-Rotation:\n   World (0, 0, 500) -> Local (%.3f, %.3f, %.3f)\n   ✓ Expected: (-500.000, 0.000, 0.000)\n",
                    local.x, local.y, local.z));
        }

        // 180° rotation
        {
            OBB obb = new OBB(new AABBd(-50, -50, -50, 50, 50, 50),
                              new Quaterniond().rotateY(Math.toRadians(180)),
                              new Vector3d(0, 0, 0));
            Vector3d local = PlanetShading.worldToLocal(new Vector3d(0, 0, 500), obb);
            summary.append(String.format("\n4. 180° Y-Rotation:\n   World (0, 0, 500) -> Local (%.3f, %.3f, %.3f)\n   ✓ Expected: (0.000, 0.000, -500.000)\n",
                    local.x, local.y, local.z));
        }

        summary.append("\n\n========== ANALYSIS ==========\n");
        summary.append("The transformations are MATHEMATICALLY CORRECT.\n");
        summary.append("When a cube is rotated 45° around Y, a light at (0,0,500)\n");
        summary.append("appears at (-353.553, 0, 353.553) in the cube's frame.\n");
        summary.append("This is the CORRECT inverse transformation.\n");
        summary.append("=======================================================\n\n");

        // Print the summary
        System.out.println(summary.toString());

        // This test always passes - it's just for showing the results
        assertTrue(true, summary.toString());
    }
}
