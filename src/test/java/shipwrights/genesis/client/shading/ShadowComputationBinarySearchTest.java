package shipwrights.genesis.client.shading;

import org.joml.Quaterniond;
import org.joml.Vector2dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Binary search through the shadow computation pipeline to isolate the bug.
 *
 * Pipeline:
 * 1. Transform occluder corners to target's local space
 * 2. Get facing planes of target
 * 3. For each plane, intersect rays (light -> corner) with plane
 * 4. Clip projected polygon to plane bounds
 * 5. Merge and finalize shadows
 */
class ShadowComputationBinarySearchTest {

    // Test scenario: Target rotated 45° around Y, occluder at z=200, light at z=500
    private static final Vector3dc TARGET_CENTER = new Vector3d(0, 0, 0);
    private static final Quaterniond TARGET_ROTATION = new Quaterniond().rotateY(Math.toRadians(45));
    private static final double TARGET_SIZE = 100;

    private static final Vector3dc OCCLUDER_CENTER = new Vector3d(0, 0, 200);
    private static final Quaterniond OCCLUDER_ROTATION = new Quaterniond(); // Identity
    private static final double OCCLUDER_SIZE = 50;

    private static final Vector3dc LIGHT_POSITION = new Vector3d(0, 0, 500);

    @Test
    @DisplayName("STEP 1: Verify occluder corners are transformed correctly to target's local space")
    void testStep1_OccluderCornersTransformation() {
        // Setup
        OBB targetOBB = createTargetOBB();
        OBB occluderOBB = createOccluderOBB();

        System.out.println("\n========== STEP 1: Occluder Corner Transformation ==========");
        System.out.println("Target: center=" + TARGET_CENTER + ", rotation=45° Y");
        System.out.println("Occluder: center=" + OCCLUDER_CENTER + ", no rotation");
        System.out.println("Light: " + LIGHT_POSITION);

        // Transform light to target's local space
        Vector3d lightLocal = PlanetShading.worldToLocal(LIGHT_POSITION, targetOBB);
        System.out.println("\nLight in target's local space: " + lightLocal);

        // Transform occluder corners to target's local space
        Vector3dc[] occluderCorners = occluderOBB.getCorners();
        System.out.println("\nOccluder has " + occluderCorners.length + " corners");

        for (int i = 0; i < Math.min(4, occluderCorners.length); i++) {
            Vector3d cornerLocal = PlanetShading.worldToLocal(occluderCorners[i], targetOBB);
            System.out.println("  Corner " + i + ": world=" + occluderCorners[i] + " -> local=" + cornerLocal);

            // Verify corner is between target (at origin) and light
            // Light is at roughly (-354, 0, 354), occluder should be between (0,0,0) and that
            boolean betweenTargetAndLight =
                (cornerLocal.z > -50) && // Past target's back face
                (cornerLocal.z < lightLocal.z); // Before light

            System.out.println("    Between target and light in Z? " + betweenTargetAndLight);
            assertTrue(betweenTargetAndLight,
                "Corner " + i + " should be between target and light in Z axis. Corner Z=" + cornerLocal.z + ", Light Z=" + lightLocal.z);
        }
    }

    @Test
    @DisplayName("STEP 2: Verify facing planes are detected correctly for rotated target")
    void testStep2_FacingPlanesDetection() {
        OBB targetOBB = createTargetOBB();

        System.out.println("\n========== STEP 2: Facing Planes Detection ==========");

        List<AAPlane> facingPlanes = PlanetShading.getFacingPlanes(targetOBB, LIGHT_POSITION);

        System.out.println("Detected " + facingPlanes.size() + " facing planes:");
        for (AAPlane plane : facingPlanes) {
            System.out.println("  Plane: normal=" + plane.normal() + ", position=" + plane.position());
        }

        // Should have 2-3 planes facing the light for a rotated cube
        assertTrue(facingPlanes.size() >= 2 && facingPlanes.size() <= 3,
                "Should detect 2-3 facing planes, got " + facingPlanes.size());

        // Verify planes are within cube bounds
        for (AAPlane plane : facingPlanes) {
            double pos = plane.position();
            assertTrue(Math.abs(pos) <= 50.1, "Plane position should be within cube bounds: " + pos);
        }
    }

