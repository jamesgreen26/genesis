package shipwrights.genesis.math;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;


public record OBB(AABBdc localAabb, Quaterniondc orientation, Vector3dc center) {

    /**
     * Creates a cube-shaped OBB with the specified side length, orientation, and center.
     *
     * @param sideLength the length of each side of the cube
     * @param orientation the orientation of the cube as a quaternion
     * @param center the center position of the cube in world space
     * @return a new OBB representing the cube
     */
    public static OBB createCube(double sideLength, Quaterniondc orientation, Vector3dc center) {
        double halfSize = sideLength / 2.0;
        org.joml.primitives.AABBd localAabb = new org.joml.primitives.AABBd(
                -halfSize, -halfSize, -halfSize,
                halfSize, halfSize, halfSize
        );
        return new OBB(localAabb, orientation, center);
    }

    /**
     * Creates an OBB from an axis-aligned bounding box.
     * The resulting OBB will have identity rotation (no rotation) and will be positioned
     * at the center of the input AABB.
     *
     * @param aabb the axis-aligned bounding box to convert
     * @return a new OBB representing the same volume as the AABB
     */
    public static OBB fromAABB(AABBdc aabb) {
        // Calculate the center of the AABB
        double centerX = (aabb.minX() + aabb.maxX()) / 2.0;
        double centerY = (aabb.minY() + aabb.maxY()) / 2.0;
        double centerZ = (aabb.minZ() + aabb.maxZ()) / 2.0;
        Vector3d center = new Vector3d(centerX, centerY, centerZ);

        // Get the half-extents from the AABB
        Vector3d halfExtents = aabb.extent(new Vector3d());

        // Create a local AABB centered at the origin with the same dimensions
        org.joml.primitives.AABBd localAabb = new org.joml.primitives.AABBd(
                -halfExtents.x, -halfExtents.y, -halfExtents.z,
                halfExtents.x, halfExtents.y, halfExtents.z
        );

        // Use identity rotation (no rotation)
        Quaterniondc identityRotation = new org.joml.Quaterniond();

        return new OBB(localAabb, identityRotation, center);
    }

    /**
     * Calculates the minimum Euclidean distance between this OBB and another OBB.
     * Returns 0 if the OBBs are overlapping.
     */
    public double distanceTo(OBB other) {
        // Find the closest point on this OBB to the other OBB's center
        Vector3d closestOnThis = closestPointTo(other.center);

        // Find the closest point on the other OBB to that point
        Vector3d closestOnOther = other.closestPointTo(closestOnThis);

        // Refine: find closest point on this OBB to the point we just found
        closestOnThis = closestPointTo(closestOnOther);

        // Calculate Euclidean distance
        double distance = closestOnThis.distance(closestOnOther);

        // Check if boxes are actually overlapping using SAT
        if (overlapsWith(other)) {
            return 0.0;
        }

        return distance;
    }

    /**
     * Finds the closest point on this OBB to the given point in world space.
     */
    public Vector3d closestPointTo(Vector3dc point) {
        // Transform point to OBB's local space
        Vector3d localPoint = new Vector3d(point).sub(this.center);

        // Get inverse rotation (conjugate of quaternion)
        Quaterniondc invRotation = new org.joml.Quaterniond(this.orientation).conjugate();
        localPoint.rotate(invRotation);

        // Clamp to AABB bounds
        Vector3d halfExtents = this.localAabb.extent(new Vector3d());
        localPoint.x = Math.max(-halfExtents.x, Math.min(halfExtents.x, localPoint.x));
        localPoint.y = Math.max(-halfExtents.y, Math.min(halfExtents.y, localPoint.y));
        localPoint.z = Math.max(-halfExtents.z, Math.min(halfExtents.z, localPoint.z));

        // Transform back to world space
        localPoint.rotate(this.orientation);
        localPoint.add(this.center);

        return localPoint;
    }

    /**
     * Checks if this OBB overlaps with another OBB using the Separating Axis Theorem.
     */
    public boolean overlapsWith(OBB other) {
        Vector3d[] axesA = new Vector3d[3];
        Vector3d[] axesB = new Vector3d[3];

        getAxes(this.orientation, axesA);
        getAxes(other.orientation, axesB);

        Vector3d centerDelta = new Vector3d(other.center).sub(this.center);

        // Test face normals of both boxes
        for (int i = 0; i < 3; i++) {
            if (axisDistance(axesA[i], centerDelta, this.localAabb, other.localAabb, axesA, axesB) > 0) {
                return false;
            }
            if (axisDistance(axesB[i], centerDelta, this.localAabb, other.localAabb, axesA, axesB) > 0) {
                return false;
            }
        }

        // Test cross products of edges
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Vector3d axis = new Vector3d(axesA[i]).cross(axesB[j]);
                if (axis.lengthSquared() < 1e-10) continue;
                axis.normalize();

                if (axisDistance(axis, centerDelta, this.localAabb, other.localAabb, axesA, axesB) > 0) {
                    return false;
                }
            }
        }

        return true; // No separating axis found, boxes overlap
    }

    static void getAxes(Quaterniondc q, Vector3d[] axes) {
        axes[0] = new Vector3d(1, 0, 0).rotate(q);
        axes[1] = new Vector3d(0, 1, 0).rotate(q);
        axes[2] = new Vector3d(0, 0, 1).rotate(q);
    }

    static double axisDistance(
            Vector3dc axis,
            Vector3dc centerDelta,
            AABBdc aabbA,
            AABBdc aabbB,
            Vector3d[] axesA,
            Vector3d[] axesB
    ) {
        double centerProj = Math.abs(centerDelta.dot(axis));

        double rA = projectRadius(aabbA, axis, axesA);
        double rB = projectRadius(aabbB, axis, axesB);

        return centerProj - (rA + rB);
    }

    static double projectRadius(AABBdc aabb, Vector3dc axis, Vector3d[] boxAxes) {
        Vector3d halfExtents = aabb.extent(new Vector3d());

        return Math.abs(axis.dot(boxAxes[0])) * halfExtents.x +
                Math.abs(axis.dot(boxAxes[1])) * halfExtents.y +
                Math.abs(axis.dot(boxAxes[2])) * halfExtents.z;
    }

}
