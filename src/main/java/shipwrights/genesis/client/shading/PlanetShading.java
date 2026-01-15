package shipwrights.genesis.client.shading;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.slf4j.Logger;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanetShading {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Transform world points into the local space of an OBB - manual implementation */
    public static Vector3d worldToLocal(Vector3dc worldPoint, OBB obb) {
        // Step 1: Translate to move OBB center to origin
        Vector3d translated = new Vector3d(worldPoint).sub(obb.center());

        // Step 2: Rotate by inverse of OBB orientation using MATRIX instead of quaternion
        // Create a rotation matrix from the inverse orientation
        Quaterniond inverseOrientation = new Quaterniond(obb.orientation()).invert();
        Matrix3d rotMatrix = new Matrix3d().set(inverseOrientation);

        // Apply the rotation matrix
        Vector3d result = new Vector3d();
        rotMatrix.transform(translated, result);

        return result;
    }

    /** Transform world points into the local space of an OBB */
    public static Matrix4dc obbToLocal(OBB obb) {
        // DEPRECATED: Use worldToLocal instead for clarity
        // Keeping this for now but it may have wrong order
        return new Matrix4d()
                .translate(new Vector3d(obb.center()).negate())
                .rotate(new Quaterniond(obb.orientation()).conjugate());
    }

    /** Get OBB1 planes facing the reference point in local space */
    public static List<AAPlane> getFacingPlanes(OBB obb, Vector3dc referencePoint) {
        List<AAPlane> planes = new ArrayList<>();
        // Transform reference point to local space using manual transformation
        Vector3d refLocal = worldToLocal(referencePoint, obb);

        AABBdc aabb = obb.localAabb();

        // Loop over the three axes: 0=x, 1=y, 2=z
        for (int axis = 0; axis < 3; axis++) {
            // Check min face (normal points in negative direction)
            {
                double pos = aabb.getMin(axis);
                Vector3d normal = new Vector3d();
                normal.setComponent(axis, -1); // Min face has negative normal

                // Face center in local space
                Vector3d faceCenter = new Vector3d(0, 0, 0);
                faceCenter.setComponent(axis, pos);

                // Vector from face to reference point
                Vector3d toRef = new Vector3d(refLocal).sub(faceCenter);

                // Check if face normal points toward reference (full dot product)
                double dot = normal.dot(toRef);
                if (dot > 0) { // Normal points toward reference
                    planes.add(new AAPlane(new Vector3i(
                        (int)normal.x, (int)normal.y, (int)normal.z), pos));
                }
            }

            // Check max face (normal points in positive direction)
            {
                double pos = aabb.getMax(axis);
                Vector3d normal = new Vector3d();
                normal.setComponent(axis, 1); // Max face has positive normal

                // Face center in local space
                Vector3d faceCenter = new Vector3d(0, 0, 0);
                faceCenter.setComponent(axis, pos);

                // Vector from face to reference point
                Vector3d toRef = new Vector3d(refLocal).sub(faceCenter);

                // Check if face normal points toward reference (full dot product)
                double dot = normal.dot(toRef);
                if (dot > 0) { // Normal points toward reference
                    planes.add(new AAPlane(new Vector3i(
                        (int)normal.x, (int)normal.y, (int)normal.z), pos));
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

    /** Transform a local plane to world space */
    private static class WorldPlane {
        final Vector3d normal;
        final Vector3d point; // A point on the plane

        WorldPlane(Vector3d normal, Vector3d point) {
            this.normal = normal;
            this.point = point;
        }
    }

    private static WorldPlane planeToWorld(AAPlane localPlane, OBB obb) {
        // Transform the plane's normal from local to world space
        Vector3d localNormal = new Vector3d(localPlane.normal().x(), localPlane.normal().y(), localPlane.normal().z());
        Vector3d worldNormal = new Vector3d(localNormal).rotate(obb.orientation()).normalize();

        // Get a point on the plane in local space
        // For an axis-aligned plane, the point is simply at (position, 0, 0), (0, position, 0), or (0, 0, position)
        Vector3d localPoint = new Vector3d();
        if (localPlane.normal().x() != 0) {
            localPoint.x = localPlane.position();
        } else if (localPlane.normal().y() != 0) {
            localPoint.y = localPlane.position();
        } else {
            localPoint.z = localPlane.position();
        }

        // Transform to world space
        Vector3d worldPoint = new Vector3d(localPoint).rotate(obb.orientation()).add(obb.center());

        return new WorldPlane(worldNormal, worldPoint);
    }

    /** Intersect ray with plane in WORLD space, return world intersection point */
    private static Vector3d intersectRayWithPlaneWorld(Vector3dc rayOrigin, Vector3dc rayTarget, WorldPlane plane) {
        Vector3d origin = new Vector3d(rayOrigin);
        Vector3d target = new Vector3d(rayTarget);
        Vector3d dir = new Vector3d(target).sub(origin).normalize();

        double denom = plane.normal.dot(dir);
        if (Math.abs(denom) < 1e-8) return null; // parallel

        // Plane equation: normal · (P - point) = 0
        // Ray: P = origin + t * dir
        // Solve: normal · (origin + t * dir - point) = 0
        // t = normal · (point - origin) / (normal · dir)
        Vector3d toPlane = new Vector3d(plane.point).sub(origin);
        double t = toPlane.dot(plane.normal) / denom;

        if (t < 0) return null; // intersection behind origin

        return origin.add(new Vector3d(dir).mul(t));
    }

    /** Main function: project OBB2 onto OBB1 planes - WORLD SPACE VERSION */
    public static Map<AAPlane, List<Vector2dc>> projectOBB2OntoOBB1Planes(OBB obb1, OBB obb2, Vector3dc referencePoint) {
        Map<AAPlane, List<Vector2dc>> result = new HashMap<>();
        Vector3dc[] obb2Corners = obb2.getCorners();

        // Get facing planes in local space (this part stays the same)
        List<AAPlane> facingPlanes = getFacingPlanes(obb1, referencePoint);

        // For each facing plane, do projection in WORLD space
        for (AAPlane localPlane : facingPlanes) {
            // Transform plane to world space
            WorldPlane worldPlane = planeToWorld(localPlane, obb1);

            List<Vector2dc> hits = new ArrayList<>();

            // For each occluder corner (already in world space)
            for (Vector3dc cornerWorld : obb2Corners) {
                // Intersect ray from light through corner with plane in WORLD space
                Vector3d hitWorld = intersectRayWithPlaneWorld(referencePoint, cornerWorld, worldPlane);

                if (hitWorld != null) {
                    // Transform the 3D world intersection back to local space
                    Vector3d hitLocal = worldToLocal(hitWorld, obb1);

                    // Convert the 3D local point to 2D plane coordinates
                    // We don't need to check bounds here - the polygon clipping step
                    // in ShadowProjection.projectAndClip() will handle trimming to face bounds
                    Vector2dc hit2d = localPoint3DToPlane2D(hitLocal, localPlane);
                    if (hit2d != null) {
                        hits.add(hit2d);
                    }
                }
            }

            result.put(localPlane, hits);
        }

        return result;
    }

    /** Convert a 3D point in local space to 2D coordinates on a plane */
    private static Vector2dc localPoint3DToPlane2D(Vector3d point3D, AAPlane plane) {
        // For an axis-aligned plane, just extract the two non-plane-axis coordinates
        if (plane.normal().x() != 0) {
            // X-normal plane: 2D coords are (y, z)
            return new Vector2d(point3D.y, point3D.z);
        } else if (plane.normal().y() != 0) {
            // Y-normal plane: 2D coords are (x, z)
            return new Vector2d(point3D.x, point3D.z);
        } else {
            // Z-normal plane: 2D coords are (x, y)
            return new Vector2d(point3D.x, point3D.y);
        }
    }

}
