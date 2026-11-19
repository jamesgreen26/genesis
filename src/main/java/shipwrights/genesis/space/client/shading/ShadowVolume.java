package shipwrights.genesis.space.client.shading;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.joml.Vector4dc;
import shipwrights.genesis.space.math.OBB;
import shipwrights.genesis.space.math.Occlusion;
import shipwrights.genesis.space.math.PolygonClipping;

import java.util.ArrayList;
import java.util.List;

/// The volume an occluder shades from a point light: a pyramid with its apex at the light and one side plane through
/// each edge of the occluder's silhouette, cut off by a plane across the middle of the occluder, as what's on the
/// light's side of it isn't in its shadow.
///
/// How dark a point on what's shadowed is depends only on how deep inside this volume it is, never on which surface
/// it's on, so the shadow's soft edge matches up across the edges of a cube.
///
/// @param planes    the planes, in the local space of what's shadowed, as {@code (normal, d)} with the unit normal
///                  pointing into the volume: {@code normal · p + d} is how far inside that plane {@code p} is
/// @param edgeWidth how deep inside the volume a point must be to be fully in shadow
public record ShadowVolume(List<Vector4dc> planes, double edgeWidth) {

    /// The most planes a cube's volume has: its silhouette never has more than six edges
    public static final int MAX_PLANES = 7;

    /// How much of the shadow's cross-section radius the soft edge takes up
    private static final double EDGE_WIDTH_FRACTION = 0.25;

    /// The volumes of everything in {@code others} that could shade {@code self} from {@code light}
    public static List<ShadowVolume> compute(OBB self, List<OBB> others, Vector3dc light) {
        List<ShadowVolume> volumes = new ArrayList<>();
        for (OBB occluder : Occlusion.getPotentiallyOccluding(self, others, light)) {
            ShadowVolume volume = cast(self, occluder, light);
            if (volume != null) volumes.add(volume);
        }
        return List.copyOf(volumes);
    }

    /// The volume {@code occluder} shades, whether or not it reaches {@code self}
    ///
    /// @param self what the shadow is drawn on, whose local space the volume is in
    /// @return null if the occluder has no silhouette from the light, as when the light is inside or beside it
    public static @Nullable ShadowVolume cast(OBB self, OBB occluder, Vector3dc light) {
        Vector3d toOccluder = new Vector3d(occluder.center()).sub(light);
        double occluderDistance = toOccluder.length();
        if (occluderDistance == 0) return null;

        // A basis looking from the light at the occluder, with u x v = w
        Vector3d w = toOccluder.div(occluderDistance, new Vector3d());
        Vector3d u = perpendicular(w);
        Vector3d v = w.cross(u, new Vector3d());

        // The occluder's corners as the light sees them
        List<Vector2d> projected = new ArrayList<>();
        for (Vector3dc corner : occluder.getCorners()) {
            Vector3d ray = new Vector3d(corner).sub(light);
            double depth = ray.dot(w);
            if (depth <= 0) return null;
            projected.add(new Vector2d(ray.dot(u) / depth, ray.dot(v) / depth));
        }
        List<Vector2d> silhouette = PolygonClipping.convexHull(projected);
        if (silhouette.size() < 3) return null;

        Quaterniond toLocal = new Quaterniond(self.orientation()).conjugate();
        Vector3d selfFromLight = new Vector3d(self.center()).sub(light);

        List<Vector4dc> planes = new ArrayList<>();
        for (int i = 0; i < silhouette.size(); i++) {
            Vector3d from = rayThrough(silhouette.get(i), u, v, w);
            Vector3d to = rayThrough(silhouette.get((i + 1) % silhouette.size()), u, v, w);

            Vector3d normal = from.cross(to, new Vector3d());
            if (normal.lengthSquared() == 0) continue;
            normal.normalize();
            // The occluder's centre is inside the volume
            if (normal.dot(w) < 0) normal.negate();

            // Measured from a point on the plane beside the occluder rather than from the light, which is much
            // further away and would lose precision
            Vector3d onPlane = from.mul(occluderDistance, new Vector3d());
            double d = normal.dot(selfFromLight.sub(onPlane, new Vector3d()));

            Vector3d localNormal = toLocal.transform(normal, new Vector3d());
            planes.add(new Vector4d(localNormal.x, localNormal.y, localNormal.z, d));
        }
        if (planes.size() < 3) return null;

        // What's on the light's side of the occluder isn't in its shadow
        Vector3d localDirection = toLocal.transform(w, new Vector3d());
        planes.add(new Vector4d(localDirection.x, localDirection.y, localDirection.z,
                w.dot(new Vector3d(self.center()).sub(occluder.center()))));

        // The silhouette's radius, scaled up to the shadow's cross-section where it reaches what's shadowed
        Vector2d centroid = new Vector2d();
        for (Vector2d point : silhouette) centroid.add(point);
        centroid.div(silhouette.size());
        double radius = 0;
        for (Vector2d point : silhouette) radius = Math.max(radius, point.distance(centroid));
        double edgeWidth = EDGE_WIDTH_FRACTION * radius * selfFromLight.dot(w);
        if (!(edgeWidth > 0)) return null;

        return new ShadowVolume(List.copyOf(planes), edgeWidth);
    }

    /// The direction from the light through a point of the silhouette
    private static Vector3d rayThrough(Vector2d point, Vector3d u, Vector3d v, Vector3d w) {
        return new Vector3d(w).fma(point.x, u).fma(point.y, v);
    }

    private static Vector3d perpendicular(Vector3d direction) {
        // Cross with whichever axis the direction is furthest from, so the result is never near zero
        Vector3d axis = Math.abs(direction.x) < 0.9 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
        return direction.cross(axis, new Vector3d()).normalize();
    }

    /// How far inside the volume a point is, in the local space of what's shadowed: negative outside, and at least
    /// {@link #edgeWidth} where the shadow is at full strength.
    public double depth(Vector3dc localPoint) {
        double depth = Double.POSITIVE_INFINITY;
        for (Vector4dc plane : planes) {
            depth = Math.min(depth,
                    plane.x() * localPoint.x() + plane.y() * localPoint.y() + plane.z() * localPoint.z() + plane.w());
        }
        return depth;
    }
}
