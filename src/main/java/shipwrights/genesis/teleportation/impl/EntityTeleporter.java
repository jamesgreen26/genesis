package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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
     *
     * @param <T>      the type of the entity being teleported
     * @param entity   the entity to teleport
     * @param newLevel the destination level
     * @param newPos   the destination position
     * @return the teleported entity (a new instance for non-player entities), or {@code null} if teleportation failed
     */
    public static <T extends Entity> T teleportEntityAndPassengers(T entity, ServerLevel newLevel, Vec3 newPos) {
        Vec3 oldPos = entity.position();

        List<Entity> passengers = detachPassengers(entity);

        T newEntity = teleportEntity(entity, newLevel, newPos);

        if (newEntity != null) {
            reattachPassengers(passengers, oldPos, newPos, newEntity, newLevel);
        }

        return newEntity;
    }

    /**
     * Teleports a single entity. Players are teleported in-place; all other entities
     * are cloned into the new level.
     *
     * @param <T>      the type of the entity
     * @param entity   the entity to teleport
     * @param newLevel the destination level
     * @param newPos   the destination position
     * @return the entity in the new level, or {@code null} if cloning failed
     */
    @SuppressWarnings("unchecked")
    private static <T extends Entity> T teleportEntity(T entity, ServerLevel newLevel, Vec3 newPos) {
        if (entity instanceof ServerPlayer player) {
            teleportPlayer(player, newLevel, newPos);
            return entity;
        }

        if (entity.level() == newLevel) {
            entity.moveTo(newPos.x, newPos.y, newPos.z, entity.getYRot(), entity.getXRot());
            return entity;
        }

        return (T) cloneAndTeleportEntity(entity, newLevel, newPos);
    }

    /**
     * Teleports a player to the given level and position, preserving their rotation.
     *
     * @param player the player to teleport
     * @param level  the destination level
     * @param pos    the destination position
     */
    private static void teleportPlayer(ServerPlayer player, ServerLevel level, Vec3 pos) {
        player.teleportTo(level, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
    }

    /**
     * Creates a copy of the entity in the new level, restores its state, and marks the
     * original as removed with reason {@link Entity.RemovalReason#CHANGED_DIMENSION}.
     * Returns {@code null} if the entity type fails to produce an instance of the expected class.
     *
     * @param entity   the source entity
     * @param newLevel the destination level
     * @param newPos   the destination position
     * @return the new entity added to {@code newLevel}, or {@code null} on failure
     */
    private static Entity cloneAndTeleportEntity(Entity entity, ServerLevel newLevel, Vec3 newPos) {
        Entity newEntity = entity.getType().create(newLevel);

        if (!entity.getClass().isInstance(newEntity)) {
            return null;
        }

        newEntity.restoreFrom(entity);
        newEntity.moveTo(newPos.x, newPos.y, newPos.z, entity.getYRot(), entity.getXRot());
        newEntity.setYHeadRot(entity.getYHeadRot());
        newEntity.setYBodyRot(entity.getVisualRotationYInDegrees());

        newLevel.addDuringTeleport(newEntity);

        entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);

        return newEntity;
    }

    /**
     * Ejects all passengers from the entity and returns them as a list.
     *
     * @param entity the vehicle entity
     * @return a snapshot of the passengers before ejection
     */
    private static List<Entity> detachPassengers(Entity entity) {
        List<Entity> passengers = new ArrayList<>(entity.getPassengers());
        entity.ejectPassengers();
        return passengers;
    }

    /**
     * Teleports each passenger to the new level, offsetting their position by the same
     * delta as the vehicle, then mounts them back onto {@code newEntity}.
     *
     * @param passengers the passengers to reattach
     * @param oldPos     the vehicle's position before teleportation
     * @param newPos     the vehicle's position after teleportation
     * @param newEntity  the teleported vehicle they should ride
     * @param newLevel   the destination level
     */
    private static void reattachPassengers(
            List<Entity> passengers,
            Vec3 oldPos,
            Vec3 newPos,
            Entity newEntity,
            ServerLevel newLevel
    ) {
        for (Entity passenger : passengers) {
            Vec3 passengerPos = passenger.position().subtract(oldPos).add(newPos);

            Entity newPassenger = teleportEntityAndPassengers(passenger, newLevel, passengerPos);

            if (newPassenger != null) {
                newPassenger.startRiding(newEntity, true);
            }
        }
    }
}
