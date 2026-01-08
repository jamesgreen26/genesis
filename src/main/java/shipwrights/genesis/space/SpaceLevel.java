package shipwrights.genesis.space;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.GenesisMod;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SpaceLevel {

    public static Optional<OrbitingBody.WithDistanceSq> getNearestOrbitingBody(Vector3dc position, long ticks) {
        return GenesisMod.SPACE_REGISTRY.getAllOrbitingBodies().stream()
                .map(it -> it.withDistanceSq(position.distanceSquared(it.getCurrentPos(ticks))))
                .min(Comparator.comparingDouble(OrbitingBody.WithDistanceSq::getDistanceSquared))
                .filter(pwd -> pwd.getDistanceSquared() < Double.MAX_VALUE);
    }

    public static Optional<Star.WithDistanceSq> getNearestStar(Vector3dc position, long ticks) {
        return GenesisMod.SPACE_REGISTRY.getAllStars().stream()
                .map(it -> it.withDistanceSq(position.distanceSquared(it.getCurrentPos(ticks))))
                .min(Comparator.comparingDouble(Star.WithDistanceSq::getDistanceSquared))
                .filter(pwd -> pwd.getDistanceSquared() < Double.MAX_VALUE);
    }

    private static double raycastAABB(Vector3d origin, Vector3d direction, Vector3d min, Vector3d max)
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

    private static double raycastOBB(Vector3d origin, Vector3d direction, Vector3d center, Matrix3d rotation, Vector3d localMin, Vector3d localMax)
    {
        // Inverse rotation = transpose (rotation is orthonormal)
        Matrix3d invRot = new Matrix3d(rotation).transpose();

        // Transform ray into box-local space
        Vector3d localOrigin = new Vector3d(origin).sub(center).mul(invRot);

        Vector3d localDir = new Vector3d(direction).mul(invRot);

        return raycastAABB(localOrigin, localDir, localMin, localMax);
    }

    public static Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> celestialRaycast(long ticks, Vector3d origin, Vector3d direction)
    {
        double closestT = Double.POSITIVE_INFINITY;
        Orbitable.Celestial result = null;

        List<Orbitable.Celestial> celestials = new java.util.ArrayList<>();
        celestials.addAll(GenesisMod.SPACE_REGISTRY.getAllOrbitingBodies());
        celestials.addAll(GenesisMod.SPACE_REGISTRY.getAllStars());

        for (Orbitable.Celestial body : celestials) {
            Vector3dc pos = body.getCurrentPos(ticks);
            double oR  = body.getActualSize()/2 + ((body.getActualSize()/2)/2);
            AABB box = new AABB(pos.x()-oR,pos.y()-oR,pos.z()-oR,pos.x()+oR,pos.y()+oR,pos.z()+oR);
            Quaterniondc rotation = body.getRotation(ticks);
            Vec3 center = box.getCenter();
            double t = raycastAABB(
                    origin,
                    direction,
                    new Vector3d(box.minX,box.minY,box.minZ),
                    new Vector3d(box.maxX,box.maxY,box.maxZ)
            );

            if (t < closestT) {
                closestT = t;
                result = body;
            }
        }

        if(result!=null)
        {
            return Optional.of(new Orbitable.Celestial.WithDistanceSq<>(result, closestT * closestT));
        }

        return Optional.empty();
    }

}