    @Test
    @DisplayName("STEP 3: Verify ray-plane intersection for NON-ROTATED case (baseline)")
    void testStep3_RayPlaneIntersectionBaseline() {
        // Non-rotated target for baseline
        OBB targetOBB = new OBB(
            new AABBd(-50, -50, -50, 50, 50, 50),
            new Quaterniond(), // No rotation
            new Vector3d(0, 0, 0)
        );

        System.out.println("\n========== STEP 3: Ray-Plane Intersection (Baseline) ==========");

        // Light and occluder in simple positions
        Vector3dc lightLocal = new Vector3d(0, 0, 500);  // Light at +Z
        Vector3dc cornerLocal = new Vector3d(0, 0, 200); // Corner between target and light

        // Test intersection with +Z face (position=50, normal=(0,0,1))
        AAPlane zFace = new AAPlane(new org.joml.Vector3i(0, 0, 1), 50.0);

        Vector2dc hit = PlanetShading.intersectRayWithPlane(lightLocal, cornerLocal, zFace);

        System.out.println("Light (local): " + lightLocal);
        System.out.println("Corner (local): " + cornerLocal);
        System.out.println("Plane: normal=" + zFace.normal() + ", position=" + zFace.position());
        System.out.println("Intersection (2D): " + hit);

        assertNotNull(hit, "Should find intersection");

        // For Z-normal plane, 2D coords are (x, y)
        // Ray from (0,0,500) to (0,0,200) hits z=50 plane at (0,0,50)
        // 2D projection should be (0, 0)
        assertEquals(0.0, hit.x(), 0.1, "X should be 0");
        assertEquals(0.0, hit.y(), 0.1, "Y should be 0");

        System.out.println("✓ Baseline intersection works correctly");
    }

    @Test
    @DisplayName("STEP 4: Verify ray-plane intersection for ROTATED case")
    void testStep4_RayPlaneIntersectionRotated() {
        OBB targetOBB = createTargetOBB();

        System.out.println("\n========== STEP 4: Ray-Plane Intersection (Rotated) ==========");

        // Transform light and a simple occluder point to target's local space
        Vector3d lightLocal = PlanetShading.worldToLocal(LIGHT_POSITION, targetOBB);
        Vector3d occluderCenterLocal = PlanetShading.worldToLocal(OCCLUDER_CENTER, targetOBB);

        System.out.println("Light (world): " + LIGHT_POSITION + " -> (local): " + lightLocal);
        System.out.println("Occluder center (world): " + OCCLUDER_CENTER + " -> (local): " + occluderCenterLocal);

        // Get one facing plane
        List<AAPlane> facingPlanes = PlanetShading.getFacingPlanes(targetOBB, LIGHT_POSITION);
        AAPlane testPlane = facingPlanes.get(0);

        System.out.println("\nTesting plane: normal=" + testPlane.normal() + ", position=" + testPlane.position());

        // Intersect ray from light through occluder center with this plane
        Vector2dc hit = PlanetShading.intersectRayWithPlane(lightLocal, occluderCenterLocal, testPlane);

        if (hit != null) {
            System.out.println("Intersection (2D): " + hit);

            // The 2D coordinates should be within the plane's bounds
            // For a 100x100 cube, plane bounds are [-50, 50] x [-50, 50]
            boolean within2DBounds =
                Math.abs(hit.x()) <= 50 &&
                Math.abs(hit.y()) <= 50;

            System.out.println("Within 2D plane bounds [-50, 50]? " + within2DBounds);

            if (!within2DBounds) {
                System.out.println("⚠️  BUG FOUND: 2D intersection is OUTSIDE plane bounds!");
                System.out.println("   This is likely why clipping removes all vertices.");
            }

            // Don't assert - we want to see the values even if they're wrong
        } else {
            System.out.println("No intersection found (ray might be parallel to plane)");
        }
    }

    @Test
    @DisplayName("STEP 5: Full projection - verify complete projectOBB2OntoOBB1Planes")
    void testStep5_FullProjection() {
        OBB targetOBB = createTargetOBB();
        OBB occluderOBB = createOccluderOBB();

        System.out.println("\n========== STEP 5: Full Projection ==========");

        Map<AAPlane, List<Vector2dc>> projections =
            PlanetShading.projectOBB2OntoOBB1Planes(targetOBB, occluderOBB, LIGHT_POSITION);

        System.out.println("Projected onto " + projections.size() + " planes:");

        for (Map.Entry<AAPlane, List<Vector2dc>> entry : projections.entrySet()) {
            AAPlane plane = entry.getKey();
            List<Vector2dc> points = entry.getValue();

            System.out.println("\nPlane: normal=" + plane.normal() + ", position=" + plane.position());
            System.out.println("  Projected " + points.size() + " points:");

            int outOfBounds = 0;
            for (int i = 0; i < Math.min(4, points.size()); i++) {
                Vector2dc p = points.get(i);
                boolean inBounds = Math.abs(p.x()) <= 50 && Math.abs(p.y()) <= 50;
                System.out.println("    Point " + i + ": " + p + " " + (inBounds ? "✓" : "⚠️ OUT OF BOUNDS"));
                if (!inBounds) outOfBounds++;
            }

            if (outOfBounds > 0) {
                System.out.println("  ⚠️  " + outOfBounds + "/" + points.size() + " points are outside plane bounds!");
                System.out.println("  This will cause clipping to fail.");
            }
        }
    }

