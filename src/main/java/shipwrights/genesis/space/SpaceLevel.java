package shipwrights.genesis.space;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static shipwrights.genesis.math.Raycast.raycastOBB;

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

    public static Optional<Orbitable.Celestial.WithDistanceSq<Orbitable.Celestial>> celestialRaycast(long ticks, Vector3d origin, Vector3d direction) {
        double closestT = Double.POSITIVE_INFINITY;
        Orbitable.Celestial result = null;

        List<Orbitable.Celestial> celestials = new java.util.ArrayList<>();
        celestials.addAll(GenesisMod.SPACE_REGISTRY.getAllOrbitingBodies());
        celestials.addAll(GenesisMod.SPACE_REGISTRY.getAllStars());

        for (Orbitable.Celestial body : celestials) {
            Vector3dc pos = body.getCurrentPos(ticks);
            double oR  = body.getActualSize()/2;
            AABB box = new AABB(pos.x()-oR,pos.y()-oR,pos.z()-oR,pos.x()+oR,pos.y()+oR,pos.z()+oR);
            Quaterniondc rotation = body.getRotation(ticks);
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
            return Optional.of(new Orbitable.Celestial.WithDistanceSq<>(result, closestT * closestT));
        }

        return Optional.empty();
    }

}
