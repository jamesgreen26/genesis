package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Quaterniondc;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import shipwrights.genesis.GenesisMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityCollector {

    private final ServerLevel oldLevel;
    private final Map<Entity, Vec3> entityStorage = new HashMap<>();

    public EntityCollector(ServerLevel oldLevel) {
        this.oldLevel = oldLevel;
    }

    public Map<Entity, Vec3> collect(
            List<ServerShip> ships,
            Vector3dc origin,
            Vector3dc newPos,
            Quaterniondc rotation
    ) {
        ships.forEach(ship -> addEntitiesForShip(ship, origin, newPos, rotation));

        return entityStorage;
    }

    private void addEntitiesForShip(ServerShip ship, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        AABBd worldAABB = new AABBd(ship.getWorldAABB());
        AABBic shipAABB = ship.getShipAABB();

        if (shipAABB != null) {
            AABBd shipAABBd = new AABBd(shipAABB.minX(), shipAABB.minY(), shipAABB.minZ(), shipAABB.maxX(), shipAABB.maxY(), shipAABB.maxZ());

            oldLevel.getEntities(
                    (Entity) null,
                    VectorConversionsMCKt.toMinecraft(shipAABBd).inflate(48),
                    entity -> !entityStorage.containsKey(entity)
            ).forEach(entity -> addEntity(entity, origin, newPos, rotation));

            worldAABB.union(shipAABBd.transform(ship.getPrevTickTransform().getShipToWorld()));
        }

        oldLevel.getEntities(
                (Entity) null,
                VectorConversionsMCKt.toMinecraft(worldAABB).inflate(8 * GenesisMod.getDimensionScale(oldLevel)),
                entity -> !entityStorage.containsKey(entity)
        ).forEach(entity ->
            addEntity(entity, origin, newPos, rotation)
        );
    }

    private void addEntity(Entity entity, Vector3dc origin, Vector3dc newPos, Quaterniondc rotation) {
        Entity root = entity.getRootVehicle();
        if (entityStorage.containsKey(root)) {
            return;
        }
        Vec3 pos = root.position();
        if (!VSGameUtilsKt.isBlockInShipyard(oldLevel, pos)) {
            Vector3d relPos = VectorConversionsMCKt.toJOML(pos).sub(origin);
            rotation.transform(relPos);
            relPos.add(newPos);
            pos = VectorConversionsMCKt.toMinecraft(relPos);
        }
        entityStorage.put(root, pos);
    }
}