    @Test
    @DisplayName("STEP 6: Isolate the ray-plane intersection math")
    void testStep6_IsolateRayPlaneIntersectionMath() {
        System.out.println("\n========== STEP 6: Isolate Ray-Plane Intersection Math ==========");

        // Use a concrete example from the rotated case
        // Light at: (-353.55, 0, 353.55) in local space
        // Occluder corner at: roughly (-141, -25, 106) in local space (estimated)
        // Plane: Z-face at position=50, normal=(0,0,1)

        Vector3d lightLocal = new Vector3d(-353.55, 0, 353.55);
        Vector3d cornerLocal = new Vector3d(-141, -25, 106);
        AAPlane plane = new AAPlane(new org.joml.Vector3i(0, 0, 1), 50.0);

        System.out.println("Ray origin (light): " + lightLocal);
        System.out.println("Ray target (corner): " + cornerLocal);
        System.out.println("Plane: normal=" + plane.normal() + ", position=" + plane.position());

        // Manual calculation:
        // Ray direction: corner - light = (-141 - (-353.55), -25 - 0, 106 - 353.55)
        //                              = (212.55, -25, -247.55)
        Vector3d dir = new Vector3d(cornerLocal).sub(lightLocal).normalize();
        System.out.println("Ray direction (normalized): " + dir);

        // Plane equation: normal · point = position
        // For Z-plane: z = 50
        // Ray: P = light + t * dir
        // At intersection: P.z = 50
        // light.z + t * dir.z = 50
        // t = (50 - light.z) / dir.z
        Vector3d planeNormal = new Vector3d(0, 0, 1);
        double denom = planeNormal.dot(dir);
        double t = (plane.position() - planeNormal.dot(lightLocal)) / denom;

        System.out.println("t parameter: " + t);
        System.out.println("Is t positive? " + (t > 0));

        if (t > 0) {
            Vector3d hitPoint = new Vector3d(lightLocal).add(new Vector3d(dir).mul(t));
            System.out.println("3D intersection point: " + hitPoint);

            // Convert to 2D (for Z-plane, take x and y)
            Vector2dc hit2D = new org.joml.Vector2d(hitPoint.x, hitPoint.y);
            System.out.println("2D intersection point: " + hit2D);

            // Check if within bounds
            boolean inBounds = Math.abs(hit2D.x()) <= 50 && Math.abs(hit2D.y()) <= 50;
            System.out.println("Within plane bounds [-50, 50]? " + inBounds);

            // Now call the actual function and compare
            Vector2dc actualHit = PlanetShading.intersectRayWithPlane(lightLocal, cornerLocal, plane);
            System.out.println("\nActual function result: " + actualHit);

            if (actualHit != null) {
                double diffX = Math.abs(actualHit.x() - hit2D.x());
                double diffY = Math.abs(actualHit.y() - hit2D.y());
                System.out.println("Difference from manual calculation: dx=" + diffX + ", dy=" + diffY);

                if (diffX > 0.1 || diffY > 0.1) {
                    System.out.println("⚠️  BUG FOUND: Function result differs from manual calculation!");
                }
            }
        } else {
            System.out.println("⚠️  Ray is pointing away from plane (t < 0)");
        }
    }

    // Helper methods
    private OBB createTargetOBB() {
        return new OBB(
            new AABBd(-TARGET_SIZE/2, -TARGET_SIZE/2, -TARGET_SIZE/2,
                      TARGET_SIZE/2, TARGET_SIZE/2, TARGET_SIZE/2),
            TARGET_ROTATION,
            TARGET_CENTER
        );
    }

    private OBB createOccluderOBB() {
        return new OBB(
            new AABBd(-OCCLUDER_SIZE/2, -OCCLUDER_SIZE/2, -OCCLUDER_SIZE/2,
                      OCCLUDER_SIZE/2, OCCLUDER_SIZE/2, OCCLUDER_SIZE/2),
            OCCLUDER_ROTATION,
            OCCLUDER_CENTER
        );
    }
}
