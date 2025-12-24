package shipwrights.genesis.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.planets.PlanetData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Utility class for planet-related operations in Genesis
 */
public class PlanetUtil {

    /**
     * Get the space dimension (Great Unknown) where planets orbit
     */
    public static ResourceKey<Level> getSpaceDimension() {
        return ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            GenesisMod.SPACE_DIM
        );
    }

    /**
     * Check if a dimension is the space dimension
     */
    public static boolean isSpaceDimension(ResourceKey<Level> dimension) {
        return dimension.location().equals(GenesisMod.SPACE_DIM);
    }

    /**
     * Get planet data for a specific dimension
     */
    @Nullable
    public static PlanetData getPlanetByDimension(ResourceKey<Level> dimension) {
        return GenesisMod.planets.get(dimension.location());
    }

    /**
     * Get the nearest planet to a position in space
     */
    public static Optional<PlanetWithDistance> getNearestPlanet(Vec3 position, long ticks) {
        return GenesisMod.planets.values().stream()
            .map(planet -> {
                Vector3d planetPos = planet.getCurrentPos(ticks);
                double distance = Math.sqrt(
                    Math.pow(position.x - planetPos.x, 2) +
                    Math.pow(position.y - planetPos.y, 2) +
                    Math.pow(position.z - planetPos.z, 2)
                );
                return new PlanetWithDistance(planet, distance);
            })
            .min(Comparator.comparingDouble(pwd -> pwd.distance))
            .filter(pwd -> pwd.distance < Double.MAX_VALUE);
    }


    /**
     * Get the rotation quaternion for a planet
     */
    public static Quaterniond getPlanetRotation(PlanetData planet) {
        // Convert the planet's rotation vector to a quaternion
        // For now, using identity - you may want to implement proper rotation from the Vector3d
        return new Quaterniond(); // TODO: Implement proper rotation conversion from planet.rot
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

    public static Optional<PlanetWithDistance> celestialRaycast(long ticks,Vector3d origin, Vector3d direction)
    {
        double closestT = Double.POSITIVE_INFINITY;
        PlanetData planetData = null;

        for (PlanetData data : GenesisMod.planets.values()) {
            Vector3d pos = data.getCurrentPos(ticks);
            double oR  = data.getActualSize()/2 + ((data.getActualSize()/2)/2);
            AABB box = new AABB(pos.x-oR,pos.y-oR,pos.z-oR,pos.x+oR,pos.y+oR,pos.z+oR);
            Matrix3d rotation = data.getRotationMatrix();
            Vec3 center = box.getCenter();
            double t = raycastAABB(
                    origin,
                    direction,
                    new Vector3d(box.minX,box.minY,box.minZ),
                    new Vector3d(box.maxX,box.maxY,box.maxZ)
            );

            if (t < closestT) {
                closestT = t;
                planetData = data;
            }
        }

        if(planetData!=null)
        {
            return Optional.of(new PlanetWithDistance(planetData,closestT));
        }

        return Optional.empty();
    }

    /**
     * Helper record to store planet with calculated distance
     */
    public record PlanetWithDistance(PlanetData planet, double distance) {}
}
