package shipwrights.genesis.client.shading;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBd;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for shadow computation that replicate the exact scenario
 * used in PlanetRenderer to debug why shadows aren't appearing.
 */
class ShadowRenderingIntegrationTest {

    @Test
    @DisplayName("Simple two-cube scenario: one cube occludes another from a light source")
    void testSimpleTwoCubeOcclusion() {
        // Setup: Create a target cube at origin and an occluder between it and the light
        // This replicates the simplest shadow scenario

        // Target cube at origin, size 100x100x100
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        OBB targetOBB = new OBB(targetAabb, new Quaterniond(), targetCenter);

        // Occluder cube between target and light, size 50x50x50
        // Positioned at z=200 (between target at z=0 and light at z=500)
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        double occluderSize = 50;
        AABBd occluderAabb = new AABBd(
                -occluderSize / 2, -occluderSize / 2, -occluderSize / 2,
                occluderSize / 2, occluderSize / 2, occluderSize / 2
        );
        OBB occluderOBB = new OBB(occluderAabb, new Quaterniond(), occluderCenter);

        // Light source position (star)
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        // Compute shadows (exactly as PlanetRenderer does)
        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluderOBB),
                lightPosition
        );

        // Assertions
        System.out.println("\n=== Simple Two-Cube Occlusion Test ===");
        System.out.println("Target: " + targetCenter + ", size: " + targetSize);
        System.out.println("Occluder: " + occluderCenter + ", size: " + occluderSize);
        System.out.println("Light: " + lightPosition);
        System.out.println("Shadows computed: " + shadows.size());

        assertFalse(shadows.isEmpty(), "Should produce at least one shadow");

        for (int i = 0; i < shadows.size(); i++) {
            FaceShadow shadow = shadows.get(i);
            System.out.println("\nShadow " + i + ":");
            System.out.println("  Plane normal: " + shadow.plane().normal());
            System.out.println("  Plane position: " + shadow.plane().position());
            System.out.println("  Polygon vertices: " + shadow.polygon().size());
            for (int j = 0; j < shadow.polygon().size(); j++) {
                System.out.println("    Vertex " + j + ": " + shadow.polygon().get(j));
            }
        }

        // Verify we have a shadow on the +Z face (front face)
        boolean hasZFaceShadow = shadows.stream()
                .anyMatch(s -> s.plane().normal().z() == 1);
        assertTrue(hasZFaceShadow, "Should have shadow on +Z face (front of target cube)");
    }

    @Test
    @DisplayName("Off-axis occlusion: occluder is offset from center")
    void testOffAxisOcclusion() {
        // Target cube at origin
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        OBB targetOBB = new OBB(targetAabb, new Quaterniond(), targetCenter);

        // Occluder offset to the right and up
        Vector3dc occluderCenter = new Vector3d(30, 20, 200);
        double occluderSize = 40;
        AABBd occluderAabb = new AABBd(
                -occluderSize / 2, -occluderSize / 2, -occluderSize / 2,
                occluderSize / 2, occluderSize / 2, occluderSize / 2
        );
        OBB occluderOBB = new OBB(occluderAabb, new Quaterniond(), occluderCenter);

        // Light source
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluderOBB),
                lightPosition
        );

        System.out.println("\n=== Off-Axis Occlusion Test ===");
        System.out.println("Occluder offset: " + occluderCenter);
        System.out.println("Shadows computed: " + shadows.size());

        for (FaceShadow shadow : shadows) {
            System.out.println("Shadow on plane " + shadow.plane().normal() +
                    " with " + shadow.polygon().size() + " vertices");
        }

        assertFalse(shadows.isEmpty(), "Should produce shadows even with offset occluder");
    }

    @Test
    @DisplayName("Multiple occluders: two cubes casting shadows")
    void testMultipleOccluders() {
        // Target cube
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        OBB targetOBB = new OBB(targetAabb, new Quaterniond(), targetCenter);

        // First occluder
        Vector3dc occluder1Center = new Vector3d(-20, 0, 200);
        double occluder1Size = 30;
        AABBd occluder1Aabb = new AABBd(
                -occluder1Size / 2, -occluder1Size / 2, -occluder1Size / 2,
                occluder1Size / 2, occluder1Size / 2, occluder1Size / 2
        );
        OBB occluder1OBB = new OBB(occluder1Aabb, new Quaterniond(), occluder1Center);

        // Second occluder
        Vector3dc occluder2Center = new Vector3d(20, 0, 250);
        double occluder2Size = 25;
        AABBd occluder2Aabb = new AABBd(
                -occluder2Size / 2, -occluder2Size / 2, -occluder2Size / 2,
                occluder2Size / 2, occluder2Size / 2, occluder2Size / 2
        );
        OBB occluder2OBB = new OBB(occluder2Aabb, new Quaterniond(), occluder2Center);

        // Light source
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluder1OBB, occluder2OBB),
                lightPosition
        );

        System.out.println("\n=== Multiple Occluders Test ===");
        System.out.println("Two occluders at different positions");
        System.out.println("Shadows computed: " + shadows.size());

        for (FaceShadow shadow : shadows) {
            System.out.println("Shadow on plane " + shadow.plane().normal() +
                    " with " + shadow.polygon().size() + " vertices");
        }

        assertFalse(shadows.isEmpty(), "Should produce shadows from multiple occluders");
    }

    @Test
    @DisplayName("No occlusion: occluder behind light source")
    void testNoOcclusionBehindLight() {
        // Target cube
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        OBB targetOBB = new OBB(targetAabb, new Quaterniond(), targetCenter);

        // Occluder BEHIND the light (z > light z)
        Vector3dc occluderCenter = new Vector3d(0, 0, 600);
        double occluderSize = 50;
        AABBd occluderAabb = new AABBd(
                -occluderSize / 2, -occluderSize / 2, -occluderSize / 2,
                occluderSize / 2, occluderSize / 2, occluderSize / 2
        );
        OBB occluderOBB = new OBB(occluderAabb, new Quaterniond(), occluderCenter);

        // Light source
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluderOBB),
                lightPosition
        );

        System.out.println("\n=== No Occlusion (Behind Light) Test ===");
        System.out.println("Occluder behind light");
        System.out.println("Shadows computed: " + shadows.size());

        // Should have no shadows or empty shadows
        assertTrue(shadows.isEmpty() || shadows.stream().allMatch(s -> s.polygon().size() < 3),
                "Should not produce valid shadows when occluder is behind light");
    }

    @Test
    @DisplayName("Rotated target cube: test with non-identity rotation")
    void testRotatedTargetCube() {
        // Target cube rotated 45 degrees around Y axis
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(45));
        OBB targetOBB = new OBB(targetAabb, rotation, targetCenter);

        // Occluder
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        double occluderSize = 50;
        AABBd occluderAabb = new AABBd(
                -occluderSize / 2, -occluderSize / 2, -occluderSize / 2,
                occluderSize / 2, occluderSize / 2, occluderSize / 2
        );
        OBB occluderOBB = new OBB(occluderAabb, new Quaterniond(), occluderCenter);

        // Light source
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        System.out.println("\n=== Rotated Target Cube Detailed Test ===");
        System.out.println("Target: center=" + targetCenter + ", rotation=" + rotation);
        System.out.println("Occluder: center=" + occluderCenter);
        System.out.println("Light: " + lightPosition);

        // Check what facing planes are detected
        List<AAPlane> facingPlanes = PlanetShading.getFacingPlanes(targetOBB, lightPosition);
        System.out.println("Facing planes detected: " + facingPlanes.size());
        for (AAPlane plane : facingPlanes) {
            System.out.println("  Plane: normal=" + plane.normal() + ", pos=" + plane.position());
        }

        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluderOBB),
                lightPosition
        );

        System.out.println("Shadows computed: " + shadows.size());

        for (FaceShadow shadow : shadows) {
            System.out.println("Shadow on plane " + shadow.plane().normal() +
                    " with " + shadow.polygon().size() + " vertices");
        }
        assertFalse(shadows.isEmpty(), "Should produce shadows even with rotated target");
    }

    @Test
    @DisplayName("Edge case: very small occluder")
    void testVerySmallOccluder() {
        // Target cube
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        double targetSize = 100;
        AABBd targetAabb = new AABBd(
                -targetSize / 2, -targetSize / 2, -targetSize / 2,
                targetSize / 2, targetSize / 2, targetSize / 2
        );
        OBB targetOBB = new OBB(targetAabb, new Quaterniond(), targetCenter);

        // Very small occluder
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        double occluderSize = 5;  // Very small
        AABBd occluderAabb = new AABBd(
                -occluderSize / 2, -occluderSize / 2, -occluderSize / 2,
                occluderSize / 2, occluderSize / 2, occluderSize / 2
        );
        OBB occluderOBB = new OBB(occluderAabb, new Quaterniond(), occluderCenter);

        // Light source
        Vector3dc lightPosition = new Vector3d(0, 0, 500);

        List<FaceShadow> shadows = ShadowProjection.computeShadows(
                targetOBB,
                List.of(occluderOBB),
                lightPosition
        );

        System.out.println("\n=== Very Small Occluder Test ===");
        System.out.println("Occluder size: " + occluderSize);
        System.out.println("Shadows computed: " + shadows.size());

        for (FaceShadow shadow : shadows) {
            System.out.println("Shadow on plane " + shadow.plane().normal() +
                    " with " + shadow.polygon().size() + " vertices");
            System.out.println("  Shadow vertices:");
            for (int i = 0; i < shadow.polygon().size(); i++) {
                System.out.println("    " + i + ": " + shadow.polygon().get(i));
            }
        }

        // Small occluder should still produce shadows, just small ones
        assertFalse(shadows.isEmpty(), "Even very small occluder should produce shadows");
    }
}
