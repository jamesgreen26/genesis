package shipwrights.genesis.compat.vs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;
import org.valkyrienskies.core.api.ships.ServerShip;
import shipwrights.genesis.space.travel.CelestialTravelEvent;

import java.util.Set;

/// Posted on the NeoForge event bus before a Valkyrien Skies ship, with the ships joined to it and the entities aboard,
/// travels between space and a celestial's dimension. Cancel it to keep them where they are; it's posted again each tick
/// they're still in range. The ships' counterpart to {@link CelestialTravelEvent}
public class CelestialShipTravelEvent extends Event implements ICancellableEvent {
    private final ServerShip ship;
    private final Set<Long> group;
    private final ResourceLocation celestial;
    private final CelestialTravelEvent.Direction direction;
    private final ServerLevel from;
    private final ServerLevel to;

    public CelestialShipTravelEvent(ServerShip ship, Set<Long> group, ResourceLocation celestial,
                                    CelestialTravelEvent.Direction direction, ServerLevel from, ServerLevel to) {
        this.ship = ship;
        this.group = group;
        this.celestial = celestial;
        this.direction = direction;
        this.from = from;
        this.to = to;
    }

    /// The ship that reached where travel happens
    public ServerShip ship() {
        return ship;
    }

    /// The ids of every ship travelling: {@link #ship()} and those joined to it
    public Set<Long> group() {
        return group;
    }

    /// The celestial being arrived at or left, which shares its id with its dimension
    public ResourceLocation celestial() {
        return celestial;
    }

    public CelestialTravelEvent.Direction direction() {
        return direction;
    }

    public ServerLevel from() {
        return from;
    }

    public ServerLevel to() {
        return to;
    }
}
