package shipwrights.genesis.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import java.util.UUID;
import net.neoforged.bus.api.IEventBus;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

/// Genesis with Sable: sub-levels travel between space and celestials' dimensions too (see {@link SubLevelTravel}),
/// taking along the sub-levels connected to them and the entities aboard. Only reached when Sable is loaded (see
/// {@link shipwrights.genesis.compat.Compat}), so this package alone may use its classes
public final class SableCompat {

    private SableCompat() {
    }

    public static void init(IEventBus gameBus) {
        SubLevelTravel.init(gameBus);
        gameBus.addListener(SableCompat::onCelestialTravel);
        gameBus.addListener(EventPriority.LOWEST, SableCompat::onTravelToDimension);
    }

    /// A player leaving a dimension stops following its sub-levels now, rather than on Sable's next tick there. Sable
    /// would then tell its client to forget each, by where its plot is, and its client, by then in another dimension,
    /// would forget whatever sub-level is in that plot there: often the one it arrived on, as plots are handed out the
    /// same way in each dimension. Its client lets go of them all as it changes dimension anyway
    private static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        ServerSubLevelContainer container = SubLevelContainer.getContainer(player.serverLevel());
        if (container == null) return;
        UUID id = player.getGameProfile().getId();
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel server) server.getTrackingPlayers().remove(id);
        }
    }

    /// Entities aboard a sub-level only travel with it, never on their own
    private static void onCelestialTravel(CelestialTravelEvent event) {
        if (isAboardSubLevel(event.rootVehicle())) {
            event.setCanceled(true);
        }
    }

    /// Whether {@code entity} is in a sub-level's plot, or standing on or riding one, which Sable is moving it with
    static boolean isAboardSubLevel(Entity entity) {
        return SableCompanion.INSTANCE.isInPlotGrid(entity)
                || SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity) != null;
    }
}
