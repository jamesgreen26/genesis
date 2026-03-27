package shipwrights.genesis.teleportation.integration;

import net.minecraftforge.eventbus.api.Event;
import org.valkyrienskies.core.api.ships.Ship;
import shipwrights.genesis.space.Celestial;


/// Fired when a ship is denied entry to a planet due to addon or datapack functionality
public final class TeleportDisallowedEvent extends Event {
    private final Ship ship;
    private final Celestial celestial;

    public TeleportDisallowedEvent(Ship ship, Celestial celestial) {
        this.ship = ship;
        this.celestial = celestial;
    }

    /// ship that tried to teleport to a Celestial
    public Ship ship() {
        return ship;
    }

    /// Celestial that the ship tried to teleport to
    public Celestial celestial() {
        return celestial;
    }
}
