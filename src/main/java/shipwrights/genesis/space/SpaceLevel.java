package shipwrights.genesis.space;

import kotlin.Pair;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.type.CelestialType;

import java.util.Comparator;
import java.util.function.Predicate;

import static shipwrights.genesis.math.Raycast.raycastOBB;

public class SpaceLevel {

    /// returns a pair of the nearest Celestial matching the predicate, if found, and the distance squared to its center
    public @Nullable static Pair<Celestial, Double> nearestCelestialWhere(Vector3dc position, long ticks, float partialTick, Predicate<CelestialType> predicate) {
        return GenesisMod.SPACE_REGISTRY.getWhere(predicate).stream()
                .map(it -> new Pair<>(it, position.distanceSquared(it.getPosition(ticks))))
                .min(Comparator.comparingDouble(Pair::component2))
                .filter(pwd -> pwd.getSecond() < Double.MAX_VALUE).orElse(null);
    }

    /// returns a pair of the nearest matching Celestial in the ray, if found, and the distance squared to the hit location
    public @Nullable static Pair<Celestial, Double> celestialRaycast(long ticks, float partialTick, Vector3d origin, Vector3d direction, Predicate<CelestialType> predicate) {
        double closestT = Double.POSITIVE_INFINITY;
        Celestial result = null;

        for (Celestial body : GenesisMod.SPACE_REGISTRY.getWhere(predicate)) {
            Vector3dc pos = body.getPosition(ticks, partialTick);
            double oR  = body.getActualSize()/2;
            AABB box = new AABB(pos.x()-oR,pos.y()-oR,pos.z()-oR,pos.x()+oR,pos.y()+oR,pos.z()+oR);
            Quaterniondc rotation = body.getRotation(ticks, partialTick);
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
