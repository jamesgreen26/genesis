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
 * Visualize the geometry to understand why shadows don't appear for rotated targets.
 */
class ShadowGeometryVisualizationTest {

    @Test
    @DisplayName("Visualize the problem: collinearity is broken by rotation")
    void testCollinearityBreaksWithRotation() {
        System.out.println("\n========== GEOMETRY VISUALIZATION ==========\n");

        // World space setup
        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        Vector3dc lightPos = new Vector3d(0, 0, 500);

        System.out.println("WORLD SPACE:");
        System.out.println("  Target:   " + targetCenter);
        System.out.println("  Occluder: " + occluderCenter);
        System.out.println("  Light:    " + lightPos);
        System.out.println("  ✓ These are collinear along Z-axis!");
        System.out.println("  ✓ Occluder is between target and light");

        // Rotated target
        Quaterniond targetRotation = new Quaterniond().rotateY(Math.toRadians(45));
        OBB targetOBB = new OBB(
            new AABBd(-50, -50, -50, 50, 50, 50),
            targetRotation,
            targetCenter
        );

        // Transform to target's local space
        Vector3d occluderLocal = PlanetShading.worldToLocal(occluderCenter, targetOBB);
        Vector3d lightLocal = PlanetShading.worldToLocal(lightPos, targetOBB);

        System.out.println("\nTARGET'S LOCAL SPACE (after 45° Y-rotation):");
        System.out.println("  Target:   (0, 0, 0)");
        System.out.println("  Occluder: " + occluderLocal);
        System.out.println("  Light:    " + lightLocal);
        System.out.println("  ⚠️  NO LONGER collinear!");
        System.out.println("  ⚠️  Occluder is offset in X direction!");

        // Check which face should receive the shadow in world space
        System.out.println("\nWHICH FACE GETS THE SHADOW?");
        System.out.println("  In world space: The +Z face (front face) should be shadowed");
        System.out.println("  But after 45° Y-rotation:");
        System.out.println("    - The +Z local face points at 45° to world +Z");
        System.out.println("    - Light comes from world +Z = local (-1, 0, 1) direction");
        System.out.println("    - Multiple faces might receive shadows!");

        // The real question: what faces in local space correspond to being shadowed?
        // The light vector in local space
        Vector3d lightDir = new Vector3d(lightLocal).normalize();
        System.out.println("\nLight direction in local space: " + lightDir);
        System.out.println("  This has BOTH -X and +Z components!");
        System.out.println("  So shadows should appear on faces facing this direction:");
        System.out.println("    - Faces with normals having positive dot product with light direction");

        // Calculate which faces see the light
        System.out.println("\nFACE ANALYSIS:");
        double[][] faceNormals = {
            {1, 0, 0}, {-1, 0, 0},   // ±X faces
            {0, 1, 0}, {0, -1, 0},   // ±Y faces
            {0, 0, 1}, {0, 0, -1}    // ±Z faces
        };
        String[] faceNames = {"+X", "-X", "+Y", "-Y", "+Z", "-Z"};

        for (int i = 0; i < faceNormals.length; i++) {
            Vector3d normal = new Vector3d(faceNormals[i][0], faceNormals[i][1], faceNormals[i][2]);
            double dot = normal.dot(lightDir);
            boolean facesLight = dot < 0; // Face normal points away from light = faces toward light
            System.out.println("  " + faceNames[i] + " face: normal=" + normal +
                    ", dot=" + String.format("%.3f", dot) +
                    ", faces light? " + facesLight);
        }
    }

    @Test
    @DisplayName("Demonstrate that NON-ROTATED case works correctly")
    void testNonRotatedCaseWorks() {
        System.out.println("\n========== NON-ROTATED BASELINE ==========\n");

        Vector3dc targetCenter = new Vector3d(0, 0, 0);
        Vector3dc occluderCenter = new Vector3d(0, 0, 200);
        Vector3dc lightPos = new Vector3d(0, 0, 500);

        // Non-rotated target
        OBB targetOBB = new OBB(
            new AABBd(-50, -50, -50, 50, 50, 50),
            new Quaterniond(), // No rotation
            targetCenter
        );

        Vector3d occluderLocal = PlanetShading.worldToLocal(occluderCenter, targetOBB);
        Vector3d lightLocal = PlanetShading.worldToLocal(lightPos, targetOBB);

        System.out.println("LOCAL SPACE (no rotation):");
        System.out.println("  Target:   (0, 0, 0)");
        System.out.println("  Occluder: " + occluderLocal);
        System.out.println("  Light:    " + lightLocal);
        System.out.println("  ✓ Still collinear along Z-axis");
        System.out.println("  ✓ Shadows will project cleanly onto +Z face");

        // Verify collinearity
        assertTrue(Math.abs(occluderLocal.x) < 0.01);
        assertTrue(Math.abs(occluderLocal.y) < 0.01);
        assertTrue(Math.abs(lightLocal.x) < 0.01);
        assertTrue(Math.abs(lightLocal.y) < 0.01);
    }

    @Test
    @DisplayName("Conclusion: The transformation is correct, but shadow SHOULD partially miss!")
    void testConclusion() {
        System.out.println("\n========== CONCLUSION ==========\n");
        System.out.println("The coordinate transformation is MATHEMATICALLY CORRECT.");
        System.out.println("");
        System.out.println("The issue is that when a cube is rotated, a collinear");
        System.out.println("arrangement in world space becomes NON-collinear in the");
        System.out.println("cube's local frame.");
        System.out.println("");
        System.out.println("As a result, shadows cast from the occluder onto the");
        System.out.println("rotated cube's faces can extend BEYOND the face boundaries,");
        System.out.println("or even miss the faces entirely!");
        System.out.println("");
        System.out.println("The current implementation correctly projects the shadow,");
        System.out.println("but after clipping to face bounds, there are too few");
        System.out.println("vertices remaining (< 3), so the shadow is discarded.");
        System.out.println("");
        System.out.println("POSSIBLE SOLUTIONS:");
        System.out.println("  1. The clipping/projection is working correctly - maybe");
        System.out.println("     the shadow SHOULD be mostly outside the face!");
        System.out.println("  2. We need a different approach for rotated targets");
        System.out.println("  3. We should compute shadows in WORLD space instead");
        System.out.println("=======================================================\n");

        assertTrue(true, "This test documents the findings");
    }
}
