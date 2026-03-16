package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for teleporting entities across server levels (dimensions),
 * preserving passengers and their relative positions.
 */
public class EntityTeleporter {

    /**
     * Teleports an entity and all of its passengers to the given level and position.
     * Passengers are detached before teleporting and reattached afterward with their
     * positions offset relative to the vehicle's movement.
     * The frame rotation is applied to each entity's look direction so it stays
     * consistent relative to the ship after the coordinate frame changes.
     *
     * @param <T>      the type of the entity being teleported
     * @param entity   the entity to teleport
     * @param newLevel the destination level
     * @param newPos   the destination position
     */
    public static <T extends Entity> void teleportEntityAndPassengers(T entity, ServerLevel newLevel, Vec3 newPos) {
        teleportEntityAndPassengers(entity, newLevel, newPos, new Quaterniond());
    }

    public static <T extends Entity> T teleportEntityAndPassengers(T entity, ServerLevel newLevel, Vec3 newPos, Quaterniondc rotation) {
        Vec3 oldPos = entity.position();

        List<Entity> passengers = detachPassengers(entity);

        T newEntity = teleportEntity(entity, newLevel, newPos, rotation);

        if (newEntity != null) {
            reattachPassengers(passengers, oldPos, newPos, newEntity, newLevel, rotation);
        }

        return newEntity;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> T teleportEntity(T entity, ServerLevel newLevel, Vec3 newPos, Quaterniondc rotation) {
        float[] newAngles = rotateLookAngles(entity.getYRot(), entity.getXRot(), rotation);
        float newYRot = newAngles[0];
        float newXRot = newAngles[1];

        if (entity instanceof ServerPlayer player) {
            teleportPlayer(player, newLevel, newPos, newYRot, newXRot);
            return entity;
        }

        if (entity.level() == newLevel) {
            entity.moveTo(newPos.x, newPos.y, newPos.z, newYRot, newXRot);
            return entity;
        }

        return (T) cloneAndTeleportEntity(entity, newLevel, newPos, newYRot, newXRot);
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel level, Vec3 pos, float yRot, float xRot) {
        player.teleportTo(level, pos.x, pos.y, pos.z, yRot, xRot);
    }

    private static Entity cloneAndTeleportEntity(Entity entity, ServerLevel newLevel, Vec3 newPos, float yRot, float xRot) {
        Entity newEntity = entity.getType().create(newLevel);

        if (!entity.getClass().isInstance(newEntity)) {
            return null;
        }

        newEntity.restoreFrom(entity);
        newEntity.moveTo(newPos.x, newPos.y, newPos.z, yRot, xRot);
        newEntity.setYHeadRot(yRot);
        newEntity.setYBodyRot(yRot);

        newLevel.addDuringTeleport(newEntity);

        entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);

        return newEntity;
    }

    private static List<Entity> detachPassengers(Entity entity) {
        List<Entity> passengers = new ArrayList<>(entity.getPassengers());
        entity.ejectPassengers();
        return passengers;
    }

    private static void reattachPassengers(
            List<Entity> passengers,
            Vec3 oldPos,
            Vec3 newPos,
            Entity newEntity,
            ServerLevel newLevel,
            Quaterniondc rotation
    ) {
        for (Entity passenger : passengers) {
            Vec3 passengerPos = passenger.position().subtract(oldPos).add(newPos);

            Entity newPassenger = teleportEntityAndPassengers(passenger, newLevel, passengerPos, rotation);

            if (newPassenger != null) {
                newPassenger.startRiding(newEntity, true);
            }
        }
    }

    /**
     * Rotates a Minecraft yaw/pitch look direction by the given quaternion rotation.
     * Minecraft uses: dx = -sin(yaw)*cos(pitch), dy = -sin(pitch), dz = cos(yaw)*cos(pitch).
     *
     * @return float[]{newYRot, newXRot} in degrees
     */
    private static float[] rotateLookAngles(float yRot, float xRot, Quaterniondc rotation) {
        double yawRad = Math.toRadians(yRot);
        double pitchRad = Math.toRadians(xRot);

        double dx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dy = -Math.sin(pitchRad);
        double dz =  Math.cos(yawRad) * Math.cos(pitchRad);

        Vector3d dir = rotation.transform(dx, dy, dz, new Vector3d());

        float newXRot = (float) Math.toDegrees(Math.asin(-dir.y));
        float newYRot = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        return new float[]{newYRot, newXRot};
    }
}
