package shipwrights.genesis.math;

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OcclusionTest {

    // Helper method to create a simple cube OBB at a position
    private OBB createOBB(double x, double y, double z, double size) {
        return OBB.createCube(size, new Quaterniond(), new Vector3d(x, y, z));
    }

    @Test
    void testObjectBetweenReferenceAndTargetOccludes() {
        // Reference at origin, target at (0, 0, 100), candidate at (0, 0, 50)
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, 50, 20);

        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object directly between reference and target should occlude");
    }

    @Test
    void testObjectBehindTargetDoesNotOcclude() {
        // Reference at origin, target at (0, 0, 100), candidate behind at (0, 0, 150)
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, 150, 20);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object behind target should not occlude");
    }

    @Test
    void testObjectBehindReferenceDoesNotOcclude() {
        // Reference at origin, target at (0, 0, 100), candidate behind reference at (0, 0, -50)
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, -50, 20);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object behind reference point should not occlude");
    }

    @Test
    void testObjectOutsideShadowConeDoesNotOcclude() {
        // Reference at origin, target at (0, 0, 100), candidate far to the side at (100, 0, 50)
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(100, 0, 50, 5);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object far outside shadow cone should not occlude");
    }

    @Test
    void testLargeObjectNearConeBoundaryOccludes() {
        // Reference at origin, target at (0, 0, 100) with radius ~8.66
        // Place a large object closer to the axis so it intersects the shadow cone
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(5, 0, 50, 10);

        // This object's bounding sphere should intersect the shadow cone
        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Large object near cone boundary should occlude");
    }

    @Test
    void testCandidateAtReferencePointDoesNotOcclude() {
        // Candidate at the same position as reference point
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, 0, 10);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object at reference point should not occlude (edge case)");
    }

    @Test
    void testTargetAtReferencePointDoesNotOcclude() {
        // Target at the same position as reference point
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 0, 10);
        OBB candidate = createOBB(0, 0, 50, 10);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "When target is at reference point, nothing should occlude (edge case)");
    }

    @Test
    void testCandidateAtSamePositionAsTarget() {
        // Candidate at the exact same position as target
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, 100, 10);

        // proj == targetDist, so should return false
        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Object at same position as target should not occlude (proj >= targetDist)");
    }

    @Test
    void testVerySmallObjectInShadowConeOccludes() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        OBB candidate = createOBB(0, 0, 50, 1); // Very small object

        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Small object directly in shadow cone should still occlude");
    }

    @Test
    void testVeryLargeObjectPartiallyIntersectingConeOccludes() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        // Large object positioned such that its bounding sphere intersects the cone
        // With size 40, bounding sphere radius is ~34.64, centered at (15, 0, 50)
        // this should definitely intersect the cone
        OBB candidate = createOBB(15, 0, 50, 40);

        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Large object with bounding sphere intersecting cone should occlude");
    }

    @Test
    void testObjectExactlyAtConeBoundaryShouldOcclude() {
        // Create a scenario where the candidate is exactly on the cone boundary
        // Using mathematical precision to place it there
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        double targetRadius = target.boundingSphereRadius();
        double targetDist = 100.0;
        double sinTheta = targetRadius / targetDist;
        double cosTheta = Math.sqrt(1.0 - sinTheta * sinTheta);

        // Place candidate at distance 50 along z-axis, perpendicular distance to be on cone
        double candidateDist = 50.0;
        double perpendicularDist = candidateDist * sinTheta;

        OBB candidate = createOBB(perpendicularDist, 0, candidateDist, 1);

        // The test should pass for objects on the boundary
        boolean result = Occlusion.couldOcclude(candidate, target, referencePoint);
        assertTrue(result, "Object on cone boundary should occlude");
    }

    @Test
    void testGetPotentiallyOccludingWithEmptyList() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);
        List<OBB> others = new ArrayList<>();

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        assertTrue(result.isEmpty(), "Empty list should return empty result");
    }

    @Test
    void testGetPotentiallyOccludingWithNoOccluders() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        List<OBB> others = List.of(
                createOBB(0, 0, -50, 10),   // Behind reference
                createOBB(0, 0, 150, 10),   // Behind target
                createOBB(100, 0, 50, 5)    // Outside cone
        );

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        assertTrue(result.isEmpty(), "Should return empty when no objects occlude");
    }

    @Test
    void testGetPotentiallyOccludingWithSomeOccluders() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        OBB occluder1 = createOBB(0, 0, 50, 10);      // Should occlude
        OBB occluder2 = createOBB(0, 0, 75, 10);      // Should occlude
        OBB nonOccluder1 = createOBB(0, 0, -50, 10);  // Behind reference
        OBB nonOccluder2 = createOBB(100, 0, 50, 5);  // Outside cone

        List<OBB> others = List.of(occluder1, nonOccluder1, occluder2, nonOccluder2);

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        assertEquals(2, result.size(), "Should return exactly 2 occluders");
        assertTrue(result.contains(occluder1), "Should include first occluder");
        assertTrue(result.contains(occluder2), "Should include second occluder");
        assertFalse(result.contains(nonOccluder1), "Should not include object behind reference");
        assertFalse(result.contains(nonOccluder2), "Should not include object outside cone");
    }

    @Test
    void testGetPotentiallyOccludingWithAllOccluders() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        List<OBB> others = List.of(
                createOBB(0, 0, 25, 10),
                createOBB(0, 0, 50, 10),
                createOBB(0, 0, 75, 10)
        );

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        assertEquals(3, result.size(), "Should return all occluders");
    }

    @Test
    void testGetPotentiallyOccludingMayIncludeTargetItself() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        // Include the target itself in the list
        List<OBB> others = List.of(
                target,                     // The target itself
                createOBB(0, 0, 50, 10)    // An actual occluder
        );

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        // The target at same position has proj == targetDist, so it should NOT be included
        assertEquals(1, result.size(), "Target at same position should not be included");
    }

    @Test
    void testOcclusionWithNonAxisAlignedGeometry() {
        // Test with objects at various angles to ensure the algorithm works with
        // the bounding sphere approximation
        Vector3d referencePoint = new Vector3d(10, 10, 10);
        OBB target = createOBB(50, 50, 50, 10);
        OBB candidate = createOBB(30, 30, 30, 15);

        boolean result = Occlusion.couldOcclude(candidate, target, referencePoint);
        assertTrue(result, "Object between reference and target at angles should occlude");
    }

    @Test
    void testOcclusionWithDifferentSizedObjects() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 5);  // Small target
        OBB candidate = createOBB(0, 0, 50, 50); // Very large candidate

        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Large candidate should occlude small target when positioned correctly");
    }

    @Test
    void testSmallCandidateSlightlyOffAxisDoesNotOcclude() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 1000, 10);
        // Small candidate far off to the side
        OBB candidate = createOBB(50, 0, 500, 1);

        assertFalse(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Small candidate off-axis should not occlude");
    }

    @Test
    void testPrecisionWithVeryDistantObjects() {
        // Test with objects at large distances to ensure numerical stability
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 10000, 100);
        OBB candidate = createOBB(0, 0, 5000, 200);

        assertTrue(Occlusion.couldOcclude(candidate, target, referencePoint),
                "Should handle very distant objects correctly");
    }

    @Test
    void testMultipleOccludersAtDifferentDepths() {
        Vector3d referencePoint = new Vector3d(0, 0, 0);
        OBB target = createOBB(0, 0, 100, 10);

        // Create occluders at different depths along the ray
        List<OBB> others = List.of(
                createOBB(0, 0, 20, 5),
                createOBB(0, 0, 40, 5),
                createOBB(0, 0, 60, 5),
                createOBB(0, 0, 80, 5)
        );

        List<OBB> result = Occlusion.getPotentiallyOccluding(target, others, referencePoint);

        assertEquals(4, result.size(), "All objects along the ray should be potential occluders");
    }
}