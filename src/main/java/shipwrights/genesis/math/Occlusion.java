package shipwrights.genesis.math;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;

public class Occlusion {
    /**
     * Filters a list of oriented bounding boxes (OBBs) to those that could
     * potentially occlude a reference-point light source (e.g., a sun)
     * from reaching a target OBB.
     *
     * <p>
     * This method performs a conservative, approximate geometric test.
     * It may return false positives (objects that do not actually occlude),
     * but must not return false negatives (objects that could occlude).
     * </p>
     *
     * <p>
     * Occlusion is defined with respect to a point light located at
     * {@code referencePoint}. An OBB {@code candidate} is considered a
     * potential occluder of {@code self} if:
     * </p>
     *
     * <ul>
     *   <li>The candidate lies strictly between the reference point and the target
     *       along the reference-to-target direction.</li>
     *   <li>The candidate’s bounding sphere intersects the shadow cone formed by
     *       the reference point and the target’s bounding sphere.</li>
     * </ul>
     *
     * <p>
     * No exact OBB–OBB intersection, ray casting, or SAT tests are performed.
     * All OBBs are approximated using their bounding spheres.
     * </p>
     *
     * <p><b>Expected properties:</b></p>
     * <ul>
     *   <li>Objects behind the target relative to the reference point are excluded.</li>
     *   <li>Objects outside the target’s shadow cone are excluded.</li>
     *   <li>Objects exactly on the cone boundary are included.</li>
     *   <li>The target itself may be included if present in {@code others}
     *       (callers should filter it out if undesired).</li>
     * </ul>
     *
     * <p>
     * This method is intended for broad-phase shadow or visibility culling and
     * is suitable for real-time use.
     * </p>
     *
     * @param self the target OBB receiving light from the reference point
     * @param others a collection of other OBBs to test as potential occluders
     * @param referencePoint the position of the point light source (e.g., sun)
     * @return an immutable list of OBBs that could potentially occlude light
     *         from the reference point to {@code self}
     */
    public static List<OBB> getPotentiallyOccluding(OBB self, List<OBB> others, Vector3dc referencePoint) {
        return others.stream().filter(it -> couldOcclude(it, self, referencePoint)).toList();
    }

    /**
     * Determines whether a candidate OBB could potentially occlude light
     * from a point light source to a target OBB.
     *
     * <p>
     * The test is conservative and approximate. It treats both OBBs as
     * bounding spheres and evaluates whether the candidate intersects the
     * shadow cone defined by the reference point and the target’s silhouette.
     * </p>
     *
     * <p>
     * The following conditions must be satisfied for this method to return {@code true}:
     * </p>
     *
     * <ol>
     *   <li>The candidate lies strictly between the reference point and the target
     *       along the reference-to-target direction.</li>
     *   <li>The candidate’s bounding sphere intersects or touches the shadow cone
     *       defined by the target’s bounding sphere.</li>
     * </ol>
     *
     * <p>
     * This method guarantees no false negatives for occlusion at the bounding-sphere
     * level, but may return false positives.
     * </p>
     *
     * <p><b>Geometric assumptions:</b></p>
     * <ul>
     *   <li>The light source is a point located at {@code referencePoint}.</li>
     *   <li>{@code target.center()} and {@code candidate.center()} are expressed
     *       in the same world coordinate space.</li>
     *   <li>{@code boundingSphereRadius()} returns a non-negative radius.</li>
     * </ul>
     *
     * <p><b>Edge cases:</b></p>
     * <ul>
     *   <li>If the candidate center coincides with the reference point,
     *       the method returns {@code false}.</li>
     *   <li>If the target center coincides with the reference point,
     *       the method returns {@code false}.</li>
     *   <li>Objects exactly on the shadow cone boundary are considered occluders.</li>
     * </ul>
     *
     * @param candidate the OBB being tested as a potential occluder
     * @param target the OBB receiving light from the reference point
     * @param referencePoint the position of the point light source
     * @return {@code true} if the candidate could potentially occlude the target,
     *         {@code false} otherwise
     */
    public static boolean couldOcclude(
            OBB candidate,
            OBB target,
            Vector3dc referencePoint
    ) {
        Vector3d sunToTarget = new Vector3d(target.center()).sub(referencePoint);
        double targetDist = sunToTarget.length();
        if (targetDist == 0.0) return false;
        sunToTarget.div(targetDist);

        Vector3d sunToCandidate = new Vector3d(candidate.center()).sub(referencePoint);
        double candidateDist = sunToCandidate.length();
        if (candidateDist == 0.0) return false;

        // Step 1: half-space
        double proj = sunToCandidate.dot(sunToTarget);
        if (proj <= 0.0 || proj >= targetDist) return false;

        // Step 2: cone test
        double targetRadius = target.boundingSphereRadius();
        double sinTheta = Math.min(1.0, targetRadius / targetDist);
        double cosTheta = Math.sqrt(1.0 - sinTheta * sinTheta);

        double candidateRadius = candidate.boundingSphereRadius();

        double dot = proj / candidateDist;

        return dot >= cosTheta - (candidateRadius / candidateDist) * sinTheta;
    }
}
