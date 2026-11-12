package shipwrights.genesis.compat.vs;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import org.valkyrienskies.mod.api.ValkyrienSkies;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityDraggingInformation;
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

/// Genesis with Valkyrien Skies: ships travel between space and celestials' dimensions too (see {@link ShipTravel}),
/// taking along the ships joined to them and the entities aboard. Only reached when Valkyrien Skies is loaded (see
/// {@link shipwrights.genesis.compat.Compat}), so this package alone may use its classes
public final class VsCompat {

    private VsCompat() {
    }

    public static void init(IEventBus gameBus) {
        gameBus.addListener(ShipTravel::onLevelTick);
        gameBus.addListener(VsCompat::onCelestialTravel);
        ValkyrienSkies.api().getPhysTickEvent().on(ShipConnections::onPhysTick);
    }

    /// Entities aboard a ship only travel with it, never on their own
    private static void onCelestialTravel(CelestialTravelEvent event) {
        if (isAboardShip(event.rootVehicle())) {
            event.setCanceled(true);
        }
    }

    /// Whether {@code entity} is in a ship, in its shipyard, or standing on one, which Valkyrien Skies is moving it with
    static boolean isAboardShip(Entity entity) {
        return VSGameUtilsKt.isBlockInShipyard(entity.level(), entity.position()) || shipStoodOn(entity) != null;
    }

    /// The id of the ship Valkyrien Skies is moving {@code entity} with, having stood on it lately
    static Long shipStoodOn(Entity entity) {
        if (!(entity instanceof IEntityDraggingInformationProvider provider)) return null;
        EntityDraggingInformation dragging = provider.getDraggingInformation();
        if (dragging.getTicksSinceStoodOnShip() >= EntityDraggingInformation.TICKS_TO_DRAG_ENTITIES) return null;
        return dragging.getLastShipStoodOn();
    }
}
