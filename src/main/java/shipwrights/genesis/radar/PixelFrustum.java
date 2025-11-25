package shipwrights.genesis.radar;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.primitives.AABBdc;

public class PixelFrustum {

    public static class Plane {
        public Vector3d normal;
        public double d;  // plane eq: normal·X + d >= 0 for inside

        public Plane(Vector3d normal, Vector3dc point) {
            this.normal = normal;
            this.d = -normal.dot(point);
        }

        public void update(Vector3d normal, Vector3dc point) {
            this.normal = normal;
            this.d = -normal.dot(point);
        }

        public boolean isOutside(AABBdc aabb) {
            // For AABB-plane test use the positive vertex check
            double px = normal.x() > 0 ? aabb.maxX() : aabb.minX();
            double py = normal.y() > 0 ? aabb.maxY() : aabb.minY();
            double pz = normal.z() > 0 ? aabb.maxZ() : aabb.minZ();
            return normal.x()*px + normal.y()*py + normal.z()*pz + d < 0;
        }
    }

    private final Plane[] planes;

    public PixelFrustum(Plane... planes) {
        this.planes = planes;
    }

    public void update(Plane left, Plane right, Plane top, Plane bottom) {
        planes[0].normal = left.normal;
        planes[0].d = left.d;
        planes[1].normal = right.normal;
        planes[1].d = right.d;
        planes[2].normal = top.normal;
        planes[2].d = top.d;
        planes[3].normal = bottom.normal;
        planes[3].d = bottom.d;
    }

    public boolean intersectsAabb(AABBdc box) {
        for (Plane p : planes)
            if (p.isOutside(box))
                return false; // fully outside this plane
        return true;
    }
}
