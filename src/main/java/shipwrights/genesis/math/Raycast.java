package shipwrights.genesis.math;

import org.joml.Matrix3d;
import org.joml.Vector3d;

public class Raycast {

    /**
     * Performs ray intersection with an axis-aligned bounding box using the slab method.
     *
     * @param origin the starting point of the ray in world space
     * @param direction the direction vector of the ray (does not need to be normalized)
     * @param min the minimum corner of the AABB
     * @param max the maximum corner of the AABB
     * @return the distance t along the ray where intersection occurs (origin + t * direction),
     *         or {@link Double#POSITIVE_INFINITY} if there is no intersection
     */
    public static double raycastAABB(Vector3d origin, Vector3d direction, Vector3d min, Vector3d max)
    {
        double tMin = 0.0;
        double tMax = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            double originA = origin.get(i);
            double DirectionA = direction.get(i);
            double minA = min.get(i);
            double maxA = max.get(i);

            if (Math.abs(DirectionA) < 1e-9) {
                if (originA < minA || originA > maxA)
                    return Double.POSITIVE_INFINITY;
            } else {
                double invD = 1.0 / DirectionA;
                double t1 = (minA - originA) * invD;
                double t2 = (maxA - originA) * invD;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax)
                    return Double.POSITIVE_INFINITY;
            }
        }

        return tMin;
    }

    /**
     * Performs ray intersection with an oriented bounding box by transforming the ray
     * into the OBB's local coordinate space and performing an AABB test.
     *
     * @param origin the starting point of the ray in world space
     * @param direction the direction vector of the ray in world space (does not need to be normalized)
     * @param center the center position of the OBB in world space
     * @param rotation the rotation matrix of the OBB (must be orthonormal)
     * @param localMin the minimum corner of the box in local space, relative to the center
     *                 (typically negative half-extents)
     * @param localMax the maximum corner of the box in local space, relative to the center
     *                 (typically positive half-extents)
     * @return the distance t along the ray where intersection occurs (origin + t * direction),
     *         or {@link Double#POSITIVE_INFINITY} if there is no intersection
     */
    public static double raycastOBB(Vector3d origin, Vector3d direction, Vector3d center, Matrix3d rotation, Vector3d localMin, Vector3d localMax)
    {
        // Inverse rotation = transpose (rotation is orthonormal)
        Matrix3d invRot = new Matrix3d(rotation).transpose();

        // Transform ray into box-local space
        Vector3d localOrigin = invRot.transform(new Vector3d(origin).sub(center));

        Vector3d localDir = invRot.transform(new Vector3d(direction));

        return raycastAABB(localOrigin, localDir, localMin, localMax);
    }
}
