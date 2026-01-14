package shipwrights.genesis.client.shading;

import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetShading {

    /** Transform world points into the local space of an OBB */
    public static Matrix4dc obbToLocal(OBB obb) {
        return new Matrix4d()
                .translate(new Vector3d(obb.center()).negate())
                .rotate(new Quaterniond(obb.orientation()).conjugate());
    }

    /** Get OBB1 planes facing the reference point in local space */
    public static List<AAPlane> getFacingPlanes(OBB obb, Vector3dc referencePoint) {
        List<AAPlane> planes = new ArrayList<>();
        // Transform reference point to local space
        Vector3d refLocal = new Vector3d(referencePoint);
        Matrix4dc toLocal = obbToLocal(obb);
        refLocal.mulPosition(toLocal);

        AABBdc aabb = obb.localAabb();

        // Loop over the three axes: 0=x, 1=y, 2=z
        for (int axis = 0; axis < 3; axis++) {
            // Check min and max face along this axis
            for (double pos : new double[]{aabb.getMin(axis), aabb.getMax(axis)}) {
                Vector3i normal = new Vector3i();
                normal.setComponent(axis, 1);

                double toRef = refLocal.get(axis) - pos;
                if (toRef < 0) normal.mul(-1); // flip if facing away
                // Only add plane if normal points toward reference
                if (refLocal.get(axis) * normal.get(axis) > pos * normal.get(axis)) {
                    planes.add(new AAPlane(normal, pos));
                }
            }
        }

        return planes;
    }

    /** Project a ray from rayOrigin->rayTarget onto a plane in OBB1 local space, return Vector2dc (plane-space coordinates) */
    @SuppressWarnings("SuspiciousNameCombination")
    public static @Nullable Vector2dc intersectRayWithPlane(Vector3dc rayOrigin, Vector3dc rayTarget, AAPlane plane) {
        Vector3d origin = new Vector3d(rayOrigin);
        Vector3d target = new Vector3d(rayTarget);
        Vector3d dir = target.sub(origin, new Vector3d()).normalize();

        // Plane: normal * x = position
        Vector3d normal = new Vector3d(plane.normal().x(), plane.normal().y(), plane.normal().z());
        double denom = normal.dot(dir);
        if (Math.abs(denom) < 1e-8) return null; // parallel, no intersection

        double t = (plane.position() - normal.dot(origin)) / denom;
        if (t < 0) return null; // intersection behind origin

        Vector3d hit = origin.add(dir.mul(t, new Vector3d()));

        if (plane.normal().x() != 0) return new Vector2d(hit.y, hit.z);
        if (plane.normal().y() != 0) return new Vector2d(hit.x, hit.z);
        return new Vector2d(hit.x, hit.y); // z-normal
    }

    /** Main function: project OBB2 onto OBB1 planes */
    public static Map<AAPlane, List<Vector2dc>> projectOBB2OntoOBB1Planes(OBB obb1, OBB obb2, Vector3dc referencePoint) {
        Map<AAPlane, List<Vector2dc>> result = new HashMap<>();
        Matrix4dc toLocal = obbToLocal(obb1);
        Vector3dc[] obb2Corners = obb2.getCorners();
        Vector3d refLocal = new Vector3d(referencePoint).mulPosition(toLocal);

        // Transform OBB2 corners into OBB1 local space
        Vector3d[] obb2LocalCorners = new Vector3d[obb2Corners.length];
        for (int i = 0; i < obb2Corners.length; i++) {
            obb2LocalCorners[i] = new Vector3d(obb2Corners[i]).mulPosition(toLocal);
        }

        List<AAPlane> planes = getFacingPlanes(obb1, referencePoint);
        for (AAPlane plane : planes) {
            List<Vector2dc> hits = new ArrayList<>();
            for (Vector3d corner : obb2LocalCorners) {
                Vector2dc hit2d = intersectRayWithPlane(corner, refLocal, plane);
                if (hit2d != null) hits.add(hit2d);
            }
            result.put(plane, hits);
        }
        return result;
    }

}
