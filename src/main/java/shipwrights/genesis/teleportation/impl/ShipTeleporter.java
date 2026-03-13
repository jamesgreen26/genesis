package shipwrights.genesis.teleportation.impl;

import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.teleportation.TeleportData;

public class ShipTeleporter {

    public static void teleportShip(
            long id,
            TeleportData data,
            ServerLevel newLevel,
            VsiServerShipWorld shipWorld
    ) {
        LoadedServerShip ship = shipWorld.getLoadedShips().getById(id);
        if (ship == null) {
            return;
        }
        ship.setStatic(false);

        ShipTeleportData teleportData = createTeleportData(data, newLevel);
        shipWorld.teleportShip(ship, teleportData);

        applyPostTeleportVelocity(shipWorld, id, ship, data);
    }

    private static ShipTeleportData createTeleportData(
            TeleportData data,
            ServerLevel level
    ) {
        String vsDimName = VSGameUtilsKt.getDimensionId(level);

        return new ShipTeleportDataImpl(
                data.newPos(),
                data.rotation(),
                data.velocity(),
                data.omega(),
                vsDimName,
                GenesisMod.getDimensionScale(level),
                null
        );
    }

    private static void applyPostTeleportVelocity(
            VsiServerShipWorld shipWorld,
            long id,
            LoadedServerShip ship,
            TeleportData data
    ) {
        Vector3dc velocity = data.velocity();
        Vector3dc omega = data.omega();

        if (velocity.lengthSquared() == 0 && omega.lengthSquared() == 0) {
            return;
        }

        ship.setTransformProvider(createVelocityTransformProvider(shipWorld, id, velocity, omega));
    }

    private static ServerShipTransformProvider createVelocityTransformProvider(
            VsiServerShipWorld shipWorld,
            long id,
            Vector3dc velocity,
            Vector3dc omega
    ) {
        return (prevTransform, transform) -> {

            LoadedServerShip ship = shipWorld.getLoadedShips().getById(id);
            if (ship == null) {
                return null;
            }

            if (!hasTransformChanged(prevTransform, transform)) {
                if (ship.getVelocity().lengthSquared() == 0 &&
                        ship.getAngularVelocity().lengthSquared() == 0) {

                    return new ServerShipTransformProvider.NextTransformAndVelocityData(
                            transform,
                            velocity,
                            omega
                    );
                }
            } else {
                ship.setTransformProvider(null);
            }

            return null;
        };
    }

    private static boolean hasTransformChanged(
            ShipTransform prevTransform,
            ShipTransform transform
    ) {
        return !prevTransform.getPositionInWorld().equals(transform.getPositionInWorld())
                || !prevTransform.getShipToWorldRotation().equals(transform.getShipToWorldRotation());
    }
}
