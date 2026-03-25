package shipwrights.genesis.space;

import kotlin.Pair;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.function.Predicate;

import static shipwrights.genesis.math.Raycast.raycastOBB;

public class SpaceLevel {

    /// returns a pair of the nearest Celestial matching the predicate, if found, and the distance squared to its center
    public @Nullable static Pair<Celestial, Double> nearestCelestialWhere(@Nullable Registry<Celestial> registry, Vector3dc position, long ticks, float partialTick, Predicate<CelestialType> predicate) {
        if (registry == null) return null;
        return registry.stream()
                .filter(it -> predicate.test(it.type()))
                .map(it -> new Pair<>(it, position.distanceSquared(it.getPosition(ticks, 0f, registry))))
                .min(Comparator.comparingDouble(Pair::component2))
                .filter(pwd -> pwd.getSecond() < Double.MAX_VALUE).orElse(null);
    }

    /// returns a pair of the nearest matching Celestial in the ray, if found, and the distance squared to the hit location
    public @Nullable static Pair<Celestial, Double> celestialRaycast(Registry<Celestial> registry, long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        return celestialRaycast(registry, registry, ticks, partialTick, origin, direction, predicate);
    }

    /// Package-private overload for tests: takes an Iterable of candidates (no registry needed for StaticTransformProvider)
    static @Nullable Pair<Celestial, Double> celestialRaycast(Iterable<Celestial> candidates, long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        return celestialRaycast(candidates, null, ticks, partialTick, origin, direction, predicate);
    }

    private @Nullable static Pair<Celestial, Double> celestialRaycast(Iterable<Celestial> candidates, @Nullable Registry<Celestial> registry, long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        double closestT = Double.POSITIVE_INFINITY;
        Celestial result = null;

        for (Celestial body : candidates) {
            if (!predicate.test(body.type())) continue;
            Vector3dc pos = body.getPosition(ticks, partialTick, registry);
            double oR  = body.getActualSize()/2;
            AABB box = new AABB(pos.x()-oR,pos.y()-oR,pos.z()-oR,pos.x()+oR,pos.y()+oR,pos.z()+oR);
            Quaterniondc rotation = body.getRotation(ticks, partialTick, registry);
            Vec3 center = box.getCenter();
            double t = raycastOBB(
                    origin,
                    direction,
                    VectorConversionsMCKt.toJOML(center),
                    new Matrix3d().rotation(rotation),
                    new Vector3d(-oR, -oR, -oR),
                    new Vector3d(oR, oR, oR)
            );

            if (t < closestT) {
                closestT = t;
                result = body;
            }
        }

        if(result!=null) {
            return new Pair<>(result, closestT * closestT);
        }
        return null;
    }
}
