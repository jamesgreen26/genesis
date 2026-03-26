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
import java.util.function.Function;
import java.util.function.Predicate;

import static shipwrights.genesis.math.Raycast.raycastOBB;

public class SpaceLevel {

    /// returns a pair of the nearest Celestial matching the predicate, if found, and the distance squared to its center
    public @Nullable static Pair<Celestial, Double> nearestCelestialWhere(Registry<Celestial> registry, Vector3dc position, long ticks, float partialTick, Predicate<CelestialType> predicate) {
        return registry.stream()
                .filter(it -> predicate.test(it.type()))
                .map(it -> new Pair<>(it, position.distanceSquared(it.getPosition(ticks, 0f, registry))))
                .min(Comparator.comparingDouble(Pair::component2))
                .filter(pwd -> pwd.getSecond() < Double.MAX_VALUE).orElse(null);
    }

    /// returns a pair of the nearest matching Celestial in the ray, if found, and the distance squared to the hit location
    public @Nullable static Pair<Celestial, Double> celestialRaycast(Registry<Celestial> registry, long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        return raycastBodies(registry, ticks, partialTick, origin, direction, predicate,
                body -> body.getPosition(ticks, partialTick, registry),
                body -> body.getRotation(ticks, partialTick, registry));
    }

    /// Package-private overload for tests: takes an Iterable of candidates and an optional registry (may be null for StaticTransformProvider)
    static @Nullable Pair<Celestial, Double> celestialRaycast(Iterable<Celestial> candidates, long ticks, float partialTick, Registry<Celestial> registry, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        return raycastBodies(candidates, ticks, partialTick, origin, direction, predicate,
                body -> body.getPosition(ticks, partialTick, registry),
                body -> body.getRotation(ticks, partialTick, registry));
    }

    private @Nullable static Pair<Celestial, Double> raycastBodies(Iterable<Celestial> candidates, long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate, Function<Celestial, Vector3dc> posFn, Function<Celestial, Quaterniondc> rotFn) {
        double closestT = Double.POSITIVE_INFINITY;
        Celestial result = null;

        for (Celestial body : candidates) {
            if (!predicate.test(body.type())) continue;
            Vector3dc pos = posFn.apply(body);
            double oR = body.getActualSize() / 2;
            AABB box = new AABB(pos.x()-oR, pos.y()-oR, pos.z()-oR, pos.x()+oR, pos.y()+oR, pos.z()+oR);
            Vec3 center = box.getCenter();
            double t = raycastOBB(
                    origin,
                    direction,
                    VectorConversionsMCKt.toJOML(center),
                    new Matrix3d().rotation(rotFn.apply(body)),
                    new Vector3d(-oR, -oR, -oR),
                    new Vector3d(oR, oR, oR)
            );

            if (t < closestT) {
                closestT = t;
                result = body;
            }
        }

        return result != null ? new Pair<>(result, closestT * closestT) : null;
    }
}
